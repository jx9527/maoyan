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
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;
import org.red5.server.jmx.mxbeans.RTMPMinaConnectionMXBean;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.ClientBW;
import org.red5.server.net.rtmp.event.ServerBW;
import org.red5.server.net.rtmp.message.Packet;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Represents an RTMP connection using Mina.
 * 
 * @see "http://mina.apache.org/report/trunk/apidocs/org/apache/mina/core/session/IoSession.html"
 * 
 * @author Paul Gregoire
 */
@Slf4j
@ManagedResource
public class RTMPMinaConnection extends RTMPConnection implements RTMPMinaConnectionMXBean {
  
    private final AtomicBoolean closing = new AtomicBoolean(false);
 
    private transient IoSession ioSession;
 
    private ObjectName oName;

    protected int defaultServerBandwidth = 10000000;

    protected int defaultClientBandwidth = 10000000;

    protected boolean bandwidthDetection = true;

    /** Constructs a new RTMPMinaConnection. */
    @ConstructorProperties(value = { "persistent" })
    public RTMPMinaConnection() {
        super(IConnection.Type.PERSISTENT.name().toLowerCase());
    }
 
    @Override
    public boolean connect(IScope newScope, Object[] params) {
        log.debug("Connect scope: {}", newScope);
        boolean success = super.connect(newScope, params);
        if (success) {
            final Channel two = getChannel(2);
            // tell the flash player how fast we want data and how fast we shall send it
            two.write(new ServerBW(defaultServerBandwidth));
            // second param is the limit type (0=hard,1=soft,2=dynamic)
            two.write(new ClientBW(defaultClientBandwidth, (byte) limitType));
            // if the client is null for some reason, skip the jmx registration
            if (client != null) {
                // perform bandwidth detection
                if (bandwidthDetection && !client.isBandwidthChecked()) {
                    client.checkBandwidth();
                }
            } else {
                log.warn("Client was null");
            }
            registerJMX();
        } else {
            log.debug("Connect failed");
        }
        return success;
    }

    
    @Override
    public void close() {
        if (closing.compareAndSet(false, true)) {
            super.close();
            log.debug("IO Session closing: {}", (ioSession != null ? ioSession.isClosing() : null));
            if (ioSession != null && !ioSession.isClosing()) {
                // set a ref to ourself so that the handler can be notified when close future is done
                final RTMPMinaConnection self = this;
                // close now, no flushing, no waiting
                final CloseFuture future = ioSession.closeNow();
                log.debug("Connection close future: {}", future);
                IoFutureListener<CloseFuture> listener = new IoFutureListener<CloseFuture>() {
                    public void operationComplete(CloseFuture future) {
                        if (future.isClosed()) {
                            log.info("Connection is closed: {}", getSessionId());
                            if (log.isTraceEnabled()) {
                                log.trace("Session id - local: {} session: {}", getSessionId(), (String) ioSession.removeAttribute(RTMPConnection.RTMP_SESSION_ID));
                            }
                            handler.connectionClosed(self);
                        } else {
                            log.debug("Connection is not yet closed");
                        }
                        future.removeListener(this);
                    }
                };
                future.addListener(listener);
            }
            log.debug("Connection state: {}", getState());
            if (getStateCode() != RTMP.STATE_DISCONNECTED) {
                handler.connectionClosed(this);
            }
            // de-register with JMX
            unregisterJMX();
        } else if (log.isDebugEnabled()) {
            log.debug("Close has already been called");
        }
    }
 
    @Override
    public IoSession getIoSession() {
        return ioSession;
    }
 
    public int getDefaultServerBandwidth() {
        return defaultServerBandwidth;
    }
 
    public void setDefaultServerBandwidth(int defaultServerBandwidth) {
        this.defaultServerBandwidth = defaultServerBandwidth;
    }
 
    public int getDefaultClientBandwidth() {
        return defaultClientBandwidth;
    }
 
    public void setDefaultClientBandwidth(int defaultClientBandwidth) {
        this.defaultClientBandwidth = defaultClientBandwidth;
    }
 
    public int getLimitType() {
        return limitType;
    }
 
    public void setLimitType(int limitType) {
        this.limitType = limitType;
    }

    @Override
    public void setExecutor(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }
 
    public boolean isBandwidthDetection() {
        return bandwidthDetection;
    }
 
    public void setBandwidthDetection(boolean bandwidthDetection) {
        this.bandwidthDetection = bandwidthDetection;
    }
 
    @Override
    public boolean isReaderIdle() {
        if (ioSession != null) {
            return ioSession.isReaderIdle();
        }
        return true;
    }
 
    @Override
    public boolean isWriterIdle() {
        if (ioSession != null) {
            return ioSession.isWriterIdle();
        }
        return true;
    }
 
    @Override
    public long getPendingMessages() {
        if (ioSession != null) {
            return ioSession.getScheduledWriteMessages();
        }
        return 0;
    }
    
    @Override
    public long getReadBytes() {
        if (ioSession != null) {
            return ioSession.getReadBytes();
        }
        return 0;
    }
 
    @Override
    public long getWrittenBytes() {
        if (ioSession != null) {
            return ioSession.getWrittenBytes();
        }
        return 0;
    }

