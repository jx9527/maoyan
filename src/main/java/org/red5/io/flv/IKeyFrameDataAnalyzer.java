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

import java.io.Serializable;
import java.util.Arrays;

public interface IKeyFrameDataAnalyzer {
 
    public KeyFrameMeta analyzeKeyFrames();
 
    public static class KeyFrameMeta implements Serializable {

        private static final long serialVersionUID = 5436632873705625365L;
 
        public int videoCodecId = -1;
 
        public int audioCodecId = -1;
 
        public long duration;
 
        public boolean audioOnly;
 
        public int timestamps[];
 
        public long positions[];

        @Override
        public String toString() {
            return "KeyFrameMeta [videoCodecId=" + videoCodecId + ", audioCodecId=" + audioCodecId + ", duration=" + duration + ", audioOnly=" + audioOnly + ", timestamps=" + Arrays.toString(timestamps) + ", positions=" + Arrays.toString(positions) + "]";
        }
    }
}
