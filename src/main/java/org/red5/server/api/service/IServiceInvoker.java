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

package org.red5.server.api.service;

import org.red5.server.api.scope.IScope;

/**
 * 执行服务调用（来自客户端的远程调用）的对象的接口。
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IServiceInvoker {

    /**
     * 在给定范围内执行传递的服务调用。这将在作用域和作用域上下文中查找调用的处理程序.
      */
    boolean invoke(IServiceCall call, IScope scope);

    /**
     * 在给定对象中执行传递的服务调用。
     */
    boolean invoke(IServiceCall call, Object service);

}
