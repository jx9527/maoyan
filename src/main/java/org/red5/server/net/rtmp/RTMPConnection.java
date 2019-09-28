/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmp;

import java.beans.ConstructorProperties;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.red5.server.BaseConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.exception.ClientRejectedException;
import org.red5.server.net.protocol.RTMPDecodeState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ClientBW;
import org.red5.server.net.rtmp.event.ClientInvokeEvent;
import org.red5.server.net.rtmp.event.ClientNotifyEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.ServerBW;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;
import org.red5.server.so.FlexSharedObjectMessage;
import org.red5.server.so.ISharedObjectEvent;
import org.red5.server.so.SharedObjectMessage;
import org.red5.server.stream.AbstractClientStream;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.OutputStream;
import org.red5.server.stream.PlaylistSubscriberStream;
import org.red5.server.stream.SingleItemSubscriberStream;
import org.red5.server.stream.StreamService;
import org.red5.server.util.CustomizableThreadFactory;
import org.red5.server.util.ScopeUtils;
import org.red5.server.util.timer.HashedWheelTimer;
import org.red5.server.util.timer.Timeout;
import org.red5.server.util.timer.Timer;
import org.red5.server.util.timer.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * RTMP connection. Stores information about client streams, data transfer channels, pending RPC calls, bandwidth configuration, AMF
 * encoding type (AMF0/AMF3), connection state (is alive, last ping time and ping result) and session.
 * RTMP连接。存储有关客户端流、数据传输通道、挂起的RPC调用、带宽配置、AMF的信息
 * 编码类型（amf0/amf3）、连接状态（活动、上次Ping时间和Ping结果）和会话。
 */
public abstract class RTMPConnection extends BaseConnection implements IStreamCapableConnection, IServiceCapableConnection, IReceivedMessageTaskQueueListener {

    private static Logger log = LoggerFactory.getLogger(RTMPConnection.class);

    public static final String RTMP_SESSION_ID = "rtmp.sessionid";

    public static final String RTMP_HANDSHAKE = "rtmp.handshake";

    public static final String RTMP_CONN_MANAGER = "rtmp.connection.manager";

    public static final Object RTMP_HANDLER = "rtmp.handler";

    /**
     * Marker byte for standard or non-encrypted RTMP data.
     * 	标准或非加密RTMP数据的标记字节。
     */
    public static final byte RTMP_NON_ENCRYPTED = (byte) 0x03;

    /**
     * Marker byte for encrypted RTMP data.
     * 	加密RTMP数据标记字节
     */
    public static final byte RTMP_ENCRYPTED = (byte) 0x06;

    /**
     * Marker byte for encrypted RTMP data XTEA. http://en.wikipedia.org/wiki/XTEA
     * 	加密的rtmp数据xtea的标记字节
     */
    public static final byte RTMP_ENCRYPTED_XTEA = (byte) 0x08;

    /**
     * Marker byte for encrypted RTMP data using Blowfish. http://en.wikipedia.org/wiki/Blowfish_(cipher)
     * 	使用Blowfish加密RTMP数据的标记字节
     */
    public static final byte RTMP_ENCRYPTED_BLOWFISH = (byte) 0x09;

    /**
     * Unknown type 0x0a, seen on youtube
     * 	未知类型0x0a，在YouTube上看到
     */
    public static final byte RTMP_ENCRYPTED_UNK = (byte) 0x0a;

    /**
     * Cipher for RTMPE input
     * RTMPE输入密码
     */
    public static final String RTMPE_CIPHER_IN = "rtmpe.cipher.in";

    /**
     * Cipher for RTMPE output
     * RTMPE输出密码
     */
    public static final String RTMPE_CIPHER_OUT = "rtmpe.cipher.out";

    // ~320 streams seems like a sufficient max amount of streams for a single connection
    //对于单个连接，320个流似乎足够多
    public static final double MAX_RESERVED_STREAMS = 320;

	/**
	 * AMF version, AMF0 by default.
	 */
	private volatile Encoding encoding = Encoding.AMF0;
    /**
     * Initial channel capacity
     * 	初始信道容量
     */
    private int channelsInitalCapacity = 3;

    /**
     * Concurrency level for channels collection
     * 	通道集合的并发级别
     */
    private int channelsConcurrencyLevel = 1;

    /**
     * Initial streams capacity
     * 	初始流容量
     */
    private int streamsInitalCapacity = 1;

    /**
     * Concurrency level for streams collection
     * 	流集合的并发级别
     */
    private int streamsConcurrencyLevel = 1;

    /**
     * Initial pending calls capacity
     * 	初始挂起回调容量
     */
    private int pendingCallsInitalCapacity = 3;

    /**
     * Concurrency level for pending calls collection
     * 	挂起回到集合级别
     */
    private int pendingCallsConcurrencyLevel = 1;

    /**
     * Initial reserved streams capacity
     * 	初始预留流容量
     */
    private int reservedStreamsInitalCapacity = 1;

    /**
     * Concurrency level for reserved streams collection
     * 	保留流集合的并发级别
     */
    private int reservedStreamsConcurrencyLevel = 1;

    /**
     * Connection channels
     * 
     * @see org.red5.server.net.rtmp.Channel
     */
    private transient ConcurrentMap<Integer, Channel> channels = new ConcurrentHashMap<>(channelsInitalCapacity, 0.9f, channelsConcurrencyLevel);

    /**
     * Queues of tasks for every channel
     *	每个通道的任务队列
     * @see org.red5.server.net.rtmp.ReceivedMessageTaskQueue
     */
    private final transient ConcurrentMap<Integer, ReceivedMessageTaskQueue> tasksByStreams = new ConcurrentHashMap<>(streamsInitalCapacity, 0.9f, streamsConcurrencyLevel);

    /**
     * Client streams
     * 
     * @see org.red5.server.api.stream.IClientStream
     */
    private transient ConcurrentMap<Number, IClientStream> streams = new ConcurrentHashMap<>(streamsInitalCapacity, 0.9f, streamsConcurrencyLevel);

    /**
     * Reserved stream ids. Stream id's directly relate to individual NetStream instances.
     *	 保留流ID。流ID直接与单个Netstream实例相关。
     */
    private transient Set<Number> reservedStreams = Collections.newSetFromMap(new ConcurrentHashMap<Number, Boolean>(reservedStreamsInitalCapacity, 0.9f, reservedStreamsConcurrencyLevel));

    /**
     * Transaction identifier for remote commands.
     * 	远程命令的事务标识符。
     */
    private AtomicInteger transactionId = new AtomicInteger(1);

    /**
     * Hash map that stores pending calls and ids as pairs.
     * 	将挂起的调用和ID成对存储的哈希映射。
     */
    private transient ConcurrentMap<Integer, IPendingServiceCall> pendingCalls = new ConcurrentHashMap<>(pendingCallsInitalCapacity, 0.75f, pendingCallsConcurrencyLevel);

    /**
     * Deferred results set.
     * 	延迟的结果集。
     * @see org.red5.server.net.rtmp.DeferredResult
     */
    private transient CopyOnWriteArraySet<DeferredResult> deferredResults = new CopyOnWriteArraySet<>();

