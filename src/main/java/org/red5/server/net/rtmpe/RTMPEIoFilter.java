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

package org.red5.server.net.rtmpe;

import javax.crypto.Cipher;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPMinaCodecFactory;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPE IO filter - Server version.
 * 当该类被添加到流程中后,session中的信息会一直存在，直到连接关闭。所以其中的连接状态等都会保存。
 * @author Peter Thomas (ptrthomas@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPEIoFilter extends IoFilterAdapter {

    private static final Logger log = LoggerFactory.getLogger(RTMPEIoFilter.class);

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object obj) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("messageReceived nextFilter: {} session: {} message: {}", nextFilter, session, obj);
        }
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (sessionId != null) {
            if (log.isTraceEnabled()) {
                log.trace("RTMP Session id: {}", sessionId);
            }
            RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            if (conn == null) {
                throw new Exception("Receive on unavailable connection - session id: " + sessionId);
            }
            // filter based on current connection state
            //根据当前的连接过滤状态
            RTMP rtmp = conn.getState();
            final byte connectionState = conn.getStateCode();
            // assume message is an IoBuffer
            //假定消息是一个ioBuffer
            IoBuffer message = (IoBuffer) obj;
            // client handshake handling
            InboundHandshake handshake = null;
            switch (connectionState) {
            	//首次连接需要为握手做准备
                case RTMP.STATE_CONNECT:
                    // get the handshake from the session and process C0+C1 if we have enough bytes
                	//从session中获取handshake,检测C0+C1是否有足够字节
                    handshake = (InboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                    // buffer the incoming message or part of message
                    // 缓存收入信息作为消息的一部分。
                    handshake.addBuffer(message);
                    // check the size, we want 1537 bytes for C0C1
                    // 检测C0C1消息的大小	
                    int c0c1Size = handshake.getBufferSize();
                    log.trace("Incoming C0C1 size: {}", c0c1Size);
                    if (c0c1Size >= (Constants.HANDSHAKE_SIZE + 1)) {
                        log.debug("decodeHandshakeC0C1");
                        // get the buffered bytes
                        // 获取字节数据
                        IoBuffer buf = handshake.getBufferAsIoBuffer();
                        // set handshake to match client requested type
                        // 设置握手请求类型  一个byte
                        byte connectionType = buf.get();
                        handshake.setHandshakeType(connectionType);
                        log.trace("Incoming C0 connection type: {}", connectionType);
                        // create array for decode
                        // 初始化要解码的数组
                        byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                        // copy out 1536 bytes
                        buf.get(dst);
                        //log.debug("C1 - buffer: {}", Hex.encodeHexString(dst));
                        // set state to indicate we're waiting for C2
                        //设置连接状态
                        rtmp.setState(RTMP.STATE_HANDSHAKE);
                        // buffer any extra bytes
                        //缓存多余的数据
                        int remaining = buf.remaining();
                        if (remaining > 0) {
                            // store the remaining bytes in a thread local for use by C2 decoding
                            //存储多余的可以被c2解码使用的字节在一个线程变量中，
                        	handshake.addBuffer(buf);
                            log.trace("Stored {} bytes for later decoding", remaining);
                        }
                        //对s1请求进行编码
                        IoBuffer s1 = handshake.decodeClientRequest1(IoBuffer.wrap(dst));
                        if (s1 != null) {
                            //log.trace("S1 byte order: {}", s1.order());
                            session.write(s1);
                        } else {
                            log.warn("Client was rejected due to invalid handshake");
                            conn.close();
                        }
                    }
                    break;
                //进行握手
                case RTMP.STATE_HANDSHAKE:
                    // get the handshake from the session and process C2 if we have enough bytes
                	//获取握手对象，同时审核C2数据是否足够
                    handshake = (InboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                    // buffer the incoming message
                    handshake.addBuffer(message);
                    // no connection type byte is supposed to be in C2 data
                    // 在C2中不支持连接状态
                    int c2Size = handshake.getBufferSize();
                    log.trace("Incoming C2 size: {}", c2Size);
                    if (c2Size >= Constants.HANDSHAKE_SIZE) {
                        log.debug("decodeHandshakeC2");
                        // get the buffered bytes
                        IoBuffer buf = handshake.getBufferAsIoBuffer();
                        // create array for decode
                        byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                        // get C2 out
                        buf.get(dst);
                        if (handshake.decodeClientRequest2(IoBuffer.wrap(dst))) {
                            log.debug("Connected, removing handshake data and adding rtmp protocol filter");
                            // set state to indicate we're connected
                            //修改连接状态为 已连接
                            rtmp.setState(RTMP.STATE_CONNECTED);
                            // set encryption flag the rtmp state
                            // 设置rtmp是否为加密状态
                            if (handshake.useEncryption()) {
                                log.debug("Using encrypted communications, adding ciphers to the session");
                                rtmp.setEncrypted(true);
                                session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherIn());
                                session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherOut());
                            } 
                            // remove handshake from session now that we are connected
                            //移除握手协议
                            session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                            // add protocol filter as the last one in the chain
                            log.debug("Adding RTMP protocol filter");
                            //在 rtmpeFilter后面添加protocolFilter
                            session.getFilterChain().addAfter("rtmpeFilter", "protocolFilter", new ProtocolCodecFilter(new RTMPMinaCodecFactory()));
                            // check for remaining stored bytes left over from C0C1 and prepend to the dst array
                            if (buf.hasRemaining()) {
                                log.trace("Receiving message: {}", buf);
                                nextFilter.messageReceived(session, buf);
                            }
                        } else {
                            log.warn("Client was rejected due to invalid handshake");
                            conn.close();
                        }
                    }
                    break;
                case RTMP.STATE_CONNECTED:
                    // assuming majority of connections will not be encrypted
                    if (!rtmp.isEncrypted()) {
                        log.trace("Receiving message: {}", message);
                        nextFilter.messageReceived(session, message);
                    } else {
                        Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN);
                        if (cipher != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Decrypting message: {}", message);
                            }
                            byte[] encrypted = new byte[message.remaining()];
                            message.get(encrypted);
                            message.clear();
                            message.free();
                            byte[] plain = cipher.update(encrypted);
                            IoBuffer messageDecrypted = IoBuffer.wrap(plain);
                            if (log.isDebugEnabled()) {
                                log.debug("Receiving decrypted message: {}", messageDecrypted);
                            }
                            nextFilter.messageReceived(session, messageDecrypted);
                        }
                    }
                    break;
                case RTMP.STATE_ERROR:
                case RTMP.STATE_DISCONNECTING:
                case RTMP.STATE_DISCONNECTED:
                    // do nothing, really
                    log.debug("Nothing to do, connection state: {}", RTMP.states[connectionState]);
                    break;
                default:
                    throw new IllegalStateException("Invalid RTMP state: " + connectionState);
            }
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception {
        log.trace("filterWrite nextFilter: {} session: {} request: {}", nextFilter, session, request);
        Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
        if (cipher == null) {
            if (log.isTraceEnabled()) {
                log.trace("Writing message");
            }
            nextFilter.filterWrite(session, request);
        } else {
            IoBuffer message = (IoBuffer) request.getMessage();
            if (!message.hasRemaining()) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignoring empty message");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Encrypting message: {}", message);
                }
                byte[] plain = new byte[message.remaining()];
                message.get(plain);
                message.clear();
                message.free();
                //encrypt and write
                byte[] encrypted = cipher.update(plain);
                IoBuffer messageEncrypted = IoBuffer.wrap(encrypted);
                if (log.isDebugEnabled()) {
                    log.debug("Writing encrypted message: {}", messageEncrypted);
                }
                nextFilter.filterWrite(session, new EncryptedWriteRequest(request, messageEncrypted));
            }
        }
    }

    private static class EncryptedWriteRequest extends WriteRequestWrapper {
        private final IoBuffer encryptedMessage;

        private EncryptedWriteRequest(WriteRequest writeRequest, IoBuffer encryptedMessage) {
            super(writeRequest);
            this.encryptedMessage = encryptedMessage;
        }

        @Override
        public Object getMessage() {
            return encryptedMessage;
        }
    }

}
