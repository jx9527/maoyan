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

package org.red5.server.api;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IScope;

/**
 * The connection object.
 * 
 ** 每个连接都有一个关联的客户端和作用域。
 ** 连接可以是持久的、轮询的或暂时的。此接口的目的是提
 ** 供不同类型连接之间共享的基本连接方法。
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface IConnection extends ICoreObject, ICastingAttributeStore {

    /**
     * Encoding type.
     */
    public static enum Encoding {
        AMF0, AMF3, WEBSOCKET, SOCKETIO, RTP, SRTP, RAW
    };

    /**
     * Duty type.
     */
    public static enum Duty {
        UNDEFINED, PUBLISHER, SUBSCRIBER, PROXY, REMOTING
    };

    /**
     * Connection type.
     */
    public static enum Type {
        PERSISTENT, // Persistent connection type, eg RTMP
        POLLING, // Polling connection type, eg RTMPT
        TRANSIENT, // Transient connection type, eg Remoting, HTTP, etc
        UNKNOWN // all others not matching known types
    };

    /**
     * Get the connection type. 
     */
    @Deprecated
    public String getType(); // PERSISTENT | POLLING | TRANSIENT

    /**
     * Get the connection type. 
     */
    //public Type getType(); // PERSISTENT | POLLING | TRANSIENT

    /**
     * Get the object encoding in use for this connection. 
     */
    public Encoding getEncoding();

    /**
     * Get the duty for this connection; this is not meant nor expected to remain static. 
     */
    public Duty getDuty();

    /**
     * Initialize the connection. 
     */
    public void initialize(IClient client);

    /**
     * Try to connect to the scope. 
     */
    public boolean connect(IScope scope);

    /**
     * Try to connect to the scope with a list of connection parameters. 
     */
    public boolean connect(IScope scope, Object[] params);

    /**
     * Is the client connected to the scope. Result depends on connection type, true for persistent and polling connections,
     * false for transient.
     */
    public boolean isConnected();
 
    public void close();
 
    public Map<String, Object> getConnectParams();
 
    public void setClient(IClient client);
 
    public IClient getClient();
 
    public String getHost();
 
    public String getRemoteAddress();
 
    public List<String> getRemoteAddresses();
 
    public int getRemotePort();
 
    public String getPath();
 
    public String getSessionId();
 
    public long getReadBytes();
 
    public long getWrittenBytes();
 
    public long getReadMessages();
 
    public long getWrittenMessages();
 
    public long getDroppedMessages();
 
    public long getPendingMessages();
 
    public long getClientBytesRead();
 
    public void ping();
 
    public int getLastPingTime();
 
    public IScope getScope();
 
    public Iterator<IBasicScope> getBasicScopes();
 
    public void setBandwidth(int mbits);
 
    public void addListener(IConnectionListener listener);
 
    public void removeListener(IConnectionListener listener);
 
    public Number getStreamId();
 
    public void setStreamId(Number id);
 
    public String getProtocol();

}