    /**
     * Last ping round trip time
     * 	上次ping往返时间
     */
    private AtomicInteger lastPingRoundTripTime = new AtomicInteger(-1);

    /**
     * Timestamp when last ping command was sent.
     * 	最后一次ping的发送时间
     */
    private AtomicLong lastPingSentOn = new AtomicLong(0);

    /**
     * Timestamp when last ping result was received.
     * 	最后一次ping收到结果的时间
     */
    private AtomicLong lastPongReceivedOn = new AtomicLong(0);

    /**
     * RTMP events handler
     */
    protected transient IRTMPHandler handler;

    /**
     * Ping interval in ms to detect dead clients.
     * 	以毫秒为单位的ping间隔，用于检测死机。
     */
    private volatile int pingInterval = 5000;

    /**
     * Maximum time in ms after which a client is disconnected because of inactivity.
     * 	客户端因不活动而断开连接的最长时间（毫秒）。
     */
    protected volatile int maxInactivity = 60000;

    /**
     * Data read interval
     * 	数据读取间隔
     */
    protected long bytesReadInterval = 1024 * 1024;

    /**
     * Number of bytes to read next.
     * 	下一个要读取的字节数。
     */
    protected long nextBytesRead = 1024 * 1024;

    /**
     * Number of bytes the client reported to have received.
     *	 客户端报告已接收的字节数。
     */
    private AtomicLong clientBytesRead = new AtomicLong(0L);

    /**
     * Map for pending video packets keyed by stream id.
     * 	由流ID键控的挂起视频包的映射。
     */
    private transient ConcurrentMap<Number, AtomicInteger> pendingVideos = new ConcurrentHashMap<>(1, 0.9f, 1);

    /**
     * Number of (NetStream) streams used.
     * 	使用的（netstream）流数。
     */
    private AtomicInteger usedStreams = new AtomicInteger(0);

    /**
     * Remembered stream buffer durations.
     * 	记住流缓冲区持续时间。
     */
    private transient ConcurrentMap<Number, Integer> streamBuffers = new ConcurrentHashMap<>(1, 0.9f, 1);

    /**
     * Maximum time in milliseconds to wait for a valid handshake.
     * 	等待有效握手的最长时间（毫秒）。
     */
    private int maxHandshakeTimeout = 10000;

    /**
     * Maximum time in milliseconds allowed to process received message
     * 	允许处理接收消息的最长时间（毫秒）
     */
    protected long maxHandlingTimeout = 500L;

    /**
     * Bandwidth limit type / enforcement. (0=hard,1=soft,2=dynamic)
     * 	带宽限制类型/强制
     */
    protected int limitType = 0;

    /**
     * Protocol state
     */
    protected RTMP state = new RTMP();

    // protection for the decoder when using multiple threads per connection
    // 在每个连接使用多个线程时对解码器的保护
    protected transient Semaphore decoderLock = new Semaphore(1, true);

    // protection for the encoder when using multiple threads per connection
    //在每个连接使用多个线程时保护编码器
    protected transient Semaphore encoderLock = new Semaphore(1, true);

    // keeps track of the decode state 跟踪解码状态
    protected transient RTMPDecodeState decoderState;

    /**
     * Scheduling service
     */
    protected transient ThreadPoolTaskScheduler scheduler;

    /**
     * Thread pool for message handling.
     */
    protected transient ThreadPoolTaskExecutor executor;

    /**
     * Thread pool for guarding deadlocks.
     * 	用于保护死锁的线程池
     */
    protected transient ThreadPoolTaskScheduler deadlockGuardScheduler;

    /**
     * Keep-alive worker flag
     * 	保持活动的工人标志
     */
    protected final AtomicBoolean running;

    /**
     * Timestamp generator
     */
    private final AtomicInteger timer = new AtomicInteger(0);
    
    private static Timer timerO = new HashedWheelTimer(new CustomizableThreadFactory("RtmpConnTimerExecutor-"), 1, TimeUnit.SECONDS);
	
    /**
     * Closing flag
     */
    private final AtomicBoolean closing = new AtomicBoolean(false);

    /**
     * Packet sequence number
     */
    private final AtomicLong packetSequence = new AtomicLong();

    /**
     * Specify the size of queue that will trigger audio packet dropping, disabled if it's 0
     *	 指定将触发音频数据包丢弃的队列的大小，如果为0，则禁用
     * */
    private Integer executorQueueSizeToDropAudioPackets = 0;

    /**
     * Keep track of current queue size
     * 	跟踪当前队列大小
     * */
    private final AtomicInteger currentQueueSize = new AtomicInteger();

    /**
     * Wait for handshake task.
     * 	等待握手任务。
     */
    private ScheduledFuture<?> waitForHandshakeTask;

    /**
     * Keep alive task.
     * 	继续执行任务。
     */
    private ScheduledFuture<?> keepAliveTask;

    /**
     * Creates anonymous RTMP connection without scope.
     * 	创建没有作用域的匿名RTMP连接。
     * @param type
     *            Connection type
     */
    @ConstructorProperties({ "type" })
    public RTMPConnection(String type) {
        // 我们从没有作用域的匿名连接开始.
        // 这些参数将在稍后调用“connect”时设置。
        super(type);
        // 创建解码器状态
        decoderState = new RTMPDecodeState(getSessionId());
        // 设置运行标识
        running = new AtomicBoolean(false);
    }

    public int getId() {
        // handle the fact that a client id is a String 处理客户机ID是字符串的事实
        return client != null ? client.getId().hashCode() : -1;
    }

    @Deprecated
    public void setId(int clientId) {
        log.warn("Setting of a client id is deprecated, use IClient to manipulate the id", new Exception("RTMPConnection.setId is deprecated"));
    }

    public void setHandler(IRTMPHandler handler) {
        this.handler = handler;
    }

    public IRTMPHandler getHandler() {
        return handler;
    }

    public RTMP getState() {
        return state;
    }

    public byte getStateCode() {
        return state.getState();
    }

    public void setStateCode(byte code) {
        if (log.isTraceEnabled()) {
            log.trace("setStateCode: {} - {}", code, RTMP.states[code]);
        }
        state.setState(code);
    }

    public IoSession getIoSession() {
        return null;
    }

    /**
     * @return the decoderLock
     */
    public Semaphore getDecoderLock() {
        return decoderLock;
    }

    /**
     * @return the decoderLock
     */
    public Semaphore getEncoderLock() {
        return encoderLock;
    }

    /**
     * @return the decoderState
     */
    public RTMPDecodeState getDecoderState() {
        return decoderState;
    }

    
    public void setBandwidth(int mbits) {
        // 告诉flash播放器我们需要数据的速度，以及我们发送数据的速度
        getChannel(2).write(new ServerBW(mbits));
        // second param is the limit type (0=hard,1=soft,2=dynamic)
        getChannel(2).write(new ClientBW(mbits, (byte) limitType));
    }

    /**
     * Returns a usable timestamp for written packets.
     * 
     * @return timestamp
     */
    public int getTimer() {
        return timer.incrementAndGet();
    }

    /**
     * Opens the connection.
     */
    public void open() {
        if (log.isTraceEnabled()) {
            // dump memory stats
            log.trace("Memory at open - free: {}K total: {}K", Runtime.getRuntime().freeMemory() / 1024, Runtime.getRuntime().totalMemory() / 1024);
        }
    }

