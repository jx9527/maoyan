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

package org.red5.io.flv;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.red5.cache.ICacheStore;
import org.red5.io.IStreamableFile;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.flv.meta.IMetaData;
import org.red5.io.flv.meta.IMetaService;

/**
 * Represents FLV file
 */
public interface IFLV extends IStreamableFile {
 
    public boolean hasMetaData();
 
    @SuppressWarnings({ "rawtypes" })
    public void setMetaData(IMetaData metadata) throws FileNotFoundException, IOException;
 
    public void setMetaService(IMetaService service);
 
    @SuppressWarnings({ "rawtypes" })
    public IMetaData getMetaData() throws FileNotFoundException;
 
    public boolean hasKeyFrameData();
 
    @SuppressWarnings({ "rawtypes" })
    public void setKeyFrameData(Map keyframedata);
 
    @SuppressWarnings({ "rawtypes" })
    public Map getKeyFrameData();
 
    public void refreshHeaders() throws IOException;
 
    public void flushHeaders() throws IOException;
 
    public ITagReader readerFromNearestKeyFrame(int seekPoint);
 
    public ITagWriter writerFromNearestKeyFrame(int seekPoint);
 
    public void setCache(ICacheStore cache);
}
