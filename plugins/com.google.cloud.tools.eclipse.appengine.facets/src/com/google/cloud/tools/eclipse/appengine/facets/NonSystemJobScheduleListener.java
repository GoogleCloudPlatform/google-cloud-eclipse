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

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;

/**
 * For private use in {@link NonSystemJobSuspender} only. Listens for every job being scheduled
 * and cancels it if the suspender is active.
 */
class NonSystemJobScheduleListener implements IJobChangeListener {
  @Override
  public void scheduled(IJobChangeEvent event) {
    NonSystemJobSuspender.suspendJob(event.getJob(), event.getDelay());
  }

  @Override
  public void aboutToRun(IJobChangeEvent event) {}

  @Override
  public void awake(IJobChangeEvent event) {}

  @Override
  public void done(IJobChangeEvent event) {}

  @Override
  public void running(IJobChangeEvent event) {}

  @Override
  public void sleeping(IJobChangeEvent event) {}
}
