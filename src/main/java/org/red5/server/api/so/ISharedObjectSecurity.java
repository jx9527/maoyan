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

package org.red5.server.api.so;

import java.util.List;

import org.red5.server.api.scope.IScope;

/**
 * Interface for handlers that control access to shared objects.
 * 用于控制对共享对象访问的处理程序的接口。
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface ISharedObjectSecurity {

    /**
     * Check if the a shared object may be created in the given scope.
     * 检查是否可以在给定范围内创建共享对象。
     * @param scope
     *            scope
     * @param name
     *            name
     * @param persistent
     *            is persistent
     * @return is creation allowed
     */
    public boolean isCreationAllowed(IScope scope, String name, boolean persistent);

    /**
     * Check if a connection to the given existing shared object is allowed.
     * 检查是否允许连接到给定的现有共享对象。
     * @param so
     *            shared ojbect
     * @return is connection alowed
     */
    public boolean isConnectionAllowed(ISharedObject so);

    /**
     * Check if a modification is allowed on the given shared object.
     * 检查是否允许对给定的共享对象进行修改。
     * @param so
     *            shared object
     * @param key
     *            key
     * @param value
     *            value
     * @return true if given key is modifiable; false otherwise
     */
    public boolean isWriteAllowed(ISharedObject so, String key, Object value);

    /**
     * Check if the deletion of a property is allowed on the given shared object.
     * 检查是否允许删除给定共享对象上的属性。
     * @param so
     *            shared object
     * @param key
     *            key
     * @return true if delete allowed; false otherwise
     */
    public boolean isDeleteAllowed(ISharedObject so, String key);

    /**
     * Check if sending a message to the shared object is allowed.
     * 检查是否允许向共享对象发送消息
     * @param so
     *            shared object
     * @param message
     *            message
     * @param arguments
     *            arguments
     * @return true if allowed
     */
    public boolean isSendAllowed(ISharedObject so, String message, List<?> arguments);

}
