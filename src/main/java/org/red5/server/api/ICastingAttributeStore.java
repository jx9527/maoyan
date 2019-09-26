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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Attribute storage with automatic object casting support.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface ICastingAttributeStore extends IAttributeStore {
 
    public Boolean getBoolAttribute(String name);
 
    public Byte getByteAttribute(String name);
 
    public Double getDoubleAttribute(String name);
 
    public Integer getIntAttribute(String name);
 
    public List<?> getListAttribute(String name);
 
    public Long getLongAttribute(String name);
 
    public Map<?, ?> getMapAttribute(String name);
 
    public Set<?> getSetAttribute(String name);
 
    public Short getShortAttribute(String name);
 
    public String getStringAttribute(String name);
}