    @Override
    public boolean connect(IScope newScope, Object[] params) {
        if (log.isDebugEnabled()) {
            log.debug("Connect scope: {}", newScope);
        }
        try {
            boolean success = super.connect(newScope, params);
            if (success) {
                stopWaitForHandshake();
                // once the handshake has completed, start needed jobs start the ping / pong keep-alive
                startRoundTripMeasurement();
            } else if (log.isDebugEnabled()) {
                log.debug("Connect failed");
            }
            return success;
        } catch (ClientRejectedException e) {
            String reason = (String) e.getReason();
            log.info("Client rejected, reason: " + ((reason != null) ? reason : "None"));
            stopWaitForHandshake();
            throw e;
        }
    }

    /**
     *	 开始等待有效的握手。
     */
    public void startWaitForHandshake() {
        if (log.isDebugEnabled()) {
            log.debug("startWaitForHandshake - {}", sessionId);
        } 
        if (scheduler == null) {
            return;
        }
        // 在maxhandshaketimeout毫秒后启动握手检查器
        try {
            waitForHandshakeTask = scheduler.schedule(new WaitForHandshakeTask(), new Date(System.currentTimeMillis() + maxHandshakeTimeout));
        } catch (TaskRejectedException e) {
            log.error("WaitForHandshake task was rejected for {}", sessionId, e);
        }
    }

    /**
     * Cancels wait for handshake task.
     */
    private void stopWaitForHandshake() {
        if (waitForHandshakeTask != null) {
            boolean cancelled = waitForHandshakeTask.cancel(true);
            waitForHandshakeTask = null;
            if (cancelled && log.isDebugEnabled()) {
                log.debug("waitForHandshake was cancelled for {}", sessionId);
            }
        }
    }

