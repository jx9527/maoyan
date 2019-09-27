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

import org.red5.server.api.scheduling.IScheduledJob;

/**
 * ISubscriberStream is a stream from subscriber's point of view. That is, it provides methods for common stream operations like play, pause or seek.
 */
public interface ISubscriberStream extends IClientStream {
    
    void play() throws IOException;
 
    void pause(int position);
 
    void resume(int position);
 
    void stop();
 
    void seek(int position) throws OperationNotSupportedException;
 
    boolean isPaused();
 
    void receiveVideo(boolean receive);
 
    void receiveAudio(boolean receive);
 
    public StreamState getState();
 
    public void setState(StreamState state);
 
    public void onChange(final StreamState state, final Object... changed);
 
    public String scheduleOnceJob(IScheduledJob job);
 
    public String scheduleWithFixedDelay(IScheduledJob job, int interval);
 
    public void cancelJob(String jobName);

}
