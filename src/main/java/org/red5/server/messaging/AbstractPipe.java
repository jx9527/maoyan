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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * 生产消费者模式
 * 抽象的管道 注册 生产者，消费者和监听者。目的是让子类更容易实现。
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 *
 * @see org.red5.server.messaging.IPipe
 */
public abstract class AbstractPipe implements IPipe {

    private static final Logger log = LoggerFactory.getLogger(AbstractPipe.class);
 
    protected volatile CopyOnWriteArrayList<IConsumer> consumers = new CopyOnWriteArrayList<>();
 
    protected volatile CopyOnWriteArrayList<IProvider> providers = new CopyOnWriteArrayList<>();
 
    protected volatile CopyOnWriteArrayList<IPipeConnectionListener> listeners = new CopyOnWriteArrayList<>();
 
    private static ExecutorService taskExecutor;

    /**
     * Connect consumer to this pipe. Doesn't allow to connect one consumer twice. Does register event listeners if instance of IPipeConnectionListener is given.
     *  连接消费者到管道上，不允许同一个消费者连接两次，如果是监听这就注册到监听上面。
     */
    public boolean subscribe(IConsumer consumer, Map<String, Object> paramMap) {
        // pipe is possibly used by dozens of threads at once (like many subscribers for one server stream)
        boolean success = consumers.addIfAbsent(consumer);
        // if consumer is listener object register it as listener and consumer has just been added
        if (success && consumer instanceof IPipeConnectionListener) {
            listeners.addIfAbsent((IPipeConnectionListener) consumer);
        }
        return success;
    } 
    
    public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
        boolean success = providers.addIfAbsent(provider);
        // register event listener if given and just added
        if (success && provider instanceof IPipeConnectionListener) {
            listeners.addIfAbsent((IPipeConnectionListener) provider);
        }
        return success;
    }

    /**
     * Disconnects provider from this pipe. Fires pipe connection event.
     * 取消生产者的连接，同时触发连接事件
     * @param provider
     *            Provider that should be removed
     * @return true on success, false otherwise
     */
    public boolean unsubscribe(IProvider provider) {
        if (providers.remove(provider)) {
            fireProviderConnectionEvent(provider, PipeConnectionEvent.EventType.PROVIDER_DISCONNECT, null);
            listeners.remove(provider);
            return true;
        }
        return false;
    }

    /**
     * Disconnects consumer from this pipe. Fires pipe connection event.
     * 同上
     * @param consumer
     *            Consumer that should be removed
     * @return true on success, false otherwise
     */
    public boolean unsubscribe(IConsumer consumer) {
        if (consumers.remove(consumer)) {
            fireConsumerConnectionEvent(consumer, PipeConnectionEvent.EventType.CONSUMER_DISCONNECT, null);
            listeners.remove(consumer);
            return true;
        }
        return false;
    }
 
    public void addPipeConnectionListener(IPipeConnectionListener listener) {
        listeners.add(listener);
    }
 
    public void removePipeConnectionListener(IPipeConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Send out-of-band ("special") control message to all consumers
     *
     * @param provider
     *            Provider, may be used in concrete implementations
     * @param oobCtrlMsg
     *            Out-of-band control message
     */
    public void sendOOBControlMessage(IProvider provider, OOBControlMessage oobCtrlMsg) {
        for (IConsumer consumer : consumers) {
            try {
                consumer.onOOBControlMessage(provider, this, oobCtrlMsg);
            } catch (Throwable t) {
                log.error("exception when passing OOBCM from provider to consumers", t);
            }
        }
    }

    /**
     * Send out-of-band ("special") control message to all providers
     *
     * @param consumer
     *            Consumer, may be used in concrete implementations
     * @param oobCtrlMsg
     *            Out-of-band control message
     */
    public void sendOOBControlMessage(IConsumer consumer, OOBControlMessage oobCtrlMsg) {
        for (IProvider provider : providers) {
            try {
                provider.onOOBControlMessage(consumer, this, oobCtrlMsg);
            } catch (Throwable t) {
                log.error("exception when passing OOBCM from consumer to providers", t);
            }
        }
    }
 
    public List<IPipeConnectionListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
 
    public void setListeners(List<IPipeConnectionListener> newListeners) {
        listeners.clear();
        listeners.addAll(newListeners);
    }
 
    public List<IProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }
 
    public List<IConsumer> getConsumers() {
        return Collections.unmodifiableList(consumers);
    }
 
    protected void fireConsumerConnectionEvent(IConsumer consumer, PipeConnectionEvent.EventType type, Map<String, Object> paramMap) {
        firePipeConnectionEvent(PipeConnectionEvent.build(this, type, consumer, paramMap));
    }
 
    protected void fireProviderConnectionEvent(IProvider provider, PipeConnectionEvent.EventType type, Map<String, Object> paramMap) {
        firePipeConnectionEvent(PipeConnectionEvent.build(this, type, provider, paramMap));
    }

    /**
     * Fire any pipe connection event and run all it's tasks
     * 触发连接事件，同时运行全部的任务。
     */
    protected void firePipeConnectionEvent(PipeConnectionEvent event) {
        for (IPipeConnectionListener element : listeners) {
            try {
                element.onPipeConnectionEvent(event);
            } catch (Throwable t) {
                log.error("Exception when handling pipe connection event", t);
            }
        }
        if (taskExecutor == null) {
            taskExecutor = Executors.newCachedThreadPool(new CustomizableThreadFactory("Pipe-"));
        }
        // Run all of event's tasks
        for (Runnable task : event.getTaskList()) {
            try {
                taskExecutor.execute(task);
            } catch (Throwable t) {
                log.warn("Exception executing pipe task {}", t);
            }
        }
        // Clear event's tasks list
        event.getTaskList().clear();
    }

    /**
     * Close the pipe
     */
    public void close() {
        // clean up collections
        if (consumers != null) {
            consumers.clear();
            consumers = null;
        }
        if (providers != null) {
            providers.clear();
            providers = null;
        }
        if (listeners != null) {
            listeners.clear();
            listeners = null;
        }
    }

}
