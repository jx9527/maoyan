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

package org.red5.io;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * A Tag represents the contents or payload of a streamable file.
 *
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface ITag extends IoConstants {
 
    public IoBuffer getBody();
 
    public int getBodySize();
 
    public IoBuffer getData();
 
    public byte getDataType();
 
    public int getPreviousTagSize();
 
    public int getTimestamp();
 
    public void setBody(IoBuffer body);
 
    public void setBodySize(int size);
 
    public void setDataType(byte datatype);
 
    public void setPreviousTagSize(int size);
 
    public void setTimestamp(int timestamp);

}
