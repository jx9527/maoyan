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

package org.red5.server.messaging;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Input Endpoint for a consumer to connect.
 * 消费者输入连接点
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IMessageInput {
    /**
     * Pull message from this input endpoint. Return w/o waiting.
     * 从输入点拉去消息
     * @return The pulled message or <tt>null</tt> if message is not available.
     * @throws IOException
     *             on error
     */
    IMessage pullMessage() throws IOException;

    /**
     * Pull message from this input endpoint. Wait <tt>wait</tt> milliseconds if message is not available.
     * 从输入点推消息
     * @param wait
     *            milliseconds to wait when message is not available.
     * @return The pulled message or <tt>null</tt> if message is not available.
     */
    IMessage pullMessage(long wait);

    /**
     * Connect to a consumer.
     * 连接到一个消费者
     * @param consumer
     *            Consumer
     * @param paramMap
     *            Parameters map
     * @return <tt>true</tt> when successfully subscribed, <tt>false</tt> otherwise.
     */
    boolean subscribe(IConsumer consumer, Map<String, Object> paramMap);

    /**
     * 与一个消费者断开连接
     * Disconnect from a consumer.
     * 
     * @param consumer
     *            Consumer to disconnect
     * @return <tt>true</tt> when successfully unsubscribed, <tt>false</tt> otherwise.
     */
    boolean unsubscribe(IConsumer consumer);

    /**
     * Getter for consumers list.
     * 获取全部的消费者
     * @return Consumers.
     */
    List<IConsumer> getConsumers();

    /**
     * 发送带外数据(Out of Band, OOB) 控制消息给在通道另一头全部的提供者这
     * Send OOB Control Message to all providers on the other side of pipe.
     * 
     * @param consumer
     *            The consumer that sends the message
     * @param oobCtrlMsg
     *            Out-of-band control message
     */
    void sendOOBControlMessage(IConsumer consumer, OOBControlMessage oobCtrlMsg);
}