    /**
     * Starts measurement.
     */
    protected void startRoundTripMeasurement() {
    	if (scheduler == null) {
    		log.error("startRoundTripMeasurement cannot be executed due to missing scheduler. This can happen if a connection drops before handshake is complete");
    		return;
    	}
        if(pingInterval <= 0){
        	return;
        }
        if (log.isDebugEnabled()) {
            log.debug("startRoundTripMeasurement - {}", sessionId);
        }
        try {
            // 以现在+2s的初始延迟计划，以防止在connect post过程中ping消息
            keepAliveTask = scheduler.scheduleWithFixedDelay(new KeepAliveTask(), new Date(System.currentTimeMillis() + 2000L), pingInterval);
            if (log.isDebugEnabled()) {
                log.debug("Keep alive scheduled for {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error creating keep alive job for {}", sessionId, e);
        } 
    }

    /**
     * Stops measurement.
     */
    private void stopRoundTripMeasurement() {
        if (keepAliveTask != null) {
            boolean cancelled = keepAliveTask.cancel(true);
            keepAliveTask = null;
            if (cancelled && log.isDebugEnabled()) {
                log.debug("Keep alive was cancelled for {}", sessionId);
            }
        }
    }

    /**
     * Initialize connection. 
     */
    public void setup(String host, String path, Map<String, Object> params) {
        this.host = host;
        this.path = path;
        this.params = params;
        if (Integer.valueOf(3).equals(params.get("objectEncoding"))) {
            if (log.isDebugEnabled()) {
                log.debug("Setting object encoding to AMF3");
            }
            state.setEncoding(Encoding.AMF3);
        }
    }
    
	 
    /**
	 * Initialize connection. 
	 */
	public void setup(String host, String path, String sessionId, Map<String, Object> params) {
		this.host = host;
		this.path = path;
		this.sessionId = sessionId;
		this.params = params;
		if (params.get("objectEncoding") == Integer.valueOf(3)) {
			log.info("Setting object encoding to AMF3");
			encoding = Encoding.AMF3;
			state.setEncoding(Encoding.AMF3);
		}
	}
	 
    public Encoding getEncoding() {
        return state.getEncoding();
    }
 
    public int getNextAvailableChannelId() {
        int result = 4;
        while (isChannelUsed(result)) {
            result++;
        }
        return result;
    }
 
    public boolean isChannelUsed(int channelId) {
        return channels.get(channelId) != null;
    }
 
    public Channel getChannel(int channelId) {
        Channel channel = channels.putIfAbsent(channelId, new Channel(this, channelId));
        if (channel == null) {
            channel = channels.get(channelId);
        }
        return channel;
    }
 
    public void closeChannel(int channelId) {
        if (log.isTraceEnabled()) {
            log.trace("closeChannel: {}", channelId);
        }
        Channel chan = channels.remove(channelId);
        if (log.isTraceEnabled()) {
            log.trace("channel: {} for id: {}", chan, channelId);
            if (chan == null) {
                log.trace("Channels: {}", channels);
            }
        }
        /*
        ReceivedMessageTaskQueue queue = tasksByChannels.remove(channelId);
        if (queue != null) {
            if (isConnected()) {
                // if connected, drain and process the tasks queued-up
                log.debug("Processing remaining tasks at close for channel: {}", channelId);
                processTasksQueue(queue);
            }
            queue.removeAllTasks();
        } else if (log.isTraceEnabled()) {
            log.trace("No task queue for id: {}", channelId);
        }
        */
        chan = null;
    }
 
    protected Collection<IClientStream> getStreams() {
        return streams.values();
    }

    public Map<Number, IClientStream> getStreamsMap() {
        return Collections.unmodifiableMap(streams);
    }
 
    public Number reserveStreamId() {
        double d = 1.0d;
        for (; d < MAX_RESERVED_STREAMS; d++) {
            if (reservedStreams.add(d)) {
                break;
            }
        }
        if (d == MAX_RESERVED_STREAMS) {
            throw new IndexOutOfBoundsException("Unable to reserve new stream");
        }
        return d;
    }
 
    public Number reserveStreamId(Number streamId) {
        if (log.isTraceEnabled()) {
            log.trace("Reserve stream id: {}", streamId);
        }
        if (reservedStreams.add(streamId.doubleValue())) {
            return streamId;
        }
        return reserveStreamId();
    }
 
    public boolean isValidStreamId(Number streamId) {
        double d = streamId.doubleValue();
        if (log.isTraceEnabled()) {
            log.trace("Checking validation for streamId {}; reservedStreams: {}; streams: {}, connection: {}", new Object[] { d, reservedStreams, streams, sessionId });
        }
        if (d <= 0 || !reservedStreams.contains(d)) {
            log.warn("Stream id: {} was not reserved in connection {}", d, sessionId);
            // stream id has not been reserved before
            return false;
        }
        if (streams.get(d) != null) {
            // another stream already exists with this id
            log.warn("Another stream already exists with this id in streams {} in connection: {}", streams, sessionId);
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace("Stream id: {} is valid for connection: {}", d, sessionId);
        }
        return true;
    }

    /**
     * Returns whether or not the connection has been idle for a maximum period. 
     */
    public boolean isIdle() {
        long lastPingTime = lastPingSentOn.get();
        long lastPongTime = lastPongReceivedOn.get();
        boolean idle = (lastPongTime > 0 && (lastPingTime - lastPongTime > maxInactivity));
        if (log.isTraceEnabled()) {
            log.trace("Connection {} {} idle", getSessionId(), (idle ? "is" : "is not"));
        }
        return idle;
    }
 
    public boolean isDisconnected() {
        return state.getState() == RTMP.STATE_DISCONNECTED;
    }
 
    public IClientBroadcastStream newBroadcastStream(Number streamId) {
        if (isValidStreamId(streamId)) {
            // get ClientBroadcastStream defined as a prototype in red5-common.xml
            ClientBroadcastStream cbs = (ClientBroadcastStream) scope.getContext().getBean("clientBroadcastStream");
            customizeStream(streamId, cbs);
            if (!registerStream(cbs)) {
                cbs = null;
            }
            return cbs;
        }
        return null;
    }
 
    public ISingleItemSubscriberStream newSingleItemSubscriberStream(Number streamId) {
        if (isValidStreamId(streamId)) {
            // get SingleItemSubscriberStream defined as a prototype in red5-common.xml
            SingleItemSubscriberStream siss = (SingleItemSubscriberStream) scope.getContext().getBean("singleItemSubscriberStream");
            customizeStream(streamId, siss);
            if (!registerStream(siss)) {
                siss = null;
            }
            return siss;
        }
        return null;
    }
 
    public IPlaylistSubscriberStream newPlaylistSubscriberStream(Number streamId) {
        if (isValidStreamId(streamId)) {
            // get PlaylistSubscriberStream defined as a prototype in red5-common.xml
            PlaylistSubscriberStream pss = (PlaylistSubscriberStream) scope.getContext().getBean("playlistSubscriberStream");
            customizeStream(streamId, pss);
            if (!registerStream(pss)) {
                log.trace("Stream: {} for stream id: {} failed to register", streamId);
                pss = null;
            }
            return pss;
        }
        return null;
    }

    public void addClientStream(IClientStream stream) {
        if (reservedStreams.add(stream.getStreamId().doubleValue())) {
            registerStream(stream);
        } else {
            // stream not added to registered? what to do with it?
            log.warn("Failed adding stream: {} to reserved: {}", stream, reservedStreams);
        }
    }

    public void removeClientStream(Number streamId) {
        unreserveStreamId(streamId);
    }
 
    protected int getUsedStreamCount() {
        return usedStreams.get();
    }
 
    public IClientStream getStreamById(Number streamId) {
        return streams.get(streamId.doubleValue());
    }
 
    public Number getStreamIdForChannelId(int channelId) {
        if (channelId < 4) {
            return 0;
        }
        Number streamId = Math.floor(((channelId - 4) / 5.0d) + 1);
        if (log.isTraceEnabled()) {
            log.trace("Stream id: {} requested for channel id: {}", streamId, channelId);
        }
        return streamId;
    }
 
    public IClientStream getStreamByChannelId(int channelId) {
        // channels 2 and 3 are "special" and don't have an IClientStream associated
        if (channelId < 4) {
            return null;
        }
        Number streamId = getStreamIdForChannelId(channelId);
        if (log.isTraceEnabled()) {
            log.trace("Stream requested for channel id: {} stream id: {} streams: {}", channelId, streamId, streams);
        }
        return getStreamById(streamId);
    }
 
    public int getChannelIdForStreamId(Number streamId) {
        int channelId = (int) (streamId.doubleValue() * 5) - 1;
        if (log.isTraceEnabled()) {
            log.trace("Channel id: {} requested for stream id: {}", channelId, streamId);
        }
        return channelId;
    }
 
    public OutputStream createOutputStream(Number streamId) {
        int channelId = getChannelIdForStreamId(streamId);
        if (log.isTraceEnabled()) {
            log.trace("Create output - stream id: {} channel id: {}", streamId, channelId);
        }
        final Channel data = getChannel(channelId++);
        final Channel video = getChannel(channelId++);
        final Channel audio = getChannel(channelId++);
        if (log.isTraceEnabled()) {
            log.trace("Output stream - data: {} video: {} audio: {}", data, video, audio);
        }
        return new OutputStream(video, audio, data);
    }
 
    private void customizeStream(Number streamId, AbstractClientStream stream) {
        Integer buffer = streamBuffers.get(streamId.doubleValue());
        if (buffer != null) {
            stream.setClientBufferDuration(buffer);
        }
        stream.setName(createStreamName());
        stream.setConnection(this);
        stream.setScope(this.getScope());
        stream.setStreamId(streamId);
    }
 
    private boolean registerStream(IClientStream stream) {
        if (streams.putIfAbsent(stream.getStreamId().doubleValue(), stream) == null) {
            usedStreams.incrementAndGet();
            return true;
        }
        log.error("Unable to register stream {}, stream with id {} was already added", stream, stream.getStreamId());
        return false;
    }
 
    @SuppressWarnings("unused")
    private void unregisterStream(IClientStream stream) {
        if (stream != null) {
            deleteStreamById(stream.getStreamId());
        }
    }
 
    @Override
    public void close() {
        if (closing.compareAndSet(false, true)) {
            if (log.isDebugEnabled()) {
                log.debug("close: {}", sessionId);
            }
            stopWaitForHandshake();
            stopRoundTripMeasurement();
            // update our state
            if (state != null) {
                final byte s = getStateCode();
                switch (s) {
                    case RTMP.STATE_DISCONNECTED:
                        if (log.isDebugEnabled()) {
                            log.debug("Already disconnected");
                        }
                        return;
                    default:
                        if (log.isDebugEnabled()) {
                            log.debug("State: {}", RTMP.states[s]);
                        }
                        setStateCode(RTMP.STATE_DISCONNECTING);
                }
            }
            Red5.setConnectionLocal(this);
            IStreamService streamService = (IStreamService) ScopeUtils.getScopeService(scope, IStreamService.class, StreamService.class);
            if (streamService != null) {
                //in the end of call streamService.deleteStream we do streams.remove
                for (Iterator<IClientStream> it = streams.values().iterator(); it.hasNext();) {
                    IClientStream stream = it.next();
                    if (log.isDebugEnabled()) {
                        log.debug("Closing stream: {}", stream.getStreamId());
                    }
                    streamService.deleteStream(this, stream.getStreamId());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Stream service was not found for scope: {}", (scope != null ? scope.getName() : "null or non-existant"));
                }
            }
            // close the base connection - disconnect scopes and unregister client
            super.close();
            // kill all the collections etc
            channels.clear();
            streams.clear();
            pendingCalls.clear();
            deferredResults.clear();
            pendingVideos.clear();
            streamBuffers.clear();
            if (log.isTraceEnabled()) {
                // dump memory stats
                log.trace("Memory at close - free: {}K total: {}K", Runtime.getRuntime().freeMemory() / 1024, Runtime.getRuntime().totalMemory() / 1024);
            }
        } else if (log.isDebugEnabled()) {
            log.debug("Already closing..");
        }
    }
 
    @Override
    public void dispatchEvent(IEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Event notify: {}", event);
        }
        // determine if its an outgoing invoke or notify
        switch (event.getType()) {
            case CLIENT_INVOKE:
                ClientInvokeEvent cie = (ClientInvokeEvent) event;
                invoke(cie.getMethod(), cie.getParams(), cie.getCallback());
                break;
            case CLIENT_NOTIFY:
                ClientNotifyEvent cne = (ClientNotifyEvent) event;
                notify(cne.getMethod(), cne.getParams());
                break;
            default:
                log.warn("Unhandled event: {}", event);
        }
    }

