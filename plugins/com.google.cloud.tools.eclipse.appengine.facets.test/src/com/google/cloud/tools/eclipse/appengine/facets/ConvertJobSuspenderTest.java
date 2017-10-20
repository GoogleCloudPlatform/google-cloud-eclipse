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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Predicates;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.jsdt.web.core.javascript.JsNameManglerUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ConvertJobSuspenderTest {

  private Job job1 = new NoOpSpinJob("Test job 1");
  private Job job2 = new NoOpSpinJob("Test job 2");

  @Before
  public void setUp() {
    // Assume all test jobs are "ConvertJob"s.
    ConvertJobSuspender.isConvertJob = Predicates.alwaysTrue();
  }

  @After
  public void tearDown() {
    ConvertJobSuspender.resumeInternal();
    job1.cancel();
    job2.cancel();
  }

  @Test
  public void testConvertJobClassName() {
    String convertJobClass =
        ConvertJobSuspender.CONVERT_JOB_CLASS_NAME.replace(".", "/") + ".class";
    Bundle jsdtWebCoreBundle = FrameworkUtil.getBundle(JsNameManglerUtil.class);
    assertNotNull(jsdtWebCoreBundle.getResource(convertJobClass));
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
    job1.schedule();
    job2.schedule(10000 /* ms */);
    assertEquals(Job.NONE, job1.getState());
    assertEquals(Job.NONE, job2.getState());
  }

  @Test
  public void testScheduledJobsAreNotSuspended() {
    job1.schedule();
    job2.schedule(10000 /* ms */);
    ConvertJobSuspender.suspendFutureConvertJobs();
    assertTrue(Job.WAITING == job1.getState() || Job.RUNNING == job1.getState());
    assertEquals(Job.SLEEPING, job2.getState());
  }

  @Test
  public void testNonConvertJobsAreNotSuspended() {
    ConvertJobSuspender.suspendFutureConvertJobs();
    // Assume all test jobs are not "ConvertJob"s.
    ConvertJobSuspender.isConvertJob = Predicates.alwaysFalse();
    job1.schedule();
    job2.schedule(10000 /* ms */);
    assertTrue(Job.WAITING == job1.getState() || Job.RUNNING == job1.getState());
    assertEquals(Job.SLEEPING, job2.getState());
  }

  @Test
  public void testResume() {
    ConvertJobSuspender.suspendFutureConvertJobs();
    job1.schedule();
    job2.schedule(10000 /* ms */);
    assertEquals(Job.NONE, job1.getState());
    assertEquals(Job.NONE, job2.getState());

    ConvertJobSuspender.resume();
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
