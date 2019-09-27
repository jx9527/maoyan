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

package org.red5.server.api.stream;

import java.util.Map;

import org.red5.server.api.IConnection;

/**
 * A connection that supports streaming.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IStreamCapableConnection extends IConnection {

    /**
     * Return a reserved stream id for use. According to FCS/FMS regulation, the base is 1.
     */
    Number reserveStreamId() throws IndexOutOfBoundsException;

    /**
     * Return a reserved stream id for use with a preference for the one supplied.
     */
    Number reserveStreamId(Number streamId) throws IndexOutOfBoundsException;
 
    void unreserveStreamId(Number streamId);
 
    void deleteStreamById(Number streamId);
 
    IClientStream getStreamById(Number streamId);

    /**
     * Create a stream that can play only one item. 
     */
    ISingleItemSubscriberStream newSingleItemSubscriberStream(Number streamId);

    /**
     * Create a stream that can play a list. 
     */
    IPlaylistSubscriberStream newPlaylistSubscriberStream(Number streamId);

    /**
     * Create a broadcast stream.
     */
    IClientBroadcastStream newBroadcastStream(Number streamId);

    /**
     * Total number of video messages that are pending to be sent to a stream.
    */
    long getPendingVideoMessages(Number streamId);

    Map<Number, IClientStream> getStreamsMap();

}
