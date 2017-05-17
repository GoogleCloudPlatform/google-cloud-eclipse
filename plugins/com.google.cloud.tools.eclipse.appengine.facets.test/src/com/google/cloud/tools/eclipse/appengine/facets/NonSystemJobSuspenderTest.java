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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Test;

public class NonSystemJobSuspenderTest {

  private Job job1 = new NoOpSpinJob("Test job 1");
  private Job job2 = new NoOpSpinJob("Test job 2");

  @After
  public void tearDown() {
    NonSystemJobSuspender.resumeInternal();
    job1.cancel();
    job2.cancel();
  }

  @Test(expected = IllegalStateException.class)
  public void testCannotSuspendConcurrently() {
    NonSystemJobSuspender.suspendFutureJobs();
    NonSystemJobSuspender.suspendFutureJobs();
  }

  @Test(expected = IllegalStateException.class)
  public void testCannotResumeIfNotSuspended() {
    NonSystemJobSuspender.resume();
  }

  @Test
  public void testSuspendFutureJobs() {
    NonSystemJobSuspender.suspendFutureJobs();
    job1.schedule();
    job2.schedule(10000 /* ms */);
    assertEquals(Job.NONE, job1.getState());
    assertEquals(Job.NONE, job2.getState());
  }

  @Test
  public void testScheduledJobsAreNotSuspended() {
    job1.schedule();
    job2.schedule(10000 /* ms */);
    NonSystemJobSuspender.suspendFutureJobs();
    assertTrue(Job.WAITING == job1.getState() || Job.RUNNING == job1.getState());
    assertEquals(Job.SLEEPING, job2.getState());
  }

  @Test
  public void testSystemJobsAreNotSuspended() {
    NonSystemJobSuspender.suspendFutureJobs();
    job1.setSystem(true);
    job2.setSystem(true);
    job1.schedule();
    job2.schedule(10000 /* ms */);
    assertTrue(Job.WAITING == job1.getState() || Job.RUNNING == job1.getState());
    assertEquals(Job.SLEEPING, job2.getState());
  }

  @Test
  public void testResume() {
    NonSystemJobSuspender.suspendFutureJobs();
    job1.schedule();
    job2.schedule(10000 /* ms */);
    assertEquals(Job.NONE, job1.getState());
    assertEquals(Job.NONE, job2.getState());

    NonSystemJobSuspender.resume();
    assertTrue(Job.WAITING == job1.getState() || Job.RUNNING == job1.getState());
    assertEquals(Job.SLEEPING, job2.getState());
  }

  private static class NoOpSpinJob extends Job {

    private NoOpSpinJob(String name) {
      super(name);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      while (!monitor.isCanceled()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ex) {}
      }
      return Status.OK_STATUS;
    }
  }
}
