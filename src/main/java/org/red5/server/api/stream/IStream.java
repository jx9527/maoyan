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

import org.red5.codec.IStreamCodecInfo;
import org.red5.server.api.scope.IScope;

/**
 * 流对象的基本接口。流对象始终与作用域关联.
 */
public interface IStream { 
    /**
     * 获取流的名称。该名称在服务器上是唯一的。这只是流的id，而不是用于客户端订阅流的名称。对于该名称，
     * 请使用{@link ibroadcaststream\getpublishedname（）} 
     */
    public String getName();
 
    IStreamCodecInfo getCodecInfo();
 
    public IScope getScope();
 
    public void start();
 
    public void stop();
 
    public void close();
 
    public long getCreationTime();
 
    long getStartTime();
}
