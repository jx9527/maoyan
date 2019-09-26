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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.service.IServiceHandlerProvider;
import org.red5.server.api.statistics.IScopeStatistics;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The scope object.
 * 
 * A stateful object shared between a group of clients connected to the same <tt>context path</tt>. Scopes are arranged in hierarchical way, 
 * so its possible for a scope to have a parent and children scopes. If a client connects to a scope then they are also connected to its parent scope.
 * The scope object is used to access resources, shared object, streams, etc. Scope is a general option for grouping things in an application.
 * 
 * The following are all names for scopes: application, room, place, lobby.
 * 连接到同一上下文路径的一组客户机之间共享的状态对象。范围按层次排列，
 * 所以作用域可以有父作用域和子作用域。如果客户机连接到一个作用域，那么它们也连接到其父作用域。
 * 作用域对象用于访问资源、共享对象、流等。作用域是对应用程序中的内容进行分组的常规选项。
 * 以下是作用域的所有名称：应用程序、房间、位置、大厅。
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface IScope extends IBasicScope, ResourcePatternResolver, IServiceHandlerProvider {

    /**
     * Scope separator
     */
    public static final String SEPARATOR = ":";
 
    public boolean hasChildScope(String name);
 
    public boolean hasChildScope(ScopeType type, String name);
 
    public boolean createChildScope(String name);
 
    public boolean addChildScope(IBasicScope scope);
 
    public void removeChildScope(IBasicScope scope);
 
    public void removeChildren();
 
    public Set<String> getScopeNames();

    public Set<String> getBasicScopeNames(ScopeType type);
 
    public IBroadcastScope getBroadcastScope(String name);
 
    public IBasicScope getBasicScope(String name);
 
    public IBasicScope getBasicScope(ScopeType type, String name);
 
    public IScope getScope(String name);
 
    public Set<IClient> getClients();
 
    @Deprecated
    public Collection<Set<IConnection>> getConnections();
 
    public Set<IConnection> getClientConnections();
 
    @Deprecated
    public Set<IConnection> lookupConnections(IClient client);
 
    public IConnection lookupConnection(IClient client);
 
    public IContext getContext();
 
    public boolean hasHandler();
 
    public IScopeHandler getHandler();
 
    public String getContextPath();
 
    public boolean connect(IConnection conn);
 
    public boolean connect(IConnection conn, Object[] params);
 
    public void disconnect(IConnection conn);
 
    public IScopeStatistics getStatistics();
 
    public boolean setAttribute(String name, Object value);
 
    public Object getAttribute(String name);
 
    public boolean hasAttribute(String name);
 
    public boolean removeAttribute(String name);
 
    public Set<String> getAttributeNames();
 
    public Map<String, Object> getAttributes();

}
