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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.red5.codec.IStreamCodecInfo;
import org.red5.codec.StreamCodecInfo;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeHandler;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.StreamState;
import org.red5.server.net.rtmp.event.Notify;

/**
 * Abstract base implementation of IStream. Contains codec information, stream name, scope, event handling, and provides stream start and
 * stop operations.
 *
 * @see org.red5.server.api.stream.IStream
 */
public abstract class AbstractStream implements IStream {
 
    private String name;
 
    private IStreamCodecInfo codecInfo = new StreamCodecInfo();
 
    private transient AtomicReference<Notify> metaData = new AtomicReference<>();
 
    private IScope scope;
 
    private transient CopyOnWriteArrayList<PropertyChangeListener> stateListeners = new CopyOnWriteArrayList<>();
 
    protected long creationTime = System.currentTimeMillis();
 
    protected long startTime;
 
    protected final transient AtomicReference<StreamState> state = new AtomicReference<>(StreamState.UNINIT);
 
    protected void fireStateChange(StreamState oldState, StreamState newState) {
        final PropertyChangeEvent evt = new PropertyChangeEvent(this, "StreamState", oldState, newState);
        for (PropertyChangeListener listener : stateListeners) {
            listener.propertyChange(evt);
        }
    }
 
    public void addStateChangeListener(PropertyChangeListener listener) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }
 
    public void removeStateChangeListener(PropertyChangeListener listener) {
        stateListeners.remove(listener);
    }
 
    public String getName() {
        return name;
    }
 
    public IStreamCodecInfo getCodecInfo() {
        return codecInfo;
    }
 
    public Notify getMetaData() {
        Notify md = metaData.get();
        if (md != null) {
            try {
                return md.duplicate();
            } catch (Exception e) {
            }
        }
        return md;
    }
 
    public void setMetaData(Notify metaData) {
        this.metaData.set(metaData);
    }
 
    public IScope getScope() {
        return scope;
    }
 
    public long getCreationTime() {
        return creationTime;
    }
 
    public long getStartTime() {
        return startTime;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public void setCodecInfo(IStreamCodecInfo codecInfo) {
        this.codecInfo = codecInfo;
    }
 
    public void setScope(IScope scope) {
        this.scope = scope;
    }
 
    public StreamState getState() {
        return state.get();
    }
 
    public void setState(StreamState newState) {
        StreamState oldState = state.get();
        if (!oldState.equals(newState) && state.compareAndSet(oldState, newState)) {
            fireStateChange(oldState, newState);
        }
    }
 
    protected IStreamAwareScopeHandler getStreamAwareHandler() {
        if (scope != null) {
            IScopeHandler handler = scope.getHandler();
            if (handler instanceof IStreamAwareScopeHandler) {
                return (IStreamAwareScopeHandler) handler;
            }
        }
        return null;
    }
}
