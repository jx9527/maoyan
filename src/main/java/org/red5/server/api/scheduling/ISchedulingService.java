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

package org.red5.server.api.scheduling;

import java.util.Date;
import java.util.List;

import org.red5.server.api.scope.IScopeService;

/**
 * 支持定期执行作业、添加、删除作业并将其名称作为列表获取的服务。
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface ISchedulingService extends IScopeService {

    public static String BEAN_NAME = "schedulingService";

    /**
     * Scheduling service constant
     */
    public static final String SCHEDULING_SERVICE = "scheduling_service";

    /**
     * Scheduled job constant
     */
    public static final String SCHEDULED_JOB = "scheduled_job";

    /**
     * Schedule a job for periodic execution. 
     */
    public String addScheduledJob(int interval, IScheduledJob job);

    /**
     * Schedule a job for single execution in the future. Please note that the jobs are not saved if Red5 is restarted in the meantime.
    */
    public String addScheduledOnceJob(long timeDelta, IScheduledJob job);

    /**
     * Schedule a job for single execution at a given date. Please note that the jobs are not saved if Red5 is restarted in the meantime.
     */
    public String addScheduledOnceJob(Date date, IScheduledJob job);

    /**
     * Schedule a job for periodic execution which will start after the specified delay.
     */
    public String addScheduledJobAfterDelay(int interval, IScheduledJob job, int delay);
 
    public void pauseScheduledJob(String name);
 
    public void resumeScheduledJob(String name);
 
    public void removeScheduledJob(String name);
 
    public List<String> getScheduledJobNames();

}
