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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketException;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.red5.server.api.Red5;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPMinaCodecFactory;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmpe.RTMPEIoFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Handles all RTMP protocol events fired by the MINA framework.
 * 处理所有被MINA框架触发的RTMP协议事件
 */
@Slf4j
public class RTMPMinaIoHandler extends IoHandlerAdapter { 
    /**
     * RTMP events handler
     ** 在配置文件中设置 RTMPHandler
     */
    protected IRTMPHandler handler; 
    /**
	 * Mode
	 */
	protected boolean mode = RTMP.MODE_SERVER;
	 
	protected ProtocolCodecFactory codecFactory;

	protected IRTMPConnManager rtmpConnManager;
	
	public RTMPMinaIoHandler() { 
		handler = new RTMPHandler();
		codecFactory = new RTMPMinaCodecFactory();
		rtmpConnManager = new RTMPConnManager();
	}
     
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        log.debug("Session created RTMP");
        //检查rtmpe过滤器，如果没有，则添加
        if (!session.getFilterChain().contains("rtmpeFilter")) {
            // 添加rtmpe筛选器，成功握手时添加rtmp协议筛选器
            session.getFilterChain().addFirst("rtmpeFilter", new RTMPEIoFilter());
        } 
        RTMPMinaConnection conn = null;
        String sessionId = null; 
        //检查确保连接实例是否存在,首次连接不存在则进行创建
        if (!session.containsAttribute(RTMPConnection.RTMP_SESSION_ID)) {
            // create a connection
            conn = createRTMPMinaConnection();
            // add session to the connection
            conn.setIoSession(session);
            // add the handler
            conn.setHandler(handler);
            // get the session id
            sessionId = conn.getSessionId();
            // add the connections session id for look up using the connection manager
            session.setAttribute(RTMPConnection.RTMP_SESSION_ID, sessionId);
            // create an inbound handshake
            InboundHandshake handshake = new InboundHandshake();
            // set whether or not unverified will be allowed
            handshake.setUnvalidatedConnectionAllowed(((RTMPHandler) handler).isUnvalidatedConnectionAllowed());
            // add the in-bound handshake, defaults to non-encrypted mode
            session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, handshake);
            log.debug("Created: {}", sessionId);
        } else {
            sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
            log.warn("Session previously created: {} id: {}", session.getId(), sessionId);
            RTMPConnManager connManager = (RTMPConnManager) RTMPConnManager.getInstance();
            if ((RTMPMinaConnection) connManager.getConnectionBySessionId(sessionId) == null) {
                log.warn("Connection lookup failed for {}", sessionId);
            }
        }
    }
 
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.debug("Session opened: {} id: {}", session.getId(), sessionId);
        RTMPConnManager connManager = (RTMPConnManager) RTMPConnManager.getInstance();
        session.setAttribute(RTMPConnection.RTMP_CONN_MANAGER, new WeakReference<IConnectionManager<RTMPConnection>>(connManager));
        RTMPMinaConnection conn = (RTMPMinaConnection) connManager.getConnectionBySessionId(sessionId);
        handler.connectionOpened(conn);
    }
 
    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Idle (session: {}) local: {} remote: {}\nread: {} write: {}", session.getId(), session.getLocalAddress(), session.getRemoteAddress(), session.getReadBytes(), session.getWrittenBytes());
        }
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (sessionId != null) {
            RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            if (conn != null) {
                // close the idle socket
                conn.onInactive();
            }
        }
    }
 
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.debug("Session closed: {} id: {}", session.getId(), sessionId);
        if (log.isTraceEnabled()) {
            log.trace("Session attributes: {}", session.getAttributeKeys());
        }
        if (sessionId != null) {
            RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            if (conn != null) {
                // fire-off closed event
                handler.connectionClosed(conn);
                // clear any session attributes we may have previously set
                // TODO: verify this cleanup code is necessary. The session is over and will be garbage collected surely?
                if (session.containsAttribute(RTMPConnection.RTMP_HANDSHAKE)) {
                    session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                }
                if (session.containsAttribute(RTMPConnection.RTMPE_CIPHER_IN)) {
                    session.removeAttribute(RTMPConnection.RTMPE_CIPHER_IN);
                    session.removeAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
                }
            } else {
                log.warn("Connection was not found for {}", sessionId);
            }
            cleanSession(session, false);
        } else {
            log.debug("Connections session id was null in session, may already be closed");
        }
    }
 
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("messageReceived session: {} message: {}", session, message);
            log.trace("Filter chain: {}", session.getFilterChain());
        }
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (log.isTraceEnabled()) {
            log.trace("Message received on session: {} id: {}", session.getId(), sessionId);
        }
        RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
        if (conn != null) {
            if (message != null) {
                if (message instanceof Packet) {
                    byte state = conn.getStateCode();
                    // checking the state before allowing a task to be created will hopefully prevent rejected task exceptions
                    if (state != RTMP.STATE_DISCONNECTING && state != RTMP.STATE_DISCONNECTED) {
                        Red5.setConnectionLocal(conn);
                        conn.handleMessageReceived((Packet) message);
                        Red5.setConnectionLocal(null);
                    } else {
                        log.info("Ignoring received message on {} due to state: {}", sessionId, RTMP.states[state]);
                    }
                }
            }
        } else {
            log.warn("Connection was not found for {}, force closing", sessionId);
            throw new SocketException("Connection lost");
        }
    }

    
    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        log.trace("messageSent session: {} message: {}", session, message);
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (log.isTraceEnabled()) {
            log.trace("Message sent on session: {} id: {}", session.getId(), sessionId);
        }
        if (sessionId != null) {
            RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            if (conn != null) {
                final byte state = conn.getStateCode();
                switch (state) {
                    case RTMP.STATE_CONNECTED:
                        if (message instanceof Packet) {
                            handler.messageSent(conn, (Packet) message);
                        } else if (log.isDebugEnabled()) {
                            log.debug("Message was not of Packet type; its type: {}", message != null ? message.getClass().getName() : "null");
                        }
                        break;
                    case RTMP.STATE_CONNECT:
                    case RTMP.STATE_HANDSHAKE:
                        if (log.isTraceEnabled()) {
                            log.trace("messageSent: {}", Hex.encodeHexString(((IoBuffer) message).array()));
                        }
                        break;
                    case RTMP.STATE_DISCONNECTING:
                    case RTMP.STATE_DISCONNECTED:
                    default:
                }
            } else {
                log.warn("Destination connection was null, it is already disposed. Session id: {}", sessionId);
            }
        }
    }
 
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        log.debug("Filter chain: {}", session.getFilterChain());
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (log.isDebugEnabled()) {
            log.warn("Exception caught on session: {} id: {}", session.getId(), sessionId, cause);
        }
        if (cause instanceof IOException) {
            // Mina states that the connection will be automatically closed when an IOException is caught
            log.debug("IOException caught on {}", sessionId);
        } else {
            log.debug("Non-IOException caught on {}", sessionId);
            if (session.containsAttribute("FORCED_CLOSE")) {
                log.info("Close already forced on this session: {}", session.getId());
            } else {
                // set flag
                session.setAttribute("FORCED_CLOSE", Boolean.TRUE);
                //session.suspendRead();
                cleanSession(session, true);
            }
        }
    }
 
    private void cleanSession(final IoSession session, boolean immediately) {
        if (session.isClosing()) {
            log.debug("Session already being closed");
        } else {
            // clean up
            final String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
            if (log.isDebugEnabled()) {
                log.debug("Forcing close on session: {} id: {}", session.getId(), sessionId);
                log.debug("Session closing: {}", session.isClosing());
            }
            // get the write request queue
            final WriteRequestQueue writeQueue = session.getWriteRequestQueue();
            if (writeQueue != null && !writeQueue.isEmpty(session)) {
                log.debug("Clearing write queue");
                try {
                    writeQueue.clear(session);
                } catch (Exception ex) {
                    // clear seems to cause a write to closed session ex in some cases
                    log.warn("Exception clearing write queue for {}", sessionId, ex);
                }
            }
            // force close the session
            final CloseFuture future = immediately ? session.closeNow() : session.closeOnFlush();
            IoFutureListener<CloseFuture> listener = new IoFutureListener<CloseFuture>() {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public void operationComplete(CloseFuture future) {
                    // now connection should be closed
                    log.debug("Close operation completed {}: {}", sessionId, future.isClosed());
                    future.removeListener(this);
                    for (Object key : session.getAttributeKeys()) {
                        Object obj = session.getAttribute(key);
                        log.debug("{}: {}", key, obj);
                        if (obj != null) {
                            if (log.isTraceEnabled()) {
                                log.trace("Attribute: {}", obj.getClass().getName());
                            }
                            if (obj instanceof IoProcessor) {
                                log.debug("Flushing session in processor");
                                ((IoProcessor) obj).flush(session);
                                log.debug("Removing session from processor");
                                ((IoProcessor) obj).remove(session);
                            } else if (obj instanceof IoBuffer) {
                                log.debug("Clearing session buffer");
                                ((IoBuffer) obj).clear();
                                ((IoBuffer) obj).free();
                            }
                        }
                    }
                }
            };
            future.addListener(listener);
        }
    }
 
    public void setHandler(IRTMPHandler handler) {
        this.handler = handler;
    }
 
    @Deprecated
    public void setCodecFactory(ProtocolCodecFactory codecFactory) {
    	this.codecFactory = codecFactory;
        log.warn("This option is deprecated, the codec factory is now contained within the RTMPEIoFilter");
    }

    protected RTMPMinaConnection createRTMPMinaConnection() {
        return (RTMPMinaConnection) RTMPConnManager.getInstance().createConnection(RTMPMinaConnection.class);
    } 
    
	public void setMode(boolean mode) {
		this.mode = mode;
	} 
	
	public void setRtmpConnManager(IRTMPConnManager rtmpConnManager) {
		this.rtmpConnManager = rtmpConnManager;
	}

	protected IRTMPConnManager getRtmpConnManager() {
		return rtmpConnManager;
	}
    
}
