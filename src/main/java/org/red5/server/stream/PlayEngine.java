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

package org.red5.server.stream;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.IAudioStreamCodec;
import org.red5.codec.IStreamCodecInfo;
import org.red5.codec.IVideoStreamCodec;
import org.red5.codec.IVideoStreamCodec.FrameData;
import org.red5.codec.StreamCodecInfo;
import org.red5.io.amf.Output;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.api.stream.OperationNotSupportedException;
import org.red5.server.api.stream.StreamState;
import org.red5.server.api.stream.support.DynamicPlayItem;
import org.red5.server.messaging.AbstractMessage;
import org.red5.server.messaging.IConsumer;
import org.red5.server.messaging.IFilter;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IMessageInput;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IPassive;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.Aggregate;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.FrameType;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.ResetMessage;
import org.red5.server.stream.message.StatusMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * A play engine for playing a IPlayItem.
 * 
 * @author The Red5 Project
 * @author Steven Gong
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Dan Rossi
 * @author Tiago Daniel Jacobs (tiago@imdt.com.br)
 * @author Vladimir Hmelyoff (vlhm@splitmedialabs.com)
 */
@Slf4j
public final class PlayEngine implements IFilter, IPushableConsumer, IPipeConnectionListener {
 
    private final AtomicReference<IMessageInput> msgInReference = new AtomicReference<>();

    private final AtomicReference<IMessageOutput> msgOutReference = new AtomicReference<>();

    private final ISubscriberStream subscriberStream;

    private ISchedulingService schedulingService;

    private IConsumerService consumerService;

    private IProviderService providerService;

    private Number streamId;
 
    private boolean receiveVideo = true;
 
    private boolean receiveAudio = true;

    private boolean pullMode;

    private String waitLiveJob;
    /**  
     * 	发送第一个包的时间戳
     */
    private AtomicInteger streamStartTS = new AtomicInteger(-1);

    private AtomicReference<IPlayItem> currentItem = new AtomicReference<>();

    private RTMPMessage pendingMessage;
 
    private int bufferCheckInterval = 0;
 
    private int underrunTrigger = 10;
 
    private int maxPendingVideoFrames = 10;

    /**
     * 	如果我们有超过1个挂起的视频帧，但小于最大挂起的视频帧，请继续发送，直到有这么多个超过1个挂起的连续帧
     */
    private int maxSequentialPendingVideoFrames = 10;
    /**
     * 	大于0个挂起帧的连续视频帧数
     */
    private int numSequentialPendingVideoFrames = 0;
    /**
     * 	实时流中视频帧丢弃的状态机
     */
    private IFrameDropper videoFrameDropper = new VideoFrameDropper();

    private int timestampOffset = 0;
    /**
     *	 上次发送到客户端的消息的时间戳。
     */
    private int lastMessageTs = -1;
    /**
     * Number of bytes sent.
     * 	发送bytes的数目
     */
    private AtomicLong bytesSent = new AtomicLong(0);

    /**
     *	流播放的开始时间。不是播放流的时间，而是应该播放流的时间如果
     *	从一开始就演奏。在时间戳5s的1:00:05播放流。后场是1:00:00。
     **/
    private volatile long playbackStart;
    /**
     * 	指示是否计划推拉作业的标志。作业确保将消息发送到客户端
     */
    private volatile String pullAndPush; 
    /**
     *	指示是否调度缓冲区用完后关闭流的作业的标志。
     */
    private volatile String deferredStop; 
    /**
     * 	监控给定推拉运行的防护完成情况。用于等待作业取消完成。
     */
    private final AtomicBoolean pushPullRunning = new AtomicBoolean(false); 
    /**
     * 	流开始处的偏移量（毫秒）。
     */
    private int streamOffset; 
    /**
     *	下一步应检查缓冲区是否不足的时间戳。
     */
    private long nextCheckBufferUnderrun; 
    /**
     * 	下一步发送空白音频包
     */
    private boolean sendBlankAudio; 
    /**
     * Decision: 0 for Live, 1 for File, 2 for Wait, 3 for N/A
     */
    private int playDecision = 3; 
    /**
     * Index of the buffered interframe to send instead of current frame
     */
    private int bufferedInterframeIdx = -1;

    /**
     * List of pending operations
     */
    private ConcurrentLinkedQueue<Runnable> pendingOperations = new ConcurrentLinkedQueue<>();

    // Keep count of dropped packets so we can log every so often.
    private long droppedPacketsCount;

    private long droppedPacketsCountLastLogTimestamp = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

    private long droppedPacketsCountLogInterval = 60 * 1000L;

    private boolean configsDone;

    /**
     * Constructs a new PlayEngine.
     */
    private PlayEngine(Builder builder) {
        subscriberStream = builder.subscriberStream;
        schedulingService = builder.schedulingService;
        consumerService = builder.consumerService;
        providerService = builder.providerService;
        // get the stream id
        streamId = subscriberStream.getStreamId();
    } 
    /**
     * 	更新IMessageOutput 为当前subscriberStream的输出
     */
    public void start() {
        if (log.isDebugEnabled()) {
            log.debug("start - subscriber stream state: {}", (subscriberStream != null ? subscriberStream.getState() : null));
        }
        switch (subscriberStream.getState()) {
            case UNINIT:
                // 如果在运行中先关闭在重新开始
                subscriberStream.setState(StreamState.STOPPED);
                IMessageOutput out = consumerService.getConsumerOutput(subscriberStream);
                //初始值为null  所以  null == null 更新 为out
                if (msgOutReference.compareAndSet(null, out)) {
                    out.subscribe(this, null);
                } else if (log.isDebugEnabled()) {
                    log.debug("Message output was already set for stream: {}", subscriberStream);
                }
                break;
            default:
                throw new IllegalStateException(String.format("Cannot start in current state: %s", subscriberStream.getState()));
        }
    }

    /**
     * Play stream 
     */
    public void play(IPlayItem item) throws StreamNotFoundException, IllegalStateException, IOException {
        play(item, true);
    }

