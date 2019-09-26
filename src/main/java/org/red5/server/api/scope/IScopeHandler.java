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

package org.red5.server.api.scope;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.event.IEventHandler;
import org.red5.server.api.service.IServiceCall;

/**
 * The scope handler controls actions performed against a scope object, and also is notified of all events.
 * 
 * Gives fine grained control over what actions can be performed with the can* methods. Allows for detailed reporting on what is happening within the scope with the on* methods. This is the core interface users implement to create applications.
 * 
 * The thread local connection is always available via the Red5 object within these methods
 * 作用域处理程序控制对作用域对象执行的操作，并通知所有事件。
 * 对可以使用can*方法执行的操作进行细粒度控制。允许使用on*方法详细报告范围内发生的情况。
 * 这是用户用来创建应用程序的核心接口。
 * 线程本地连接始终通过这些方法中的red5对象可用。
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IScopeHandler extends IEventHandler {
 
    boolean start(IScope scope);
 
    void stop(IScope scope);
 
    boolean connect(IConnection conn, IScope scope, Object[] params);
 
    void disconnect(IConnection conn, IScope scope);
 
    boolean addChildScope(IBasicScope scope);
 
    void removeChildScope(IBasicScope scope);
 
    boolean join(IClient client, IScope scope);
 
    void leave(IClient client, IScope scope);
 
    boolean serviceCall(IConnection conn, IServiceCall call); 
}
