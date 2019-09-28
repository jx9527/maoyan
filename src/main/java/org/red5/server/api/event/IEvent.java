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

package org.red5.server.api.event;

/**
 * ievent接口是每个事件都应该实现的基本接口
 */
public interface IEvent {
 
    public Type getType();
 
    public Object getObject();
 
    public boolean hasSource();
 
    public IEventListener getSource();

    enum Type {
        SYSTEM, STATUS, SERVICE_CALL, SHARED_OBJECT, STREAM_ACTION, STREAM_CONTROL, STREAM_DATA, CLIENT, CLIENT_INVOKE, CLIENT_NOTIFY, SERVER
    }

}
