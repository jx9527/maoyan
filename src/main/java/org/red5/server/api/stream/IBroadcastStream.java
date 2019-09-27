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

package org.red5.server.api.stream;

import java.io.IOException;
import java.util.Collection;

import org.red5.server.messaging.IProvider;
import org.red5.server.net.rtmp.event.Notify;

/**
 * 广播流是要由客户端订阅的流源。要从客户端flash应用程序订阅流，请使用NetStream.play方法。广播流可以保存在服务器端。
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IBroadcastStream extends IStream {
 
    void saveAs(String filePath, boolean isAppend) throws IOException, ResourceNotFoundException, ResourceExistException;
 
    String getSaveFilename();
 
    IProvider getProvider();
 
    String getPublishedName();
 
    void setPublishedName(String name);
 
    public void addStreamListener(IStreamListener listener);
 
    public void removeStreamListener(IStreamListener listener);
 
    public Collection<IStreamListener> getStreamListeners();
 
    public Notify getMetaData();

}