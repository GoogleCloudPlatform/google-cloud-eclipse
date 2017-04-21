/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Prevents scheduling all future non-system jobs once {@link #suspendFutureJobs} is called, until
 * {@link #resume} is called. Jobs already scheduled are not affected and will run to completion.
 *
 * The class is for https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1155. The
 * purpose of the class is to prevent the second ConvertJob from running. The second ConvertJob is
 * triggered by the first ConvertJob when the latter job installs the JSDT facet.
 *
 * Not recommended to use for other situations, although the workings of the class are general.
 */
class NonSystemJobSuspender {

  private static class SuspendedJob {
    private Job job;
    private long scheduleDelay;  // ms

    private SuspendedJob(Job job, long scheduleDelay) {
      this.job = job;
      this.scheduleDelay = scheduleDelay;
    }
  }

  private static final AtomicBoolean suspended = new AtomicBoolean(false);

  private static final List<SuspendedJob> suspendedJobs = new ArrayList<>();

  private static final NonSystemJobScheduleListener jobScheduleListener =
      new NonSystemJobScheduleListener();

  /** Once called, it is imperative to call {@link #resume()} later. */
  public static void suspendFutureJobs() {
    synchronized (suspendedJobs) {
      Preconditions.checkState(!suspended.getAndSet(true), "Already suspended.");
      Job.getJobManager().addJobChangeListener(jobScheduleListener);
    }
  }

  public static void resume() {
    Preconditions.checkState(suspended.get(), "Not suspended.");
    resumeInternal();
  }

  @VisibleForTesting
  static void resumeInternal() {
    List<SuspendedJob> copy;
    synchronized (suspendedJobs) {
      if (!suspended.getAndSet(false)) {
        return;
      }
      Job.getJobManager().removeJobChangeListener(jobScheduleListener);
      copy = new ArrayList<>(suspendedJobs);
      suspendedJobs.clear();
    }

    for (SuspendedJob jobInfo : copy) {
      jobInfo.job.schedule(jobInfo.scheduleDelay);
    }
  }

  static void suspendJob(Job job, long scheduleDelay) {
    if (job.isSystem()) {
      return;
    }
    synchronized (suspendedJobs) {
      if (!suspended.get()) {
        return;
      }
    }
    // cancel() must be done outside of our synchronization barrier as may trigger events
    // (and even scheduling other jobs)
    if (job.cancel()) {
      // Cancel should always succeed since the job is not running yet
      synchronized (suspendedJobs) {
        // check in case the situation has changed during the cancellation
        if (suspended.get()) {
          suspendedJobs.add(new SuspendedJob(job, scheduleDelay));
        } else {
          // we've since been resumed, so reschedule this job right away
          job.schedule(scheduleDelay);
        }
      }
    }
  }

  private NonSystemJobSuspender() {}
}