    public void invokeMethod(String method) {
        invoke(method);
    }
 
    @Override
    public boolean isConnected() {
        if (log.isTraceEnabled()) {
            log.trace("Connected: {}", (ioSession != null && ioSession.isConnected()));
        }
        // Paul: not sure isClosing is actually working as we expect here
        return super.isConnected() && (ioSession != null && ioSession.isConnected());
    }
 
    @Override
    public boolean isIdle() {
        if (ioSession != null) {
            if (log.isDebugEnabled()) {
                log.debug("Connection idle - read: {} write: {}", ioSession.isReaderIdle(), ioSession.isWriterIdle());
            }
            return super.isIdle() && ioSession.isBothIdle();
        }
        return super.isIdle();
    }
 
    @Override
    protected void onInactive() {
        close();
    }
 
    public void setIoSession(IoSession protocolSession) {
        SocketAddress remote = protocolSession.getRemoteAddress();
        if (remote instanceof InetSocketAddress) {
            remoteAddress = ((InetSocketAddress) remote).getAddress().getHostAddress();
            remotePort = ((InetSocketAddress) remote).getPort();
        } else {
            remoteAddress = remote.toString();
            remotePort = -1;
        }
        remoteAddresses = new ArrayList<String>(1);
        remoteAddresses.add(remoteAddress);
        remoteAddresses = Collections.unmodifiableList(remoteAddresses);
        this.ioSession = protocolSession;
        if (log.isTraceEnabled()) {
            log.trace("setIoSession conn: {}", this);
        }
    }
    
    @Override
    public void write(Packet out) {
        if (ioSession != null) {
            final Semaphore lock = getLock();
            if (log.isTraceEnabled()) {
                log.trace("Write lock wait count: {} closed: {}", lock.getQueueLength(), isClosed());
            }
            while (!isClosed()) {
                boolean acquired = false;
                try {
                    acquired = lock.tryAcquire(10, TimeUnit.MILLISECONDS);
                    if (acquired) {
                        if (log.isTraceEnabled()) {
                            log.trace("Writing message");
                        }
                        writingMessage(out);
                        ioSession.write(out);
                        break;
                    }
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for write lock. State: {}", RTMP.states[state.getState()], e);
                    if (log.isInfoEnabled()) {
                        // further debugging to assist with possible connection problems
                        log.info("Session id: {} in queue size: {} pending msgs: {} last ping/pong: {}", getSessionId(), currentQueueSize(), getPendingMessages(), getLastPingSentAndLastPongReceivedInterval());
                        log.info("Available permits - decoder: {} encoder: {}", decoderLock.availablePermits(), encoderLock.availablePermits());
                    }
                    String exMsg = e.getMessage();
                    // if the exception cause is null break out of here to prevent looping until closed
                    if (exMsg == null || exMsg.indexOf("null") >= 0) {
                        log.debug("Exception writing to connection: {}", this);
                        break;
                    }
                } finally {
                    if (acquired) {
                        lock.release();
                    }
                }
            }
        }
    }
    
    @Override
    public void writeRaw(IoBuffer out) {
        if (ioSession != null) {
            final Semaphore lock = getLock();
            while (!isClosed()) {
                boolean acquired = false;
                try {
                    acquired = lock.tryAcquire(10, TimeUnit.MILLISECONDS);
                    if (acquired) {
                        if (log.isTraceEnabled()) {
                            log.trace("Writing raw message");
                        }
                        ioSession.write(out);
                        break;
                    }
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for write lock (writeRaw). State: {}", RTMP.states[state.getState()], e);
                    String exMsg = e.getMessage();
                    // if the exception cause is null break out of here to prevent looping until closed
                    if (exMsg == null || exMsg.indexOf("null") >= 0) {
                        log.debug("Exception writing to connection: {}", this);
                        break;
                    }
                } finally {
                    if (acquired) {
                        lock.release();
                    }
                }
            }
        }
    }

    protected void registerJMX() {
        // register with jmx
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            String cName = this.getClass().getName();
            if (cName.indexOf('.') != -1) {
                cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
            }
            String hostStr = host;
            int port = 1935;
            if (host != null && host.indexOf(":") > -1) {
                String[] arr = host.split(":");
                hostStr = arr[0];
                port = Integer.parseInt(arr[1]);
            }
            // Create a new mbean for this instance
            oName = new ObjectName(String.format("org.red5.server:type=%s,connectionType=%s,host=%s,port=%d,clientId=%s", cName, type, hostStr, port, client.getId()));
            if (!mbs.isRegistered(oName)) {
                mbs.registerMBean(new StandardMBean(this, RTMPMinaConnectionMXBean.class, true), oName);
            } else {
                log.debug("Connection is already registered in JMX");
            }
        } catch (Exception e) {
            log.warn("Error on jmx registration", e);
        }
    }

    protected void unregisterJMX() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        if (oName != null && mbs.isRegistered(oName)) {
            try {
                mbs.unregisterMBean(oName);
            } catch (Exception e) {
                log.warn("Exception unregistering: {}", oName, e);
            }
            oName = null;
        }
    }

}
