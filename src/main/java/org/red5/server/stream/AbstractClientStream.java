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

import java.lang.ref.WeakReference;

import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamCapableConnection;

/**
 * Abstract base for client streams
 */
public abstract class AbstractClientStream extends AbstractStream implements IClientStream {
 
    private Number streamId = 0.0d;
 
    private String broadcastStreamPublishName;
 
    private WeakReference<IStreamCapableConnection> conn;
 
    private int clientBufferDuration; 
 
    public void setConnection(IStreamCapableConnection conn) {
        this.conn = new WeakReference<IStreamCapableConnection>(conn);
    }
 
    public IStreamCapableConnection getConnection() {
        return conn.get();
    }
 
    public void setClientBufferDuration(int duration) {
        clientBufferDuration = duration;
    }
 
    public int getClientBufferDuration() {
        return clientBufferDuration;
    }
 
    public void setBroadcastStreamPublishName(String broadcastStreamPublishName) {
        this.broadcastStreamPublishName = broadcastStreamPublishName;
    }
 
    public String getBroadcastStreamPublishName() {
        return broadcastStreamPublishName;
    }
    public void setStreamId(Number streamId) {
        this.streamId = streamId;
    }
 
    public Number getStreamId() {
        return streamId;
    }
}
