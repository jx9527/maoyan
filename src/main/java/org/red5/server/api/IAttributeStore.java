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

import java.util.Map;
import java.util.Set;

import org.red5.server.jmx.mxbeans.AttributeStoreMXBean;

/**
 * Base interface for all API objects with attributes
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IAttributeStore extends AttributeStoreMXBean {
 
    public Set<String> getAttributeNames();
 
    public Map<String, Object> getAttributes();
 
    public boolean setAttribute(String name, Object value);
 
    boolean setAttribute(final Enum<?> enm, final Object value);
 
    public boolean setAttributes(Map<String, Object> values);
 
    public boolean setAttributes(IAttributeStore values);
 
    public Object getAttribute(String name);
 
    Object getAttribute(Enum<?> enm);
 
    public Object getAttribute(String name, Object defaultValue);
 
    public boolean hasAttribute(String name);
 
    boolean hasAttribute(Enum<?> enm);
 
    public boolean removeAttribute(String name);
 
    boolean removeAttribute(Enum<?> enm);
 
    public void removeAttributes();
 
    public int size();

}
