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

import java.util.Collection;
import java.util.Set;

/**
 * Storage for persistent objects.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 */

public interface IPersistenceStore { 
     
    public boolean save(IPersistable obj);
 
    public IPersistable load(String name); 
     
    public boolean load(IPersistable obj);
 
    public boolean remove(IPersistable obj);
 
    public boolean remove(String name); 
     
    public Set<String> getObjectNames(); 
     
    public Collection<IPersistable> getObjects();

    /**
     * Notify store that it's being closed. This allows the store to write any pending objects to disk.
     */
    public void notifyClose();

}
