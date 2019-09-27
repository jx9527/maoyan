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

package org.red5.server.scope;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.messaging.IConsumer;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.stream.IProviderService;

/**
 * Scope type for publishing that deals with pipe connection events, like async message listening in JMS
 */
public class BroadcastScope extends BasicScope implements IBroadcastScope, IPipeConnectionListener {

    /**
     * Broadcasting stream associated with this scope
     */
    private transient IClientBroadcastStream clientBroadcastStream;

    /**
     * Simple in memory push pipe, triggered by an active provider to push messages to consumer
     */
    private final transient InMemoryPushPushPipe pipe;

    /**
     * Number of components.
     */
    private AtomicInteger compCounter = new AtomicInteger(0);

    /**
     * Whether or not this "scope" has been removed
     */
    private volatile boolean removed;
 
    public BroadcastScope(IScope parent, String name) {
        super(parent, ScopeType.BROADCAST, name, false);
        pipe = new InMemoryPushPushPipe(this);
        keepOnDisconnect = true;
    }
 
    public void addPipeConnectionListener(IPipeConnectionListener listener) {
        pipe.addPipeConnectionListener(listener);
    }
 
    public void removePipeConnectionListener(IPipeConnectionListener listener) {
        pipe.removePipeConnectionListener(listener);
    }
 
    public IMessage pullMessage() {
        return pipe.pullMessage();
    }
 
    public IMessage pullMessage(long wait) {
        return pipe.pullMessage(wait);
    }
 
    public boolean subscribe(IConsumer consumer, Map<String, Object> paramMap) {
        return !removed && pipe.subscribe(consumer, paramMap);
    }
 
    public boolean unsubscribe(IConsumer consumer) {
        return pipe.unsubscribe(consumer);
    }
 
    public List<IConsumer> getConsumers() {
        return pipe.getConsumers();
    }
 
    public void sendOOBControlMessage(IConsumer consumer, OOBControlMessage oobCtrlMsg) {
        pipe.sendOOBControlMessage(consumer, oobCtrlMsg);
    }
 
    public void pushMessage(IMessage message) throws IOException {
        pipe.pushMessage(message);
    }
 
    public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
        return !removed && pipe.subscribe(provider, paramMap);
    }
 
    public boolean unsubscribe(IProvider provider) {
        return pipe.unsubscribe(provider);
    }
 
    public List<IProvider> getProviders() {
        return pipe.getProviders();
    }
 
    public void sendOOBControlMessage(IProvider provider, OOBControlMessage oobCtrlMsg) {
        pipe.sendOOBControlMessage(provider, oobCtrlMsg);
    }
 
    public void onPipeConnectionEvent(PipeConnectionEvent event) {
        // Switch event type
        switch (event.getType()) {
            case CONSUMER_CONNECT_PULL:
            case CONSUMER_CONNECT_PUSH:
            case PROVIDER_CONNECT_PULL:
            case PROVIDER_CONNECT_PUSH:
                compCounter.incrementAndGet();
                break;
            case CONSUMER_DISCONNECT:
            case PROVIDER_DISCONNECT:
                if (compCounter.decrementAndGet() <= 0) {
                    // should we synchronize parent before removing?
                    if (hasParent()) {
                        IScope parent = getParent();
                        IProviderService providerService = (IProviderService) parent.getContext().getBean(IProviderService.BEAN_NAME);
                        removed = providerService.unregisterBroadcastStream(parent, getName());
                    } else {
                        removed = true;
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Event type not supported: " + event.getType());
        }
    }
 
    public IClientBroadcastStream getClientBroadcastStream() {
        return clientBroadcastStream;
    }
 
    public void setClientBroadcastStream(IClientBroadcastStream clientBroadcastStream) {
        if (this.clientBroadcastStream != null) {
            log.info("ClientBroadcastStream already exists: {} new: {}", this.clientBroadcastStream, clientBroadcastStream);
        }
        this.clientBroadcastStream = clientBroadcastStream;
    }
 
    @Override
    public String toString() {
        return "BroadcastScope [clientBroadcastStream=" + clientBroadcastStream + ", compCounter=" + compCounter + ", removed=" + removed + "]";
    } 

}
