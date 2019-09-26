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

import java.util.Set;

import org.red5.server.api.IConnection;
import org.red5.server.api.ICoreObject;
import org.red5.server.api.event.IEventObservable;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;

/**
 * Base interface for all scope objects, including SharedObjects.
 * 所有作用域对象（包括SharedObjects）的基本接口。
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IBasicScope extends ICoreObject, IEventObservable , Iterable<IBasicScope>, IPersistable{
 
    public boolean hasParent();
 
    public IScope getParent(); 
    /**
     * Get the scopes depth, how far down the scope tree is it. The lowest depth is 0x00, the depth of Global scope. Application scope depth is 0x01. Room depth is 0x02, 0x03 and so forth.
      */
    public int getDepth();
 
    public String getName();
 
    public IPersistenceStore getStore();
 
    public String getPath();
 
    public String getType(); 
    /**
     * Sets the amount of time to keep the scope available after the last disconnect.
     */
    public void setKeepDelay(int keepDelay);

    /**
     * Validates a scope based on its name and type 
     */
    public boolean isValid();

    /**
     * 允许作用域在实际连接之前对连接执行处理连接尝试或其他处理。
     */
    public boolean isConnectionAllowed(IConnection conn);

    /**
     * Provides a means to allow a scope to perform processing on another scope prior to additional scope handling.
     * 提供一种方法，允许一个作用域在附加作用域处理之前对另一个作用域执行处理
     * @param scope scope
     * @return true if scope is allowed and false if it is not allowed
     */
    public boolean isScopeAllowed(IScope scope);

    /**
     * Sets the scope security handlers.
     * 
     * @param securityHandlers scope security handlers
     */
    public void setSecurityHandlers(Set<IScopeSecurityHandler> securityHandlers);

}
