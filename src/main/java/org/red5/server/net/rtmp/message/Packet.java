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

package org.red5.server.net.rtmp.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.rtmp.event.IRTMPEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * RTMP packet. Consists of packet header, data and event context.
 */
@Slf4j
public class Packet implements Externalizable {

    private static final long serialVersionUID = -6415050845346626950L;

    private static final boolean noCopy = System.getProperty("packet.noCopy") == null ? false : Boolean.valueOf(System.getProperty("packet.noCopy"));
 
    private Header header;
 
    private IRTMPEvent message;
 
    private IoBuffer data;
 
    private transient long expirationTime = 0L;
 
    private transient final AtomicBoolean processed = new AtomicBoolean(false);

    public Packet() {
        log.trace("ctor");
    }
 
    public Packet(Header header) {
        if (log.isTraceEnabled()) {
            log.trace("Header: {}", header);
        }
        this.header = header;
        data = IoBuffer.allocate(header.getSize()).setAutoExpand(true);
    }
 
    public Packet(Header header, IRTMPEvent event) {
        if (log.isTraceEnabled()) {
            log.trace("Header: {} event: {}", header, event);
        }
        this.header = header;
        this.message = event;
    }
 
    public Header getHeader() {
        return header;
    }
 
    public void setMessage(IRTMPEvent message) {
        this.message = message;
    }
 
    public IRTMPEvent getMessage() {
        return message;
    }
 
    public void setData(IoBuffer buffer) {
        if (noCopy) {
            log.trace("Using buffer reference");
            this.data = buffer;
        } else {
            // try the backing array first if it exists
            if (buffer.hasArray()) {
                log.trace("Buffer has backing array, making a copy");
                byte[] copy = new byte[buffer.limit()];
                buffer.mark();
                buffer.get(copy);
                buffer.reset();
                data = IoBuffer.wrap(copy);
            } else {
                log.trace("Buffer has no backing array, using ByteBuffer");
                // fallback to ByteBuffer
                data.put(buffer.buf()).flip();
            }
        }
    }
 
    public IoBuffer getData() {
        return data;
    }
 
    public boolean hasData() {
        return data != null;
    }
 
    public void clearData() {
        if (data != null) {
            data.clear();
            data.free();
            data = null;
        }
    }
 
    public long getExpirationTime() {
        return expirationTime;
    }
 
    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }
 
    public boolean isExpired() {
        // if expirationTime is zero, the expiration is not used
        return expirationTime > 0L ? System.currentTimeMillis() > expirationTime : false;
    }
 
    public void setProcessed(boolean isProcessed) {
        this.processed.set(isProcessed);
    }
 
    public boolean isProcessed() {
        return this.processed.get();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        header = (Header) in.readObject();
        message = (IRTMPEvent) in.readObject();
        message.setHeader(header);
        message.setTimestamp(header.getTimer());
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(header);
        out.writeObject(message);
    }
 
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Packet [");
        if (header != null) {
            sb.append("[header data type=" + header.getDataType() + ", channel=" + header.getChannelId() + ", timer=" + header.getTimer() + "]");
        } else {
            sb.append("[header=null]");
        }
        if (message != null) {
            sb.append(", [message timestamp=" + message.getTimestamp() + "]");
        } else {
            sb.append(", [message=null]");
        }
        sb.append("]");
        return sb.toString();
    }
}