    /**
     * When the connection has been closed, notify any remaining pending service calls that they have failed because the connection is
     * broken. Implementors of IPendingServiceCallback may only deduce from this notification that it was not possible to read a result for
     * this service call. It is possible that (1) the service call was never written to the service, or (2) the service call was written to
     * the service and although the remote method was invoked, the connection failed before the result could be read, or (3) although the
     * remote method was invoked on the service, the service implementor detected the failure of the connection and performed only partial
     * processing. The caller only knows that it cannot be confirmed that the callee has invoked the service call and returned a result.
     */
    public void sendPendingServiceCallsCloseError() {
        if (pendingCalls != null && !pendingCalls.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Connection calls pending: {}", pendingCalls.size());
            }
            for (IPendingServiceCall call : pendingCalls.values()) {
                call.setStatus(Call.STATUS_NOT_CONNECTED);
                for (IPendingServiceCallback callback : call.getCallbacks()) {
                    callback.resultReceived(call);
                }
            }
        }
    }

    
    public void unreserveStreamId(Number streamId) {
        if (log.isTraceEnabled()) {
            log.trace("Unreserve streamId: {}", streamId);
        }
        double d = streamId.doubleValue();
        if (d > 0.0d) {
            if (reservedStreams.remove(d)) {
                deleteStreamById(d);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Failed to unreserve stream id: {} streams: {}", d, streams);
                }
            }
        }
    }

    
    public void deleteStreamById(Number streamId) {
        if (log.isTraceEnabled()) {
            log.trace("Delete streamId: {}", streamId);
        }
        double d = streamId.doubleValue();
        if (d > 0.0d) {
            if (streams.remove(d) != null) {
                usedStreams.decrementAndGet();
                pendingVideos.remove(d);
                streamBuffers.remove(d);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Failed to remove stream id: {} streams: {}", d, streams);
                }
            }
        }
    }
 
    public void ping(Ping ping) {
        getChannel(2).write(ping);
    }
 
    public abstract void write(Packet out);
 
    public abstract void writeRaw(IoBuffer out);
 
    protected void updateBytesRead() {
        if (log.isTraceEnabled()) {
            log.trace("updateBytesRead");
        }
        long bytesRead = getReadBytes();
        if (bytesRead >= nextBytesRead) {
            BytesRead sbr = new BytesRead((int) (bytesRead % Integer.MAX_VALUE));
            getChannel(2).write(sbr);
            nextBytesRead += bytesReadInterval;
        }
    }
 
    public void receivedBytesRead(int bytes) {
        if (log.isDebugEnabled()) {
            log.debug("Client received {} bytes, written {} bytes, {} messages pending", new Object[] { bytes, getWrittenBytes(), getPendingMessages() });
        }
        clientBytesRead.addAndGet(bytes);
    }
 
    public long getClientBytesRead() {
        return clientBytesRead.get();
    }
 
    public void invoke(IServiceCall call) {
        invoke(call, 3);
    }
 
    public int getTransactionId() {
        return transactionId.incrementAndGet();
    }
 
    public void registerPendingCall(int invokeId, IPendingServiceCall call) {
        pendingCalls.put(invokeId, call);
    }
 
    public void invoke(IServiceCall call, int channel) {
        // We need to use Invoke for all calls to the client
        Invoke invoke = new Invoke();
        invoke.setCall(call);
        invoke.setTransactionId(getTransactionId());
        if (call instanceof IPendingServiceCall) {
            registerPendingCall(invoke.getTransactionId(), (IPendingServiceCall) call);
        }
        getChannel(channel).write(invoke);
    }
 
    public void invoke(String method) {
        invoke(method, null, null);
    }
 
    public void invoke(String method, Object[] params) {
        invoke(method, params, null);
    }
 
    public void invoke(String method, IPendingServiceCallback callback) {
        invoke(method, null, callback);
    }
 
    public void invoke(String method, Object[] params, IPendingServiceCallback callback) {
        IPendingServiceCall call = new PendingCall(method, params);
        if (callback != null) {
            call.registerCallback(callback);
        }
        invoke(call);
    }
 
    public void notify(IServiceCall call) {
        notify(call, 3);
    }
 
    public void notify(IServiceCall call, int channel) {
        Notify notify = new Notify();
        notify.setCall(call);
        getChannel(channel).write(notify);
    }
 
    public void notify(String method) {
        notify(method, null);
    }

    
    public void notify(String method, Object[] params) {
        IServiceCall call = new Call(method, params);
        notify(call);
    }

    
    public void status(Status status) {
        status(status, 3);
    }

    
    public void status(Status status, int channel) {
        if (status != null) {
            getChannel(channel).sendStatus(status);
        }
    }
 
    @Override
    public long getReadBytes() {
        return 0;
    }

    
    @Override
    public long getWrittenBytes() {
        return 0;
    }
 
    public IPendingServiceCall getPendingCall(int invokeId) {
        return pendingCalls.get(invokeId);
    }
 
    public IPendingServiceCall retrievePendingCall(int invokeId) {
        return pendingCalls.remove(invokeId);
    }
 
    protected String createStreamName() {
        return UUID.randomUUID().toString();
    }
 
    protected void writingMessage(Packet message) {
        if (message.getMessage() instanceof VideoData) {
            Number streamId = message.getHeader().getStreamId();
            final AtomicInteger value = new AtomicInteger();
            AtomicInteger old = pendingVideos.putIfAbsent(streamId.doubleValue(), value);
            if (old == null) {
                old = value;
            }
            old.incrementAndGet();
        }
    }
 
    public void messageReceived() {
        if (log.isTraceEnabled()) {
            log.trace("messageReceived");
        }
        readMessages.incrementAndGet();
        // trigger generation of BytesRead messages
        updateBytesRead();
    }

    private String getMessageType(Packet packet) {
        final Header header = packet.getHeader();
        final byte headerDataType = header.getDataType();
        return messageTypeToName(headerDataType);
    }

    public String messageTypeToName(byte headerDataType) {
        switch (headerDataType) {
            case Constants.TYPE_AGGREGATE:
                return "TYPE_AGGREGATE";
            case Constants.TYPE_AUDIO_DATA:
                return "TYPE_AUDIO_DATA";
            case Constants.TYPE_VIDEO_DATA:
                return "TYPE_VIDEO_DATA";
            case Constants.TYPE_FLEX_SHARED_OBJECT:
                return "TYPE_FLEX_SHARED_OBJECT";
            case Constants.TYPE_SHARED_OBJECT:
                return "TYPE_SHARED_OBJECT";
            case Constants.TYPE_INVOKE:
                return "TYPE_INVOKE";
            case Constants.TYPE_FLEX_MESSAGE:
                return "TYPE_FLEX_MESSAGE";
            case Constants.TYPE_NOTIFY:
                return "TYPE_NOTIFY";
            case Constants.TYPE_FLEX_STREAM_SEND:
                return "TYPE_FLEX_STREAM_SEND";
            case Constants.TYPE_PING:
                return "TYPE_PING";
            case Constants.TYPE_BYTES_READ:
                return "TYPE_BYTES_READ";
            case Constants.TYPE_CHUNK_SIZE:
                return "TYPE_CHUNK_SIZE";
            case Constants.TYPE_CLIENT_BANDWIDTH:
                return "TYPE_CLIENT_BANDWIDTH";
            case Constants.TYPE_SERVER_BANDWIDTH:
                return "TYPE_SERVER_BANDWIDTH";
            default:
                return "UNKNOWN [" + headerDataType + "]";

        }
    }
 
    public void handleMessageReceived(Packet packet) {
        if (log.isTraceEnabled()) {
            log.trace("handleMessageReceived - {}", sessionId);
        }
        // 如果未禁用maxhandlingtimeout，请设置数据包过期时间（设置为0）
        if (maxHandlingTimeout > 0) {
            packet.setExpirationTime(System.currentTimeMillis() + maxHandlingTimeout);
        }
        if (executor != null) {
            final byte dataType = packet.getHeader().getDataType();
            //将这些类型路由到执行器之外
            switch (dataType) {
                case Constants.TYPE_PING:
                case Constants.TYPE_ABORT:
                case Constants.TYPE_BYTES_READ:
                case Constants.TYPE_CHUNK_SIZE:
                case Constants.TYPE_CLIENT_BANDWIDTH:
                case Constants.TYPE_SERVER_BANDWIDTH:
                    //将消息传递给处理程序
                    try {
                        handler.messageReceived(this, packet);
                    } catch (Exception e) {
                        log.error("Error processing received message {}", sessionId, e);
                    }
                    break;
                default:
                    final String messageType = getMessageType(packet);
                    try {
                        // 增加包数
                        final long packetNumber = packetSequence.incrementAndGet();
                        if (executorQueueSizeToDropAudioPackets > 0 && currentQueueSize.get() >= executorQueueSizeToDropAudioPackets) {
                            if (packet.getHeader().getDataType() == Constants.TYPE_AUDIO_DATA) {
                                //如果队列中有积压的消息。flash可能在网络拥塞后发送了一系列消息。扔掉我们可以丢弃的包。
                                log.info("Queue threshold reached. Discarding packet: session=[{}], msgType=[{}], packetNum=[{}]", sessionId, messageType, packetNumber);
                                return;
                            }
                        }
                        int streamId = packet.getHeader().getStreamId().intValue();
                        if (log.isTraceEnabled()) {
                            log.trace("Handling message for streamId: {}, channelId: {} Channels: {}", streamId, packet.getHeader().getChannelId(), channels);
                        }
                        // 创建任务以设置处理消息
                        ReceivedMessageTask task = new ReceivedMessageTask(sessionId, packet, handler, this);
                        task.setPacketNumber(packetNumber);
                        // 创建任务队列
                        ReceivedMessageTaskQueue newStreamTasks = new ReceivedMessageTaskQueue(streamId, this);
                        // 按流映射将队列放入任务中
                        ReceivedMessageTaskQueue currentStreamTasks = tasksByStreams.putIfAbsent(streamId, newStreamTasks);
                        if (currentStreamTasks != null) {
                            //将任务添加到现有队列
                            currentStreamTasks.addTask(task);
                        } else {
                            // 将任务添加到新创建的和刚添加的队列中
                            newStreamTasks.addTask(task);
                        }
                    } catch (Exception e) {
                        log.error("Incoming message handling failed on session=[" + sessionId + "], messageType=[" + messageType + "]", e);
                        if (log.isDebugEnabled()) {
                            log.debug("Execution rejected on {} - {}", sessionId, RTMP.states[getStateCode()]);
                            log.debug("Lock permits - decode: {} encode: {}", decoderLock.availablePermits(), encoderLock.availablePermits());
                        }
                    }
            }
        } else {
            log.debug("Executor is null on {} state: {}", sessionId, RTMP.states[getStateCode()]);
            // pass message to the handler
            try {
                handler.messageReceived(this, packet);
            } catch (Exception e) {
                log.error("Error processing received message {} state: {}", sessionId, RTMP.states[getStateCode()], e);
            }
        }
    }

    @Override
    public void onTaskAdded(ReceivedMessageTaskQueue queue) {
        currentQueueSize.incrementAndGet();
        processTasksQueue(queue);
    }

    @Override
    public void onTaskRemoved(ReceivedMessageTaskQueue queue) {
        currentQueueSize.decrementAndGet();
        processTasksQueue(queue);
    }

    @SuppressWarnings("unchecked")
    private void processTasksQueue(final ReceivedMessageTaskQueue currentStreamTasks) {
        int streamId = currentStreamTasks.getStreamId();
        if (log.isTraceEnabled()) {
            log.trace("Process tasks for streamId {}", streamId);
        }
        final ReceivedMessageTask task = currentStreamTasks.getTaskToProcess();
        if(task == null){
        	if (log.isTraceEnabled()) {
                log.trace("Channel {} task queue is empty", streamId);
            }
        	return;
        } 
        Packet packet = task.getPacket();
        try {
            final String messageType = getMessageType(packet);
            ListenableFuture<Packet> future = (ListenableFuture<Packet>) executor.submitListenable(new ListenableFutureTask<Packet>(task));
            future.addCallback(new ListenableFutureCallback<Packet>() {

                final long startTime = System.currentTimeMillis();

                int getProcessingTime() {
                    return (int) (System.currentTimeMillis() - startTime);
                }

                public void onFailure(Throwable t) {
                    log.debug("ReceivedMessageTask failure: {}", t);
                    if (log.isWarnEnabled()) {
                        log.warn("onFailure - session: {}, msgtype: {}, processingTime: {}, packetNum: {}", sessionId, messageType, getProcessingTime(), task.getPacketNumber());
                    }
                    currentStreamTasks.removeTask(task);
                }

                public void onSuccess(Packet packet) {
                    log.debug("ReceivedMessageTask success");
                    if (log.isDebugEnabled()) {
                        log.debug("onSuccess - session: {}, msgType: {}, processingTime: {}, packetNum: {}", sessionId, messageType, getProcessingTime(), task.getPacketNumber());
                    }
                    currentStreamTasks.removeTask(task);
                }

            });
        } catch (TaskRejectedException tre) {
            Throwable[] suppressed = tre.getSuppressed();
            for (Throwable t : suppressed) {
                log.warn("Suppressed exception on {}", sessionId, t);
            }
            log.info("Rejected message: {} on {}", packet, sessionId);
            currentStreamTasks.removeTask(task);
        } catch (Throwable e) {
            log.error("Incoming message handling failed on session=[" + sessionId + "]", e);
            if (log.isDebugEnabled()) {
                log.debug("Execution rejected on {} - {}", getSessionId(), RTMP.states[getStateCode()]);
                log.debug("Lock permits - decode: {} encode: {}", decoderLock.availablePermits(), encoderLock.availablePermits());
            }
            currentStreamTasks.removeTask(task);
        }
        
    }
 
    public void messageSent(Packet message) {
        if (message.getMessage() instanceof VideoData) {
            Number streamId = message.getHeader().getStreamId();
            AtomicInteger pending = pendingVideos.get(streamId.doubleValue());
            if (log.isTraceEnabled()) {
                log.trace("Stream id: {} pending: {} total pending videos: {}", streamId, pending, pendingVideos.size());
            }
            if (pending != null) {
                pending.decrementAndGet();
            }
        }
        writtenMessages.incrementAndGet();
    }
 
    protected void messageDropped() {
        droppedMessages.incrementAndGet();
    }
 
    protected int currentQueueSize() {
        return currentQueueSize.get();
    }
 
    @Override
    public long getPendingVideoMessages(Number streamId) {
        AtomicInteger pendingCount = pendingVideos.get(streamId.doubleValue());
        if (log.isTraceEnabled()) {
            log.trace("Stream id: {} pendingCount: {} total pending videos: {}", streamId, pendingCount, pendingVideos.size());
        }
        return pendingCount != null ? pendingCount.intValue() : 0;
    } 
   
    public void sendSharedObjectMessage(String name, int currentVersion, boolean persistent, Set<ISharedObjectEvent> events) {
        // create a new sync message for every client to avoid concurrent access through multiple threads
        SharedObjectMessage syncMessage = state.getEncoding() == Encoding.AMF3 ? new FlexSharedObjectMessage(null, name, currentVersion, persistent) : new SharedObjectMessage(null, name, currentVersion, persistent);
        syncMessage.addEvents(events);
        try {
            // get the channel for so updates
            Channel channel = getChannel(3);
            if (log.isTraceEnabled()) {
                log.trace("Send to channel: {}", channel);
            }
            channel.write(syncMessage);
        } catch (Exception e) {
            log.warn("Exception sending shared object", e);
        }
    }

    
    public void ping() {
        long newPingTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Send Ping: session=[{}], currentTime=[{}], lastPingTime=[{}]", new Object[] { getSessionId(), newPingTime, lastPingSentOn.get() });
        }
        if (lastPingSentOn.get() == 0) {
            lastPongReceivedOn.set(newPingTime);
        }
        Ping pingRequest = new Ping();
        pingRequest.setEventType(Ping.PING_CLIENT);
        lastPingSentOn.set(newPingTime);
        int now = (int) (newPingTime & 0xffffffffL);
        pingRequest.setValue2(now);
        ping(pingRequest);
    }

    /**
     * Marks that ping back was received.
     * 
     * @param pong
     *            Ping object
     */
    public void pingReceived(Ping pong) {
        long now = System.currentTimeMillis();
        long previousPingTime = lastPingSentOn.get();
        int previousPingValue = (int) (previousPingTime & 0xffffffffL);
        int pongValue = pong.getValue2().intValue();
        if (log.isDebugEnabled()) {
            log.debug("Pong received: session=[{}] at {} with value {}, previous received at {}", new Object[] { getSessionId(), now, pongValue, previousPingValue });
        }
        if (pongValue == previousPingValue) {
            lastPingRoundTripTime.set((int) ((now - previousPingTime) & 0xffffffffL));
            if (log.isDebugEnabled()) {
                log.debug("Ping response session=[{}], RTT=[{} ms]", new Object[] { getSessionId(), lastPingRoundTripTime.get() });
            }
        } else {
            // don't log the congestion entry unless there are more than X messages waiting
            if (getPendingMessages() > 4) {
                int pingRtt = (int) ((now & 0xffffffffL)) - pongValue;
                log.info("Pong delayed: session=[{}], ping response took [{} ms] to arrive. Connection may be congested, or loopback", new Object[] { getSessionId(), pingRtt });
            }
        }
        lastPongReceivedOn.set(now);
    }

    /**
     * Difference between when the last ping was sent and when the last pong was received.
     * 
     * @return last interval of ping minus pong
     */
    public int getLastPingSentAndLastPongReceivedInterval() {
        return (int) (lastPingSentOn.get() - lastPongReceivedOn.get());
    }

    
    public int getLastPingTime() {
        return lastPingRoundTripTime.get();
    }

    /**
     * Setter for ping interval.
     * 
     * @param pingInterval
     *            Interval in ms to ping clients. Set to 0 to disable ghost detection code.
     */
    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    /**
     * Setter for maximum inactivity.
     * 
     * @param maxInactivity
     *            Maximum time in ms after which a client is disconnected in case of inactivity.
     */
    public void setMaxInactivity(int maxInactivity) {
        this.maxInactivity = maxInactivity;
    }

    /**
     * Inactive state event handler.
     */
    protected abstract void onInactive();

    /**
     * Sets the scheduler.
     * 
     * @param scheduler
     *            scheduling service / thread executor
     */
    public void setScheduler(ThreadPoolTaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * @return the scheduler
     */
    public ThreadPoolTaskScheduler getScheduler() {
        return scheduler;
    }

    public ThreadPoolTaskExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    /**
     * Thread pool for guarding deadlocks
     *
     * @return the deadlockGuardScheduler
     */
    public ThreadPoolTaskScheduler getDeadlockGuardScheduler() {
        return deadlockGuardScheduler;
    }

    /**
     * Thread pool for guarding deadlocks
     * 
     * @param deadlockGuardScheduler
     *            the deadlockGuardScheduler to set
     */
    public void setDeadlockGuardScheduler(ThreadPoolTaskScheduler deadlockGuardScheduler) {
        this.deadlockGuardScheduler = deadlockGuardScheduler;
    }

    /**
     * Registers deferred result.
     * 
     * @param result
     *            Result to register
     */
    public void registerDeferredResult(DeferredResult result) {
        deferredResults.add(result);
    }

    /**
     * Unregister deferred result
     * 
     * @param result
     *            Result to unregister
     */
    public void unregisterDeferredResult(DeferredResult result) {
        deferredResults.remove(result);
    }

    public void rememberStreamBufferDuration(int streamId, int bufferDuration) {
        streamBuffers.put(streamId, bufferDuration);
    }

    /**
     * Set maximum time to wait for valid handshake in milliseconds.
     * 
     * @param maxHandshakeTimeout
     *            Maximum time in milliseconds
     */
    public void setMaxHandshakeTimeout(int maxHandshakeTimeout) {
        this.maxHandshakeTimeout = maxHandshakeTimeout;
    }

    public long getMaxHandlingTimeout() {
        return maxHandlingTimeout;
    }

    public void setMaxHandlingTimeout(long maxHandlingTimeout) {
        this.maxHandlingTimeout = maxHandlingTimeout;
    }

    public int getChannelsInitalCapacity() {
        return channelsInitalCapacity;
    }

    public void setChannelsInitalCapacity(int channelsInitalCapacity) {
        this.channelsInitalCapacity = channelsInitalCapacity;
    }

    public int getChannelsConcurrencyLevel() {
        return channelsConcurrencyLevel;
    }

    public void setChannelsConcurrencyLevel(int channelsConcurrencyLevel) {
        this.channelsConcurrencyLevel = channelsConcurrencyLevel;
    }

    public int getStreamsInitalCapacity() {
        return streamsInitalCapacity;
    }

    public void setStreamsInitalCapacity(int streamsInitalCapacity) {
        this.streamsInitalCapacity = streamsInitalCapacity;
    }

    public int getStreamsConcurrencyLevel() {
        return streamsConcurrencyLevel;
    }

    public void setStreamsConcurrencyLevel(int streamsConcurrencyLevel) {
        this.streamsConcurrencyLevel = streamsConcurrencyLevel;
    }

    public int getPendingCallsInitalCapacity() {
        return pendingCallsInitalCapacity;
    }

    public void setPendingCallsInitalCapacity(int pendingCallsInitalCapacity) {
        this.pendingCallsInitalCapacity = pendingCallsInitalCapacity;
    }

    public int getPendingCallsConcurrencyLevel() {
        return pendingCallsConcurrencyLevel;
    }

    public void setPendingCallsConcurrencyLevel(int pendingCallsConcurrencyLevel) {
        this.pendingCallsConcurrencyLevel = pendingCallsConcurrencyLevel;
    }

    public int getReservedStreamsInitalCapacity() {
        return reservedStreamsInitalCapacity;
    }

    public void setReservedStreamsInitalCapacity(int reservedStreamsInitalCapacity) {
        this.reservedStreamsInitalCapacity = reservedStreamsInitalCapacity;
    }

    public int getReservedStreamsConcurrencyLevel() {
        return reservedStreamsConcurrencyLevel;
    }

    public void setReservedStreamsConcurrencyLevel(int reservedStreamsConcurrencyLevel) {
        this.reservedStreamsConcurrencyLevel = reservedStreamsConcurrencyLevel;
    }

    /**
     * Specify the size of queue that will trigger audio packet dropping, disabled if it's 0
     * 
     * @param executorQueueSizeToDropAudioPackets
     *            queue size
     */
    public void setExecutorQueueSizeToDropAudioPackets(Integer executorQueueSizeToDropAudioPackets) {
        this.executorQueueSizeToDropAudioPackets = executorQueueSizeToDropAudioPackets;
    }

    @Override
    public String getProtocol() {
        return "rtmp";
    }

    
    @Override
    public String toString() {
        if (log.isDebugEnabled()) {
            String id = getClient() != null ? getClient().getId() : null;
            return String.format("%1$s %2$s:%3$s to %4$s client: %5$s session: %6$s state: %7$s", new Object[] { getClass().getSimpleName(), getRemoteAddress(), getRemotePort(), getHost(), id, getSessionId(), RTMP.states[getStateCode()] });
        } else {
            Object[] args = new Object[] { getClass().getSimpleName(), getRemoteAddress(), getReadBytes(), getWrittenBytes(), getSessionId(), RTMP.states[getStateCode()] };
            return String.format("%1$s from %2$s (in: %3$s out: %4$s) session: %5$s state: %6$s", args);
        }
    }

    /**
     *使连接保持活动状态并在客户端死机时断开连接的任务。
     */
    private class KeepAliveTask implements Runnable {

        private final AtomicLong lastBytesRead = new AtomicLong(0);

        private volatile long lastBytesReadTime = 0;

        @Override
        public void run() {
            //在连接状态下才能ping
        	if (state.getState() != RTMP.STATE_CONNECTED) {
        		return;
        	}
        	// 确保作业尚未运行
            if (!running.compareAndSet(false, true)) {
            	return;
            } 
            if (log.isTraceEnabled()) {
                log.trace("Running keep-alive for {}", getSessionId());
            }
            
            try {
                // first check connected
            	if (!isConnected()){
            		 if (log.isDebugEnabled()) {
                         log.debug("No longer connected, clean up connection. Connection state: {}", RTMP.states[state.getState()]);
                     }
                     onInactive();
                     return;
            	}  
                long now = System.currentTimeMillis();
                //获取连接上当前读取的字节数
                long currentReadBytes = getReadBytes();
                //获取最后读取的字节数
                long previousReadBytes = lastBytesRead.get();
                if (log.isTraceEnabled()) {
                    log.trace("Time now: {} current read count: {} last read count: {}", new Object[] { now, currentReadBytes, previousReadBytes });
                }
                if (currentReadBytes > previousReadBytes) {
                    if (log.isTraceEnabled()) {
                        log.trace("Client is still alive, no ping needed");
                    }
                    // 自上次检查以来，客户端已发送数据，因此没有死机。不需要ping
                    if (lastBytesRead.compareAndSet(previousReadBytes, currentReadBytes)) {
                        //更新时间戳以匹配我们的更新
                        lastBytesReadTime = now;
                    }
                } else {
                    //客户端没有向ping命令发送响应，也没有发送数据太长时间，请断开连接
                    long lastPingTime = lastPingSentOn.get();
                    long lastPongTime = lastPongReceivedOn.get();
                    if (lastPongTime > 0 && (lastPingTime - lastPongTime > maxInactivity) && (now - lastBytesReadTime > maxInactivity)) {
                        log.warn("Closing connection - inactivity timeout: session=[{}], lastPongReceived=[{} ms ago], lastPingSent=[{} ms ago], lastDataRx=[{} ms ago]", new Object[] { getSessionId(), (lastPingTime - lastPongTime), (now - lastPingTime), (now - lastBytesReadTime) });
                        // 以下行处理一个非常常见的支持请求
                        log.warn("Client on session=[{}] has not responded to our ping for [{} ms] and we haven't received data for [{} ms]", new Object[] { getSessionId(), (lastPingTime - lastPongTime), (now - lastBytesReadTime) });
                        onInactive();
                    } else {
                        // 向客户端发送ping命令触发数据发送
                        ping();
                    }
                }
                 
            } catch (Exception e) {
                log.warn("Exception in keepalive for {}", getSessionId(), e);
            } finally {
                // 重置运行标识
                running.compareAndSet(true, false);
            }
        }
    }

    /**
     * 
     * 10秒钟内如果握手没有成功则断开连接
     */
    private class WaitForHandshakeTask implements TimerTask , Runnable {

        public WaitForHandshakeTask() {
            if (log.isTraceEnabled()) {
                log.trace("WaitForHandshakeTask created on scheduler: {} for session: {}", scheduler, getSessionId());
            }
        }

        public void run() {
            if (log.isTraceEnabled()) {
                log.trace("WaitForHandshakeTask started for {}", getSessionId());
            }
            // check for connected state before disconnecting
            if (state.getState() != RTMP.STATE_CONNECTED) {
                // Client didn't send a valid handshake, disconnect
                log.warn("Closing {}, due to long handshake. State: {}", getSessionId(), RTMP.states[getStateCode()]);
                onInactive();
            }
        }

		@Override
		public void run(Timeout timeout) throws Exception {
			waitForHandshakeTimeout = null;
			// Client didn't send a valid handshake, disconnect
			log.warn("Closing {}, with id {} due to long handshake", RTMPConnection.this, getId());
			onInactive();
		}

    } 
	private Timeout waitForHandshakeTimeout;
    
	protected void startWaitForHandshake(ISchedulingService service) {
		waitForHandshakeTimeout = timerO.newTimeout(new WaitForHandshakeTask(), maxHandshakeTimeout, TimeUnit.MILLISECONDS);
	}
}
