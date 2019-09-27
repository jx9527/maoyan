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

package org.red5.server.api;

import org.red5.server.exception.ClientNotFoundException;
import org.red5.server.exception.ClientRejectedException;

/**
 * 提供客户端对象的注册表。您可以使用lookup client方法通过客户机id/会话id查找客户机。
 * 这个接口实现还从给定的参数创建新的客户端对象，
 * 通常在初始连接时从客户端flex/flash应用程序传递。
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IClientRegistry { 
    /**
     * Check if a client with a given id exists. 
     */
    public boolean hasClient(String id);

    /**
     * Create a new client client object from connection params. 
     */
    public IClient newClient(Object[] params) throws ClientNotFoundException, ClientRejectedException;

    /**
     * Return an existing client from a client id. 
     */
    public IClient lookupClient(String id) throws ClientNotFoundException;

    /**
     * Adds a client to the registry. 
     */
    public void addClient(IClient client);

}