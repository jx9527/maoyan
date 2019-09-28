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

import org.red5.server.api.IConnection;
import org.red5.server.net.rtmp.status.Status;

/**
 * Connection that has options to invoke and handle remote calls
 *  	带参连接去反射和处理远程回调
 * 
 */
// this should really extend IServiceInvoker
public interface IServiceCapableConnection extends IConnection {
     
    void invoke(IServiceCall call);
 
    void invoke(IServiceCall call, int channel);
 
    void invoke(String method);
 
    void invoke(String method, IPendingServiceCallback callback);
 
    void invoke(String method, Object[] params);
 
    void invoke(String method, Object[] params, IPendingServiceCallback callback);
 
    void notify(IServiceCall call);
 
    void notify(IServiceCall call, int channel);
 
    void notify(String method);
 
    void notify(String method, Object[] params);
 
    void status(Status status);
 
    void status(Status status, int channel);

}