    /** 
     * See: https://www.adobe.com/devnet/adobe-media-server/articles/dynstream_actionscript.html
     */
    public void play(IPlayItem item, boolean withReset) throws StreamNotFoundException, IllegalStateException, IOException {
        IMessageInput in = null;
        // cannot play if state is not stopped
        switch (subscriberStream.getState()) {
            case STOPPED:
                in = msgInReference.get();
                if (in != null) {
                    in.unsubscribe(this);
                    msgInReference.set(null);
                }
                break;
            default:
                throw new IllegalStateException("Cannot play from non-stopped state");
        }
        // Play type determination
        // https://help.adobe.com/en_US/FlashPlatform/reference/actionscript/3/flash/net/NetStream.html#play()
        // The start time, in seconds. Allowed values are -2, -1, 0, or a positive number. 
        // The default value is -2, which looks for a live stream, then a recorded stream, 
        // and if it finds neither, opens a live stream. 
        // If -1, plays only a live stream. 
        // If 0 or a positive number, plays a recorded stream, beginning start seconds in.
        //
        // -2: live then recorded, -1: live, >=0: recorded
        int type = (int) (item.getStart() / 1000);
        log.debug("Type {}", type);
        // see if it's a published stream
        IScope thisScope = subscriberStream.getScope();
        final String itemName = item.getName();
        //check for input and type
        IProviderService.INPUT_TYPE sourceType = providerService.lookupProviderInput(thisScope, itemName, type);
        boolean sendNotifications = true;
        // decision: 0 for Live, 1 for File, 2 for Wait, 3 for N/A
        switch (type) {
            case -2:
                if (sourceType == IProviderService.INPUT_TYPE.LIVE) {
                    playDecision = 0;
                } else if (sourceType == IProviderService.INPUT_TYPE.VOD) {
                    playDecision = 1;
                } else if (sourceType == IProviderService.INPUT_TYPE.LIVE_WAIT) {
                    playDecision = 2;
                }
                break;
            case -1:
                if (sourceType == IProviderService.INPUT_TYPE.LIVE) {
                    playDecision = 0;
                } else if (sourceType == IProviderService.INPUT_TYPE.LIVE_WAIT) {
                    playDecision = 2;
                }
                break;
            default:
                if (sourceType == IProviderService.INPUT_TYPE.VOD) {
                    playDecision = 1;
                }
                break;
        }
        IMessage msg = null;
        currentItem.set(item);
        long itemLength = item.getLength();
        if (log.isDebugEnabled()) {
            log.debug("Play decision is {} (0=Live, 1=File, 2=Wait, 3=N/A) item length: {}", playDecision, itemLength);
        }
        switch (playDecision) {
            case 0:
                // get source input without create
                in = providerService.getLiveProviderInput(thisScope, itemName, false);
                if (msgInReference.compareAndSet(null, in)) {
                    // drop all frames up to the next keyframe
                    videoFrameDropper.reset(IFrameDropper.SEND_KEYFRAMES_CHECK);
                    if (in instanceof IBroadcastScope) {
                        IBroadcastStream stream = (IBroadcastStream) ((IBroadcastScope) in).getClientBroadcastStream();
                        if (stream != null && stream.getCodecInfo() != null) {
                            IVideoStreamCodec videoCodec = stream.getCodecInfo().getVideoCodec();
                            if (videoCodec != null) {
                                if (withReset) {
                                    sendReset();
                                    sendResetStatus(item);
                                    sendStartStatus(item);
                                }
                                sendNotifications = false;
                                if (videoCodec.getNumInterframes() > 0 || videoCodec.getKeyframe() != null) {
                                    bufferedInterframeIdx = 0;
                                    videoFrameDropper.reset(IFrameDropper.SEND_ALL);
                                }
                            }
                        }
                    }
                    // subscribe to stream (ClientBroadcastStream.onPipeConnectionEvent)
                    in.subscribe(this, null);
                    // execute the processes to get Live playback setup
                    playLive();
                } else {
                    sendStreamNotFoundStatus(item);
                    throw new StreamNotFoundException(itemName);
                }
                break;
            case 2:
                // get source input with create
                in = providerService.getLiveProviderInput(thisScope, itemName, true);
                if (msgInReference.compareAndSet(null, in)) {
                    if (type == -1 && itemLength >= 0) {
                        if (log.isDebugEnabled()) {
                            log.debug("Creating wait job for {}", itemLength);
                        }
                        // Wait given timeout for stream to be published
                        waitLiveJob = schedulingService.addScheduledOnceJob(itemLength, new IScheduledJob() {
                            public void execute(ISchedulingService service) {
                                connectToProvider(itemName);
                                waitLiveJob = null;
                                subscriberStream.onChange(StreamState.END);
                            }
                        });
                    } else if (type == -2) {
                        if (log.isDebugEnabled()) {
                            log.debug("Creating wait job");
                        }
                        // Wait x seconds for the stream to be published
                        waitLiveJob = schedulingService.addScheduledOnceJob(15000, new IScheduledJob() {
                            public void execute(ISchedulingService service) {
                                connectToProvider(itemName);
                                waitLiveJob = null;
                            }
                        });
                    } else {
                        connectToProvider(itemName);
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("Message input already set for {}", itemName);
                }
                break;
            case 1:
                in = providerService.getVODProviderInput(thisScope, itemName);
                if (msgInReference.compareAndSet(null, in)) {
                    if (in.subscribe(this, null)) {
                        // execute the processes to get VOD playback setup
                        msg = playVOD(withReset, itemLength);
                    } else {
                        log.warn("Input source subscribe failed");
                        throw new IOException(String.format("Subscribe to %s failed", itemName));
                    }
                } else {
                    sendStreamNotFoundStatus(item);
                    throw new StreamNotFoundException(itemName);
                }
                break;
            default:
                sendStreamNotFoundStatus(item);
                throw new StreamNotFoundException(itemName);
        }
        //continue with common play processes (live and vod)
        if (sendNotifications) {
            if (withReset) {
                sendReset();
                sendResetStatus(item);
            }
            sendStartStatus(item);
            if (!withReset) {
                sendSwitchStatus();
            }
            // if its dynamic playback send the complete status
            if (item instanceof DynamicPlayItem) {
                sendTransitionStatus();
            }
        }
        if (msg != null) {
            sendMessage((RTMPMessage) msg);
        }
        subscriberStream.onChange(StreamState.PLAYING, item, !pullMode);
        if (withReset) {
            log.debug("Resetting times");
            long currentTime = System.currentTimeMillis();
            playbackStart = currentTime - streamOffset;
            nextCheckBufferUnderrun = currentTime + bufferCheckInterval;
            if (item.getLength() != 0) {
                ensurePullAndPushRunning();
            }
        }
    }

    /**
     * 在线流播放方法的逻辑
     * 1、变更播放状态
     * 2、获取与输入和输出流绑定的IMessageInput，IMessageOutput
     * 3、检测并发送 metadata数据
     * 4、发送vido的配置和关键帧
     * 5、发送audio的配置
     */
    private final void playLive() throws IOException {
        //1---变为播放状态---
        subscriberStream.setState(StreamState.PLAYING);
        //2---获取与输入和输出---
        IMessageInput in = msgInReference.get();
        IMessageOutput out = msgOutReference.get();
        if(in == null || out == null){
        	 throw new IOException(String.format("A message pipe is null - in: %b out: %b", (msgInReference == null), (msgOutReference == null)));
        } 
        configsDone = true;
        // 获取流以便我们可以获取任何元数据和解码器配置
        IBroadcastStream stream = (IBroadcastStream) ((IBroadcastScope) in).getClientBroadcastStream();
        // 创建播放列表并立即刷新时阻止NPE
        if (stream == null) {
        	return ;
        } 
        int ts = 0; 
        Notify metaData = stream.getMetaData();
        //3---检测并发送 metadata数据---
        if (metaData != null) {
            ts = metaData.getTimestamp();
            log.debug("Metadata is available");
            RTMPMessage metaMsg = RTMPMessage.build(metaData, metaData.getTimestamp());
            sendMessage(metaMsg);
        } else {
            log.debug("No metadata available");
        }
        IStreamCodecInfo codecInfo = stream.getCodecInfo();
        log.debug("Codec info: {}", codecInfo);
        if (!(codecInfo instanceof StreamCodecInfo)) {
        	return ;
        }
	    StreamCodecInfo info = (StreamCodecInfo) codecInfo; 
	    IVideoStreamCodec videoCodec = info.getVideoCodec();
	    log.debug("Video codec: {}", videoCodec);
	    //4、---发送vido的配置和关键帧---
	    if (videoCodec != null) {
	        // check for decoder configuration to send
	        IoBuffer config = videoCodec.getDecoderConfiguration();
	        if (config != null) {
	            log.debug("Decoder configuration is available for {}", videoCodec.getName());
	            VideoData conf = new VideoData(config, true);
	            log.debug("Pushing video decoder configuration");
	            sendMessage(RTMPMessage.build(conf, ts));
	        }
	        // check for keyframes to send
	        FrameData[] keyFrames = videoCodec.getKeyframes();
	        for (FrameData keyframe : keyFrames) {
	            log.debug("Keyframe is available");
	            VideoData video = new VideoData(keyframe.getFrame(), true);
	            log.debug("Pushing keyframe");
	            sendMessage(RTMPMessage.build(video, ts));
	        }
	    } else {
	        log.debug("No video decoder configuration available");
	    }
	     
	    IAudioStreamCodec audioCodec = info.getAudioCodec();
	    log.debug("Audio codec: {}", audioCodec);
	    //5、---发送audio的配置---
	    if (audioCodec != null) {
	        // check for decoder configuration to send
	        IoBuffer config = audioCodec.getDecoderConfiguration();
	        if (config != null) {
	            log.debug("Decoder configuration is available for {}", audioCodec.getName());
	            AudioData conf = new AudioData(config.asReadOnlyBuffer());
	            log.debug("Pushing audio decoder configuration");
	            sendMessage(RTMPMessage.build(conf, ts));
	        }
	    } else {
	        log.debug("No audio decoder configuration available");
	    }
    }

    /**
     * Performs the processes needed for VOD / pre-recorded streams. 
     * 	播放vod方法
     */
    private final IMessage playVOD(boolean withReset, long itemLength) throws IOException {
        IMessage msg = null;
        // change state
        subscriberStream.setState(StreamState.PLAYING);
        if (withReset) {
            releasePendingMessage();
        }
        sendVODInitCM(currentItem.get());
        //在发送netstream.play.start之前，不要使用pullAndPush来检测IOExceptions 
        int start = (int) currentItem.get().getStart();
        if (start > 0) {
            streamOffset = sendVODSeekCM(start);
            // 我们找到了最近的关键帧，所以现在就使用真正的时间戳
            if (streamOffset == -1) {
                streamOffset = start;
            }
        }
        IMessageInput in = msgInReference.get();
        msg = in.pullMessage();
        if (msg instanceof RTMPMessage) {
            // Only send first video frame
            IRTMPEvent body = ((RTMPMessage) msg).getBody();
            if (itemLength == 0) {
                while (body != null && !(body instanceof VideoData)) {
                    msg = in.pullMessage();
                    if (msg != null && msg instanceof RTMPMessage) {
                        body = ((RTMPMessage) msg).getBody();
                    } else {
                        break;
                    }
                }
            }
            if (body != null) {
                // Adjust timestamp when playing lists
                body.setTimestamp(body.getTimestamp() + timestampOffset);
            }
        }
        return msg;
    }

    /**
     * Connects to the data provider.
     * 
     * @param itemName
     *            name of the item to play
     */
    private final void connectToProvider(String itemName) {
        log.debug("Attempting connection to {}", itemName);
        IMessageInput in = msgInReference.get();
        if (in == null) {
            in = providerService.getLiveProviderInput(subscriberStream.getScope(), itemName, true);
            msgInReference.set(in);
        }
        if (in != null) {
            log.debug("Provider: {}", msgInReference.get());
            if (in.subscribe(this, null)) {
                log.debug("Subscribed to {} provider", itemName);
                // execute the processes to get Live playback setup
                try {
                    playLive();
                } catch (IOException e) {
                    log.warn("Could not play live stream: {}", itemName, e);
                }
            } else {
                log.warn("Subscribe to {} provider failed", itemName);
            }
        } else {
            log.warn("Provider was not found for {}", itemName);
            StreamService.sendNetStreamStatus(subscriberStream.getConnection(), StatusCodes.NS_PLAY_STREAMNOTFOUND, "Stream was not found", itemName, Status.ERROR, streamId);
        }
    }

    /**
     * Pause at position
     * 
     * @param position
     *            Position in file
     * @throws IllegalStateException
     *             If stream is stopped
     */
    public void pause(int position) throws IllegalStateException {
        // allow pause if playing or stopped
        switch (subscriberStream.getState()) {
            case PLAYING:
            case STOPPED:
                subscriberStream.setState(StreamState.PAUSED);
                clearWaitJobs();
                sendPauseStatus(currentItem.get());
                sendClearPing();
                subscriberStream.onChange(StreamState.PAUSED, currentItem.get(), position);
                break;
            default:
                throw new IllegalStateException("Cannot pause in current state");
        }
    }

    /**
     * Resume playback
     * 
     * @param position
     *            Resumes playback
     * @throws IllegalStateException
     *             If stream is stopped
     */
    public void resume(int position) throws IllegalStateException {
        // allow resume from pause
        switch (subscriberStream.getState()) {
            case PAUSED:
                subscriberStream.setState(StreamState.PLAYING);
                sendReset();
                sendResumeStatus(currentItem.get());
                if (pullMode) {
                    sendVODSeekCM(position);
                    subscriberStream.onChange(StreamState.RESUMED, currentItem.get(), position);
                    playbackStart = System.currentTimeMillis() - position;
                    long length = currentItem.get().getLength();
                    if (length >= 0 && (position - streamOffset) >= length) {
                        // Resume after end of stream
                        stop();
                    } else {
                        ensurePullAndPushRunning();
                    }
                } else {
                    subscriberStream.onChange(StreamState.RESUMED, currentItem.get(), position);
                    videoFrameDropper.reset(VideoFrameDropper.SEND_KEYFRAMES_CHECK);
                }
                break;
            default:
                throw new IllegalStateException("Cannot resume from non-paused state");
        }
    }

    /**
     * Seek to a given position
     * 
     * @param position
     *            Position
     * @throws IllegalStateException
     *             If stream is in stopped state
     * @throws OperationNotSupportedException
     *             If this object doesn't support the operation.
     */
    public void seek(int position) throws IllegalStateException, OperationNotSupportedException {
        // add this pending seek operation to the list
        pendingOperations.add(new SeekRunnable(position));
        cancelDeferredStop();
        ensurePullAndPushRunning();
    }

    /**
     * Stop playback
     * 
     * @throws IllegalStateException
     *             If stream is in stopped state
     */
    public void stop() throws IllegalStateException {
        if (log.isDebugEnabled()) {
            log.debug("stop - subscriber stream state: {}", (subscriberStream != null ? subscriberStream.getState() : null));
        }
        // allow stop if playing or paused
        switch (subscriberStream.getState()) {
            case PLAYING:
            case PAUSED:
                subscriberStream.setState(StreamState.STOPPED);
                IMessageInput in = msgInReference.get();
                if (in != null && !pullMode) {
                    in.unsubscribe(this);
                    msgInReference.set(null);
                }
                subscriberStream.onChange(StreamState.STOPPED, currentItem.get());
                clearWaitJobs();
                cancelDeferredStop();
                if (subscriberStream instanceof IPlaylistSubscriberStream) {
                    IPlaylistSubscriberStream pss = (IPlaylistSubscriberStream) subscriberStream;
                    if (!pss.hasMoreItems()) {
                        releasePendingMessage();
                        sendCompleteStatus();
                        bytesSent.set(0);
                        sendStopStatus(currentItem.get());
                        sendClearPing();
                    } else {
                        if (lastMessageTs > 0) {
                            // remember last timestamp so we can generate correct headers in playlists.
                            timestampOffset = lastMessageTs;
                        }
                        pss.nextItem();
                    }
                }
                break;
            case CLOSED:
                clearWaitJobs();
                cancelDeferredStop();
                break;
            case STOPPED:
                log.trace("Already in stopped state");
                break;
            default:
                throw new IllegalStateException(String.format("Cannot stop in current state: %s", subscriberStream.getState()));
        }
    }

    /**
     * Close stream
     */
    public void close() {
        if (log.isDebugEnabled()) {
            log.debug("close");
        }
        if (!subscriberStream.getState().equals(StreamState.CLOSED)) {
            IMessageInput in = msgInReference.get();
            if (in != null) {
                in.unsubscribe(this);
                msgInReference.set(null);
            }
            subscriberStream.setState(StreamState.CLOSED);
            clearWaitJobs();
            releasePendingMessage();
            lastMessageTs = 0;
            // XXX is clear ping required?
            //sendClearPing();
            InMemoryPushPushPipe out = (InMemoryPushPushPipe) msgOutReference.get();
            if (out != null) {
                List<IConsumer> consumers = out.getConsumers();
                // assume a list of 1 in most cases
                if (log.isDebugEnabled()) {
                    log.debug("Message out consumers: {}", consumers.size());
                }
                if (!consumers.isEmpty()) {
                    for (IConsumer consumer : consumers) {
                        out.unsubscribe(consumer);
                    }
                }
                msgOutReference.set(null);
            }
        } else {
            log.debug("Stream is already in closed state");
        }
    }

    /**
     * Check if it's okay to send the client more data. This takes the configured bandwidth as well as the requested client buffer into
     * account.
     * 
     * @param message
     * @return true if it is ok to send more, false otherwise
     */
    private boolean okayToSendMessage(IRTMPEvent message) {
        if (message instanceof IStreamData) {
            final long now = System.currentTimeMillis();
            // check client buffer size
            if (isClientBufferFull(now)) {
                return false;
            }
            // get pending message count
            long pending = pendingMessages();
            if (bufferCheckInterval > 0 && now >= nextCheckBufferUnderrun) {
                if (pending > underrunTrigger) {
                    // client is playing behind speed, notify him
                    sendInsufficientBandwidthStatus(currentItem.get());
                }
                nextCheckBufferUnderrun = now + bufferCheckInterval;
            }
            // check for under run
            if (pending > underrunTrigger) {
                // too many messages already queued on the connection
                return false;
            }
            return true;
        } else {
            String itemName = "Undefined";
            // if current item exists get the name to help debug this issue
            if (currentItem.get() != null) {
                itemName = currentItem.get().getName();
            }
            Object[] errorItems = new Object[] { message.getClass(), message.getDataType(), itemName };
            throw new RuntimeException(String.format("Expected IStreamData but got %s (type %s) for %s", errorItems));
        }
    }

    /**
     * Estimate client buffer fill. 
     */
    private boolean isClientBufferFull(final long now) {
        // check client buffer length when we've already sent some messages
        if (lastMessageTs > 0) {
            // duration the stream is playing / playback duration
            final long delta = now - playbackStart;
            // buffer size as requested by the client
            final long buffer = subscriberStream.getClientBufferDuration();
            // expected amount of data present in client buffer
            final long buffered = lastMessageTs - delta;
            log.trace("isClientBufferFull: timestamp {} delta {} buffered {} buffer duration {}", new Object[] { lastMessageTs, delta, buffered, buffer });
            // fix for SN-122, this sends double the size of the client buffer
            if (buffer > 0 && buffered > (buffer * 2)) {
                // client is likely to have enough data in the buffer
                return true;
            }
        }
        return false;
    }

    private boolean isClientBufferEmpty() {
        // check client buffer length when we've already sent some messages
        if (lastMessageTs >= 0) {
            // duration the stream is playing / playback duration
            final long delta = System.currentTimeMillis() - playbackStart;
            // expected amount of data present in client buffer
            final long buffered = lastMessageTs - delta;
            log.trace("isClientBufferEmpty: timestamp {} delta {} buffered {}", new Object[] { lastMessageTs, delta, buffered });
            if (buffered < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 确保推拉处理正在运行。
     */
    private void ensurePullAndPushRunning() {
        log.trace("State should be PLAYING to running this task: {}", subscriberStream.getState());
        if (pullMode && pullAndPush == null && subscriberStream.getState() == StreamState.PLAYING) {
            // client buffer is at least 100ms
            pullAndPush = subscriberStream.scheduleWithFixedDelay(new PullAndPushRunnable(), 10);
        }
    }

    /**
     * Clear all scheduled waiting jobs
     */
    private void clearWaitJobs() {
        log.debug("Clear wait jobs");
        if (pullAndPush != null) {
            subscriberStream.cancelJob(pullAndPush);
            releasePendingMessage();
            pullAndPush = null;
        }
        if (waitLiveJob != null) {
            schedulingService.removeScheduledJob(waitLiveJob);
            waitLiveJob = null;
        }
    }

    /**
     * Sends a status message. 
     */
    private void doPushMessage(Status status) {
        StatusMessage message = new StatusMessage();
        message.setBody(status);
        doPushMessage(message);
    }

    /**
     * Send message to output stream and handle exceptions. 
     */
    private void doPushMessage(AbstractMessage message) {
        if (log.isTraceEnabled()) {
            String msgType = message.getMessageType();
            log.trace("doPushMessage: {}", msgType);
        }
        
        IMessageOutput out = msgOutReference.get();
        if(out == null){
        	log.warn("Push message failed due to null output pipe");
        	return;
        } 
        
        try {
            out.pushMessage(message);
            
            if (message instanceof RTMPMessage) {
                IRTMPEvent body = ((RTMPMessage) message).getBody();
                //更新上次发送的消息的时间戳
                lastMessageTs = body.getTimestamp();
                IoBuffer streamData = null;
                if (body instanceof IStreamData && (streamData = ((IStreamData<?>) body).getData()) != null) {
                    bytesSent.addAndGet(streamData.limit());
                }
            }
        } catch (IOException err) {
            log.warn("Error while pushing message", err);
        }
         
    }

    /**
     * Send an RTMP message 
     */
    private void sendMessage(RTMPMessage messageIn) {
        IRTMPEvent eventIn = messageIn.getBody();
        IRTMPEvent event;
        switch (eventIn.getDataType()) {
            case Constants.TYPE_AGGREGATE:
                event = new Aggregate(((Aggregate) eventIn).getData());
                break;
            case Constants.TYPE_AUDIO_DATA:
                event = new AudioData(((AudioData) eventIn).getData());
                break;
            case Constants.TYPE_VIDEO_DATA:
                event = new VideoData(((VideoData) eventIn).getData());
                break;
            default:
                event = new Notify(((Notify) eventIn).getData());
                break;
        }
        // 获取传入事件时间
        int eventTime = eventIn.getTimestamp();
        // 获取传入事件源类型并设置传出事件
        event.setSourceType(eventIn.getSourceType());
        // 实例化传出消息
        RTMPMessage messageOut = RTMPMessage.build(event, eventTime);
        if (log.isTraceEnabled()) {
            log.trace("Source type - in: {} out: {}", eventIn.getSourceType(), messageOut.getBody().getSourceType());
            long delta = System.currentTimeMillis() - playbackStart;
            log.trace("sendMessage: streamStartTS {}, length {}, streamOffset {}, timestamp {} last timestamp {} delta {} buffered {}", new Object[] { streamStartTS.get(), currentItem.get().getLength(), streamOffset, eventTime, lastMessageTs, delta, lastMessageTs - delta });
        }
        if (playDecision == 1) { // 1 == vod/file
            if (eventTime > 0 && streamStartTS.compareAndSet(-1, eventTime)) {
                log.debug("sendMessage: set streamStartTS");
                messageOut.getBody().setTimestamp(0);
            }
            long length = currentItem.get().getLength();
            if (length >= 0) {
                int duration = eventTime - streamStartTS.get();
                if (log.isTraceEnabled()) {
                    log.trace("sendMessage duration={} length={}", duration, length);
                }
                if (duration - streamOffset >= length) {
                    // sent enough data to client
                    stop();
                    return;
                }
            }
        } else {
            // 对于实时流，不要将streamstartts重置为0
            if (eventTime > 0 && streamStartTS.compareAndSet(-1, eventTime)) {
                log.debug("sendMessage: set streamStartTS");
            }
            //实时流的相对时间戳调整
            int startTs = streamStartTS.get();
            if (startTs > 0) {
                // 减去流开始为客户端播放时的偏移时间
                eventTime -= startTs;
                messageOut.getBody().setTimestamp(eventTime);
                if (log.isTraceEnabled()) {
                    log.trace("sendMessage (updated): streamStartTS={}, length={}, streamOffset={}, timestamp={}", new Object[] { startTs, currentItem.get().getLength(), streamOffset, eventTime });
                }
            }
        }
        doPushMessage(messageOut);
    }

    /**
     * 发送清除ping。让客户端知道流没有更多的数据要发送。
     */
    private void sendClearPing() {
        Ping eof = new Ping();
        eof.setEventType(Ping.STREAM_PLAYBUFFER_CLEAR);
        eof.setValue2(streamId);
        // eos 
        RTMPMessage eofMsg = RTMPMessage.build(eof);
        doPushMessage(eofMsg);
    }

    /**
     * Send reset message
     */
    private void sendReset() {
        if (pullMode) {
            Ping recorded = new Ping();
            recorded.setEventType(Ping.RECORDED_STREAM);
            recorded.setValue2(streamId);
            // recorded 
            RTMPMessage recordedMsg = RTMPMessage.build(recorded);
            doPushMessage(recordedMsg);
        }
        Ping begin = new Ping();
        begin.setEventType(Ping.STREAM_BEGIN);
        begin.setValue2(streamId);
        // begin 
        RTMPMessage beginMsg = RTMPMessage.build(begin);
        doPushMessage(beginMsg);
        // reset
        ResetMessage reset = new ResetMessage();
        doPushMessage(reset);
    }

    /**
     * Send reset status for item 
     */
    private void sendResetStatus(IPlayItem item) {
        Status reset = new Status(StatusCodes.NS_PLAY_RESET);
        reset.setClientid(streamId);
        reset.setDetails(item.getName());
        reset.setDesciption(String.format("Playing and resetting %s.", item.getName()));

        doPushMessage(reset);
    }

    /**
     * Send playback start status notification 
     */
    private void sendStartStatus(IPlayItem item) {
        Status start = new Status(StatusCodes.NS_PLAY_START);
        start.setClientid(streamId);
        start.setDetails(item.getName());
        start.setDesciption(String.format("Started playing %s.", item.getName()));

        doPushMessage(start);
    }

    /**
     * Send playback stoppage status notification 
     */
    private void sendStopStatus(IPlayItem item) {
        Status stop = new Status(StatusCodes.NS_PLAY_STOP);
        stop.setClientid(streamId);
        stop.setDesciption(String.format("Stopped playing %s.", item.getName()));
        stop.setDetails(item.getName());

        doPushMessage(stop);
    }

    /**
     * Sends an onPlayStatus message.
     * 
     * http://help.adobe.com/en_US/FlashPlatform/reference/actionscript/3/flash/events/NetDataEvent.html
     
     */
    private void sendOnPlayStatus(String code, int duration, long bytes) {
        if (log.isDebugEnabled()) {
            log.debug("Sending onPlayStatus - code: {} duration: {} bytes: {}", code, duration, bytes);
        }
        // create the buffer
        IoBuffer buf = IoBuffer.allocate(102);
        buf.setAutoExpand(true);
        Output out = new Output(buf);
        out.writeString("onPlayStatus");
        ObjectMap<Object, Object> args = new ObjectMap<>();
        args.put("code", code);
        args.put("level", Status.STATUS);
        args.put("duration", duration);
        args.put("bytes", bytes);
        String name = currentItem.get().getName();
        if (StatusCodes.NS_PLAY_TRANSITION_COMPLETE.equals(code)) {
            args.put("clientId", streamId);
            args.put("details", name);
            args.put("description", String.format("Transitioned to %s", name));
            args.put("isFastPlay", false);
        }
        out.writeObject(args);
        buf.flip();
        Notify event = new Notify(buf, "onPlayStatus");
        if (lastMessageTs > 0) {
            event.setTimestamp(lastMessageTs);
        } else {
            event.setTimestamp(0);
        }
        RTMPMessage msg = RTMPMessage.build(event);
        doPushMessage(msg);
    }

    /**
     * Send playlist switch status notification
     */
    private void sendSwitchStatus() {
        // TODO: find correct duration to send
        sendOnPlayStatus(StatusCodes.NS_PLAY_SWITCH, 1, bytesSent.get());
    }

    /**
     * Send transition status notification
     */
    private void sendTransitionStatus() {
        sendOnPlayStatus(StatusCodes.NS_PLAY_TRANSITION_COMPLETE, 0, bytesSent.get());
    }

    /**
     * Send playlist complete status notification
     *
     */
    private void sendCompleteStatus() {
        // may be the correct duration
        int duration = (lastMessageTs > 0) ? Math.max(0, lastMessageTs - streamStartTS.get()) : 0;
        if (log.isDebugEnabled()) {
            log.debug("sendCompleteStatus - duration: {} bytes sent: {}", duration, bytesSent.get());
        }
        sendOnPlayStatus(StatusCodes.NS_PLAY_COMPLETE, duration, bytesSent.get());
    }

    /**
     * Send seek status notification 
     */
    private void sendSeekStatus(IPlayItem item, int position) {
        Status seek = new Status(StatusCodes.NS_SEEK_NOTIFY);
        seek.setClientid(streamId);
        seek.setDetails(item.getName());
        seek.setDesciption(String.format("Seeking %d (stream ID: %d).", position, streamId));

        doPushMessage(seek);
    }

    /**
     * Send pause status notification 
     */
    private void sendPauseStatus(IPlayItem item) {
        Status pause = new Status(StatusCodes.NS_PAUSE_NOTIFY);
        pause.setClientid(streamId);
        pause.setDetails(item.getName());

        doPushMessage(pause);
    }

    /**
     * Send resume status notification 
     */
    private void sendResumeStatus(IPlayItem item) {
        Status resume = new Status(StatusCodes.NS_UNPAUSE_NOTIFY);
        resume.setClientid(streamId);
        resume.setDetails(item.getName());

        doPushMessage(resume);
    }

    /**
     * Send published status notification 
     */
    private void sendPublishedStatus(IPlayItem item) {
        Status published = new Status(StatusCodes.NS_PLAY_PUBLISHNOTIFY);
        published.setClientid(streamId);
        published.setDetails(item.getName());

        doPushMessage(published);
    }

    /**
     * Send unpublished status notification 
     */
    private void sendUnpublishedStatus(IPlayItem item) {
        Status unpublished = new Status(StatusCodes.NS_PLAY_UNPUBLISHNOTIFY);
        unpublished.setClientid(streamId);
        unpublished.setDetails(item.getName());

        doPushMessage(unpublished);
    }

    /**
     * Stream not found status notification 
     */
    private void sendStreamNotFoundStatus(IPlayItem item) {
        Status notFound = new Status(StatusCodes.NS_PLAY_STREAMNOTFOUND);
        notFound.setClientid(streamId);
        notFound.setLevel(Status.ERROR);
        notFound.setDetails(item.getName());

        doPushMessage(notFound);
    }

    /**
     * Insufficient bandwidth notification 
     */
    private void sendInsufficientBandwidthStatus(IPlayItem item) {
        Status insufficientBW = new Status(StatusCodes.NS_PLAY_INSUFFICIENT_BW);
        insufficientBW.setClientid(streamId);
        insufficientBW.setLevel(Status.WARNING);
        insufficientBW.setDetails(item.getName());
        insufficientBW.setDesciption("Data is playing behind the normal speed.");

        doPushMessage(insufficientBW);
    }

    /**
     * Send VOD init control message 
     */
    private void sendVODInitCM(IPlayItem item) {
        OOBControlMessage oobCtrlMsg = new OOBControlMessage();
        oobCtrlMsg.setTarget(IPassive.KEY);
        oobCtrlMsg.setServiceName("init");
        Map<String, Object> paramMap = new HashMap<String, Object>(1);
        paramMap.put("startTS", (int) item.getStart());
        oobCtrlMsg.setServiceParamMap(paramMap);
        msgInReference.get().sendOOBControlMessage(this, oobCtrlMsg);
    }

    /**
     * Send VOD seek control message 
     */
    private int sendVODSeekCM(int position) {
        OOBControlMessage oobCtrlMsg = new OOBControlMessage();
        oobCtrlMsg.setTarget(ISeekableProvider.KEY);
        oobCtrlMsg.setServiceName("seek");
        Map<String, Object> paramMap = new HashMap<String, Object>(1);
        paramMap.put("position", position);
        oobCtrlMsg.setServiceParamMap(paramMap);
        msgInReference.get().sendOOBControlMessage(this, oobCtrlMsg);
        if (oobCtrlMsg.getResult() instanceof Integer) {
            return (Integer) oobCtrlMsg.getResult();
        } else {
            return -1;
        }
    }
 
    private boolean sendCheckVideoCM() {
        OOBControlMessage oobCtrlMsg = new OOBControlMessage();
        oobCtrlMsg.setTarget(IStreamTypeAwareProvider.KEY);
        oobCtrlMsg.setServiceName("hasVideo");
        msgInReference.get().sendOOBControlMessage(this, oobCtrlMsg);
        if (oobCtrlMsg.getResult() instanceof Boolean) {
            return (Boolean) oobCtrlMsg.getResult();
        } else {
            return false;
        }
    }

    
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
        if ("ConnectionConsumer".equals(oobCtrlMsg.getTarget())) {
            if (source instanceof IProvider) {
                IMessageOutput out = msgOutReference.get();
                if (out != null) {
                    out.sendOOBControlMessage((IProvider) source, oobCtrlMsg);
                } else {
                    // this may occur when a client attempts to play and then disconnects
                    log.warn("Output is not available, message cannot be sent");
                    close();
                }
            }
        }
    }

    
    public void onPipeConnectionEvent(PipeConnectionEvent event) {
        switch (event.getType()) {
            case PROVIDER_CONNECT_PUSH:
                if (event.getProvider() != this) {
                    if (waitLiveJob != null) {
                        schedulingService.removeScheduledJob(waitLiveJob);
                        waitLiveJob = null;
                    }
                    sendPublishedStatus(currentItem.get());
                }
                break;
            case PROVIDER_DISCONNECT:
                if (pullMode) {
                    sendStopStatus(currentItem.get());
                } else {
                    sendUnpublishedStatus(currentItem.get());
                }
                break;
            case CONSUMER_CONNECT_PULL:
                if (event.getConsumer() == this) {
                    pullMode = true;
                }
                break;
            case CONSUMER_CONNECT_PUSH:
                if (event.getConsumer() == this) {
                    pullMode = false;
                }
                break;
            default:
                if (log.isDebugEnabled()) {
                    log.debug("Unhandled pipe event: {}", event);
                }
        }
    }

    private boolean shouldLogPacketDrop() {
        long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        if (now - droppedPacketsCountLastLogTimestamp > droppedPacketsCountLogInterval) {
            droppedPacketsCountLastLogTimestamp = now;
            return true;
        } 
        return false;
    }
    
    /**
     * 1、由InMemoryPushPushPipe启动Consumer和Provider
     * 2、Provider的pushMessage发送消息
     * 3、Consumer的pushMessage接受消息
     */
    @Override
    public void pushMessage(IPipe pipe, IMessage message) throws IOException {
        if (!pullMode) {
            if (!configsDone) {
                log.debug("dump early");
                return;
            }
        }
        String sessionId = subscriberStream.getConnection().getSessionId();
        if (message instanceof RTMPMessage) {
            
        	IMessageInput msgIn = msgInReference.get();
            RTMPMessage rtmpMessage = (RTMPMessage) message;
            IRTMPEvent body = rtmpMessage.getBody();
            
            if(!(body instanceof IStreamData)){
            	throw new RuntimeException(String.format("Expected IStreamData but got %s (type %s)", body.getClass(), body.getDataType()));
            }  
            // the subscriber paused 
            if (subscriberStream.getState() == StreamState.PAUSED) {
                if (log.isInfoEnabled() && shouldLogPacketDrop()) {
                    log.info("Dropping packet because we are paused. sessionId={} stream={} count={}", sessionId, subscriberStream.getBroadcastStreamPublishName(), droppedPacketsCount);
                }
                videoFrameDropper.dropPacket(rtmpMessage);
                return;
            }
            
            if (body instanceof VideoData && body.getSourceType() == Constants.SOURCE_TYPE_LIVE) {
                // 我们只想从实时流中丢弃数据包。视频点播流我们让它缓冲。
                // 我们不希望观看电影的用户因为带宽低而观看不稳定的视频。
            	if (!(msgIn instanceof IBroadcastScope)) {
            		sendMessage(rtmpMessage);
            		return;
            	} 
                 
                IBroadcastStream stream = (IBroadcastStream) ((IBroadcastScope) msgIn).getClientBroadcastStream();
                if (stream == null || stream.getCodecInfo() == null) {
                	sendMessage(rtmpMessage);
            		return;
                } 
                	
                IVideoStreamCodec videoCodec = stream.getCodecInfo().getVideoCodec();
                // 如果视频编解码器为空，不要尝试丢弃帧
                if (videoCodec == null || !videoCodec.canDropFrames()) {
                	sendMessage(rtmpMessage);
            		return;
                }
                
                if (!receiveVideo) {
                    videoFrameDropper.dropPacket(rtmpMessage);
                    droppedPacketsCount++;
                    if (log.isInfoEnabled() && shouldLogPacketDrop()) {
                        // client disabled video or the app doesn't have enough bandwidth allowed for this stream
                        log.info("Drop packet. Failed to acquire token or no video. sessionId={} stream={} count={}", sessionId, subscriberStream.getBroadcastStreamPublishName(), droppedPacketsCount);
                    }
                    return;
                }
                //对视频流实施某种背压。当客户处于拥挤状态时connection，red5无法足够快地发送数据包。米娜把这些包放进
                //unbounded队列。如果我们生成视频包足够快，队列就会变大 可能会触发OutOfMemory异常。为了缓解这种情况，我们检查
                //挂起视频消息并丢弃视频数据包，直到队列低于阈值。仅检查编解码器是否支持帧丢弃
                long pendingVideos = pendingVideoMessages();
                if (log.isTraceEnabled()) {
                    log.trace("Pending messages sessionId={} pending={} threshold={} sequential={} stream={}, count={}", new Object[] { sessionId, pendingVideos, maxPendingVideoFrames, numSequentialPendingVideoFrames, subscriberStream.getBroadcastStreamPublishName(), droppedPacketsCount });
                }
                if (!videoFrameDropper.canSendPacket(rtmpMessage, pendingVideos)) {
                    // 删除帧，因为它依赖于以前删除的其他帧
                    droppedPacketsCount++;
                    if (log.isInfoEnabled() && shouldLogPacketDrop()) {
                        log.info("Frame dropper says to drop packet. sessionId={} stream={} count={}", sessionId, subscriberStream.getBroadcastStreamPublishName(), droppedPacketsCount);
                    }
                    return;
                }
                // 按顺序增加挂起视频帧的次数
                if (pendingVideos > 1) {
                    numSequentialPendingVideoFrames++;
                } else {
                    //如果1或0挂起，则重置顺序挂起的帧数
                    numSequentialPendingVideoFrames = 0;
                }
                if (pendingVideos > maxPendingVideoFrames || numSequentialPendingVideoFrames > maxSequentialPendingVideoFrames) {
                    droppedPacketsCount++;
                    if (log.isInfoEnabled() && shouldLogPacketDrop()) {
                        log.info("Drop packet. Pending above threshold. sessionId={} pending={} threshold={} sequential={} stream={} count={}", new Object[] { sessionId, pendingVideos, maxPendingVideoFrames, numSequentialPendingVideoFrames, subscriberStream.getBroadcastStreamPublishName(), droppedPacketsCount });
                    }
                    // drop because the client has insufficient bandwidth
                    long now = System.currentTimeMillis();
                    if (bufferCheckInterval > 0 && now >= nextCheckBufferUnderrun) {
                        // notify client about frame dropping (keyframe)
                        sendInsufficientBandwidthStatus(currentItem.get());
                        nextCheckBufferUnderrun = now + bufferCheckInterval;
                    }
                    videoFrameDropper.dropPacket(rtmpMessage);
                    return;
                }
                // we are ok to send, check if we should send buffered frame
                if (bufferedInterframeIdx > -1) {
                    IVideoStreamCodec.FrameData fd = videoCodec.getInterframe(bufferedInterframeIdx++);
                    if (fd != null) {
                        VideoData interframe = new VideoData(fd.getFrame());
                        interframe.setTimestamp(body.getTimestamp());
                        rtmpMessage = RTMPMessage.build(interframe);
                    } else {
                        // it means that new keyframe was received and we should send current frames instead of buffered
                        bufferedInterframeIdx = -1;
                    }
                }
            } else if (body instanceof AudioData) {
                if (!receiveAudio && sendBlankAudio) {
                    // send blank audio packet to reset player
                    sendBlankAudio = false;
                    body = new AudioData();
                    if (lastMessageTs > 0) {
                        body.setTimestamp(lastMessageTs);
                    } else {
                        body.setTimestamp(0);
                    }
                    rtmpMessage = RTMPMessage.build(body);
                } else if (!receiveAudio) {
                    return;
                }
            }
            sendMessage(rtmpMessage);
            
        } else if (message instanceof ResetMessage) {
            sendReset();
        } else {
            msgOutReference.get().pushMessage(message);
        }
    }

    /**
     * Get number of pending video messages 
     */
    private long pendingVideoMessages() {
        IMessageOutput out = msgOutReference.get();
        if (out != null) {
            OOBControlMessage pendingRequest = new OOBControlMessage();
            pendingRequest.setTarget("ConnectionConsumer");
            pendingRequest.setServiceName("pendingVideoCount");
            out.sendOOBControlMessage(this, pendingRequest);
            if (pendingRequest.getResult() != null) {
                return (Long) pendingRequest.getResult();
            }
        }
        return 0;
    }
 
    private long pendingMessages() {
        return subscriberStream.getConnection().getPendingMessages();
    }

    public boolean isPullMode() {
        return pullMode;
    }

    public boolean isPaused() {
        return subscriberStream.isPaused();
    }
 
    public int getLastMessageTimestamp() {
        return lastMessageTs;
    }

    public long getPlaybackStart() {
        return playbackStart;
    }

    public void sendBlankAudio(boolean sendBlankAudio) {
        this.sendBlankAudio = sendBlankAudio;
    }
 
    public boolean receiveAudio() {
        return receiveAudio;
    }
 
    public boolean receiveAudio(boolean receive) {
        boolean oldValue = receiveAudio;
        //set new value
        if (receiveAudio != receive) {
            receiveAudio = receive;
        }
        return oldValue;
    }
 
    public boolean receiveVideo() {
        return receiveVideo;
    }
 
    public boolean receiveVideo(boolean receive) {
        boolean oldValue = receiveVideo;
        //set new value
        if (receiveVideo != receive) {
            receiveVideo = receive;
        }
        return oldValue;
    }
 
    private void releasePendingMessage() {
        if (pendingMessage == null) {
        	return;
        } 
        IRTMPEvent body = pendingMessage.getBody();
        if (body instanceof IStreamData && ((IStreamData<?>) body).getData() != null) {
            ((IStreamData<?>) body).getData().free();
        }
        pendingMessage = null; 
    }
 
    protected boolean checkSendMessageEnabled(RTMPMessage message) {
        IRTMPEvent body = message.getBody();
        if (!receiveAudio && body instanceof AudioData) {
            // The user doesn't want to get audio packets
            ((IStreamData<?>) body).getData().free();
            if (sendBlankAudio) {
                // Send reset audio packet
                sendBlankAudio = false;
                body = new AudioData();
                // We need a zero timestamp
                if (lastMessageTs >= 0) {
                    body.setTimestamp(lastMessageTs - timestampOffset);
                } else {
                    body.setTimestamp(-timestampOffset);
                }
                message = RTMPMessage.build(body);
            } else {
                return false;
            }
        } else if (!receiveVideo && body instanceof VideoData) {
            // The user doesn't want to get video packets
            ((IStreamData<?>) body).getData().free();
            return false;
        }
        return true;
    }
 
    private void runDeferredStop() {
        // Stop current jobs from running.
        clearWaitJobs();
        // Schedule deferred stop executor.
        log.trace("Ran deferred stop");
        if (deferredStop == null) {
            // set deferred stop if we get a job name returned 
            deferredStop = subscriberStream.scheduleWithFixedDelay(new DeferredStopRunnable(), 100);
        }
    }

    private void cancelDeferredStop() {
        log.debug("Cancel deferred stop");
        if (deferredStop != null) {
            subscriberStream.cancelJob(deferredStop);
            deferredStop = null;
        }
    }
 
    private final class SeekRunnable implements Runnable {

        private final int position;

        SeekRunnable(int position) {
            this.position = position;
        }

        @SuppressWarnings("incomplete-switch")
        public void run() {
            log.trace("Seek: {}", position);
            boolean startPullPushThread = false;
            switch (subscriberStream.getState()) {
                case PLAYING:
                    startPullPushThread = true;
                case PAUSED:
                case STOPPED:
                    //allow seek if playing, paused, or stopped
                    if (!pullMode) {
                        // throw new OperationNotSupportedException();
                        throw new RuntimeException();
                    }
                    releasePendingMessage();
                    clearWaitJobs();
                    break;
                default:
                    throw new IllegalStateException("Cannot seek in current state");
            }
            sendClearPing();
            sendReset();
            sendSeekStatus(currentItem.get(), position);
            sendStartStatus(currentItem.get());
            int seekPos = sendVODSeekCM(position);
            // 我们找到了最近的关键帧，所以现在就使用真正的时间戳
            if (seekPos == -1) {
                seekPos = position;
            }
            //what should our start be?
            log.trace("Current playback start: {}", playbackStart);
            playbackStart = System.currentTimeMillis() - seekPos;
            log.trace("Playback start: {} seek pos: {}", playbackStart, seekPos);
            subscriberStream.onChange(StreamState.SEEK, currentItem.get(), seekPos);
            //从没有发送任何消息开始
            boolean messageSent = false;
            //阅读我们的客户状态
            switch (subscriberStream.getState()) {
                case PAUSED:
                case STOPPED:
                    //暂停时发送一个快照
                	if (!sendCheckVideoCM()) {
                		break;
                	} 
                	
                    IMessage msg = null;
                    IMessageInput in = msgInReference.get();
                    do {
                        try {
                            msg = in.pullMessage();
                        } catch (Throwable err) {
                            log.warn("Error while pulling message", err);
                            break;
                        }
                        if (!(msg instanceof RTMPMessage)) {
                        	continue;
                        } 
                        RTMPMessage rtmpMessage = (RTMPMessage) msg;
                        IRTMPEvent body = rtmpMessage.getBody();
                        if (body instanceof VideoData && ((VideoData) body).getFrameType() == FrameType.KEYFRAME) {
                            //body.setTimestamp(seekPos);
                            doPushMessage(rtmpMessage);
                            rtmpMessage.getBody().release();
                            messageSent = true;
                            lastMessageTs = body.getTimestamp();
                            break;
                        } 
                    } while (msg != null);
            }
            // 从河的尽头探出
            long length = currentItem.get().getLength();
            if (length >= 0 && (position - streamOffset) >= length) {
                stop();
            }
            // 如果此时未发送任何消息，请发送音频数据包
            if (!messageSent) {
                // 发送空白音频包通知客户端新位置
                log.debug("Sending blank audio packet");
                AudioData audio = new AudioData();
                audio.setTimestamp(seekPos);
                audio.setHeader(new Header());
                audio.getHeader().setTimer(seekPos);
                RTMPMessage audioMessage = RTMPMessage.build(audio);
                lastMessageTs = seekPos;
                doPushMessage(audioMessage);
                audioMessage.getBody().release();
            }
            if (!messageSent && subscriberStream.getState() == StreamState.PLAYING) {
                boolean isRTMPTPlayback = subscriberStream.getConnection().getProtocol().equals("rtmpt");
                // send all frames from last keyframe up to requested position and fill client buffer
                if (sendCheckVideoCM()) {
                    final long clientBuffer = subscriberStream.getClientBufferDuration();
                    IMessage msg = null;
                    IMessageInput in = msgInReference.get();
                    int msgSent = 0;
                    do {
                        try {
                            msg = in.pullMessage();
                            if (!(msg instanceof RTMPMessage)) {
                            	continue;
                            } 
                            RTMPMessage rtmpMessage = (RTMPMessage) msg;
                            IRTMPEvent body = rtmpMessage.getBody();
                            if (body.getTimestamp() >= position + (clientBuffer * 2)) {
                                // 客户端缓冲区现在应该已经满了，继续正常的拉/推
                                releasePendingMessage();
                                if (checkSendMessageEnabled(rtmpMessage)) {
                                    pendingMessage = rtmpMessage;
                                }
                                break;
                            }
                            if (!checkSendMessageEnabled(rtmpMessage)) {
                                continue;
                            }
                            msgSent++;
                            sendMessage(rtmpMessage);
                            
                        } catch (Throwable err) {
                            log.warn("Error while pulling message", err);
                            break;
                        }
                    } while (!isRTMPTPlayback && (msg != null));
                    log.trace("msgSent: {}", msgSent);
                    playbackStart = System.currentTimeMillis() - lastMessageTs;
                }
            }
            // 开始推拉
            if (startPullPushThread) {
                ensurePullAndPushRunning();
            }
        }
    }

    /**
     * 	由执行器定期触发以向客户端发送消息。
     */
    private final class PullAndPushRunnable implements IScheduledJob{ 
        /**
         * 	触发消息发送。
         */
        public void execute(ISchedulingService svc) {
            // 确保作业尚未运行
        	if (!pushPullRunning.compareAndSet(false, true)) {
        		log.debug("Push / pull already running");
        	}
            
            try {
                // 处理任何挂起的操作
                Runnable worker = null;
                while (!pendingOperations.isEmpty()) {
                    log.debug("Pending operations: {}", pendingOperations.size());
                    //删除第一个操作并执行它
                    worker = pendingOperations.remove();
                    log.debug("Worker: {}", worker);
                    // 如果操作是seek，请确保它是集合中的最后一个请求
                    while (worker instanceof SeekRunnable) {
                        Runnable tmp = pendingOperations.peek();
                        if (tmp != null && tmp instanceof SeekRunnable) {
                            worker = pendingOperations.remove();
                        } else {
                            break;
                        }
                    }
                    if (worker != null) {
                        log.debug("Executing pending operation");
                        worker.run();
                    }
                }
                // 如果消息是数据（不是音频或视频），则接收然后发送
                if (subscriberStream.getState() != StreamState.PLAYING || !pullMode) {
                	return;
                }
                 
                if (pendingMessage != null) {
                    IRTMPEvent body = pendingMessage.getBody();
                    if (okayToSendMessage(body)) {
                        sendMessage(pendingMessage);
                        releasePendingMessage();
                    } else {
                        return;
                    }
                } else {
                	IMessage msg = null;
                    IMessageInput in = msgInReference.get();
                    do { 
                    	msg = in.pullMessage();
                        if (msg == null) {
                        	// No more packets to send
                            log.debug("Ran out of packets");
                            runDeferredStop();
                            continue;
                        }
                        if (!(msg instanceof RTMPMessage)){
                        	continue;
                        }
                        
                        RTMPMessage rtmpMessage = (RTMPMessage) msg;
                        if (checkSendMessageEnabled(rtmpMessage)) {
                            // Adjust timestamp when playing lists
                            IRTMPEvent body = rtmpMessage.getBody();
                            body.setTimestamp(body.getTimestamp() + timestampOffset);
                            if (okayToSendMessage(body)) {
                                log.trace("ts: {}", rtmpMessage.getBody().getTimestamp());
                                sendMessage(rtmpMessage);
                                IoBuffer data = ((IStreamData<?>) body).getData();
                                if (data != null) {
                                    data.free();
                                }
                            } else {
                                pendingMessage = rtmpMessage;
                            }
                            ensurePullAndPushRunning();
                            break;
                        }
                    } while (msg != null);
                } 
            } catch (IOException err) {
                // 我们无法获取更多数据，请停止流。
                log.warn("Error while getting message", err);
                runDeferredStop();
            } finally {
                // 重置运行标志
                pushPullRunning.compareAndSet(true, false);
            }
             
        }
    }

    private class DeferredStopRunnable implements IScheduledJob {

        public void execute(ISchedulingService service) {
            if (isClientBufferEmpty()) {
                log.trace("Buffer is empty, stop will proceed");
                stop();
            }
        }

    }

    /**
     * @param maxPendingVideoFrames
     *            the maxPendingVideoFrames to set
     */
    public void setMaxPendingVideoFrames(int maxPendingVideoFrames) {
        this.maxPendingVideoFrames = maxPendingVideoFrames;
    }

    /**
     * @param maxSequentialPendingVideoFrames
     *            the maxSequentialPendingVideoFrames to set
     */
    public void setMaxSequentialPendingVideoFrames(int maxSequentialPendingVideoFrames) {
        this.maxSequentialPendingVideoFrames = maxSequentialPendingVideoFrames;
    }

    public void setBufferCheckInterval(int bufferCheckInterval) {
        this.bufferCheckInterval = bufferCheckInterval;
    }

    public void setUnderrunTrigger(int underrunTrigger) {
        this.underrunTrigger = underrunTrigger;
    }

    void setMessageOut(IMessageOutput msgOut) {
        this.msgOutReference.set(msgOut);
    }

    /**
     * Builder pattern
     */
    public final static class Builder {
        //Required for play engine
        private ISubscriberStream subscriberStream;

        //Required for play engine
        private ISchedulingService schedulingService;

        //Required for play engine
        private IConsumerService consumerService;

        //Required for play engine
        private IProviderService providerService;

        public Builder(ISubscriberStream subscriberStream, ISchedulingService schedulingService, IConsumerService consumerService, IProviderService providerService) {
            this.subscriberStream = subscriberStream;
            this.schedulingService = schedulingService;
            this.consumerService = consumerService;
            this.providerService = providerService;
        }

        public PlayEngine build() {
            return new PlayEngine(this);
        }

    }
}
