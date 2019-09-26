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

package org.red5.server.api.persistence;

import java.io.IOException;

import org.red5.io.object.Input;
import org.red5.io.object.Output;

/**
 * 可设为永久的对象的基本接口。
 * 符合此接口的每个对象都必须提供一个只接受输入流作为参数的构造函数或一个空构造函数，以便可以从持久性存储加载它。
 * 但是，对于由应用程序创建并随后初始化的对象，这不是必需的。
 * @see org.red5.io.object.Input
 * @see IPersistenceStore#load(String)
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 */

public interface IPersistable {

    /**
     * Prefix for attribute names that should not be made persistent.
     */
    public static final String TRANSIENT_PREFIX = "_transient";
 
    public boolean isPersistent();
 
    public void setPersistent(boolean persistent);
 
    public String getName();
 
    public void setName(String name);

    /**
     * Returns the type of the persistent object. 
     */
    public String getType();

    /**
     * 持久化对象路径
     */
    public String getPath();
 
    public void setPath(String path);

    /**
     * 返回最后一次修改的时间戳timestamp 
     */
    public long getLastModified();

   
    public IPersistenceStore getStore();
 
    void setStore(IPersistenceStore store);

    /**
     * Write the object to the passed output stream.
     * 
     * @param output
     *            Output stream to write to
     * @throws java.io.IOException
     *             Any I/O exception
     */
    void serialize(Output output) throws IOException;

    /**
     * Load the object from the passed input stream.
     * 
     * @param input
     *            Input stream to load from
     * @throws java.io.IOException
     *             Any I/O exception
     */
    void deserialize(Input input) throws IOException;

}
