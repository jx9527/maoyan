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

/**
 * Base marker interface for all scope services. Used by the ScopeUtils to lookup services defined as beans in Spring application context. A scope service usually can perform various tasks on a scope like managing shared objects, streams, etc.
 * 所有范围服务的基础标记接口。scopeutils用于查找在Spring应用程序上下文中定义为bean的服务。
 * 作用域服务通常可以在作用域上执行各种任务，如管理共享对象、流等。
 * @author The Red5 Project
 * @author Joachim Bauch (bauch@struktur.de)
 */
public interface IScopeService {

    /**
     * Name of a bean defining that scope service. Override in subinterfaces.
     * */
    public static String BEAN_NAME = null;

}
