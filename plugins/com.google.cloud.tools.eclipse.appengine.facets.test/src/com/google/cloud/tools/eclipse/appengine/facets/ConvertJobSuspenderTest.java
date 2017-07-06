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
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Test;

public class ConvertJobSuspenderTest {

  private Job convertJob1 = new NoOpSpinJob("Configuring for JavaScript");
  private Job convertJob2 = new NoOpSpinJob("Configuring for JavaScript");

  @After
  public void tearDown() {
    ConvertJobSuspender.resumeInternal();
    convertJob1.cancel();
    convertJob2.cancel();
  }

  @Test
  public void testCannotSuspendConcurrently() {
    ConvertJobSuspender.suspendFutureConvertJobs();
    try {
      ConvertJobSuspender.suspendFutureConvertJobs();
      fail();
    } catch (IllegalStateException ex) {
      assertEquals("Already suspended.", ex.getMessage());
    }
  }

  @Test
  public void testCannotResumeIfNotSuspended() {
    try {
      ConvertJobSuspender.resume();
      fail();
    } catch (IllegalStateException ex) {
      assertEquals("Not suspended.", ex.getMessage());
    }
  }

  @Test
  public void testSuspendFutureConvertJobs() {
    ConvertJobSuspender.suspendFutureConvertJobs();
    convertJob1.schedule();
    convertJob2.schedule(10000 /* ms */);
    assertEquals(Job.NONE, convertJob1.getState());
    assertEquals(Job.NONE, convertJob2.getState());
  }

  @Test
  public void testScheduledJobsAreNotSuspended() {
    convertJob1.schedule();
    convertJob2.schedule(10000 /* ms */);
    ConvertJobSuspender.suspendFutureConvertJobs();
    assertTrue(Job.WAITING == convertJob1.getState() || Job.RUNNING == convertJob1.getState());
    assertEquals(Job.SLEEPING, convertJob2.getState());
  }

  @Test
  public void testNonConvertJobsAreNotSuspended() {
    Job job1 = new NoOpSpinJob("Non-ConvertJob 1");
    Job job2 = new NoOpSpinJob("Non-ConvertJob 2");
    try {
      ConvertJobSuspender.suspendFutureConvertJobs();
      job1.schedule();
      job2.schedule(10000 /* ms */);
      assertTrue(Job.WAITING == job1.getState() || Job.RUNNING == job1.getState());
      assertEquals(Job.SLEEPING, job2.getState());
    } finally {
      job1.cancel();
      job2.cancel();
    }
  }

  @Test
  public void testResume() {
    ConvertJobSuspender.suspendFutureConvertJobs();
    convertJob1.schedule();
    convertJob2.schedule(10000 /* ms */);
    assertEquals(Job.NONE, convertJob1.getState());
    assertEquals(Job.NONE, convertJob2.getState());

    ConvertJobSuspender.resume();
    assertTrue(Job.WAITING == convertJob1.getState() || Job.RUNNING == convertJob1.getState());
    assertEquals(Job.SLEEPING, convertJob2.getState());
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
