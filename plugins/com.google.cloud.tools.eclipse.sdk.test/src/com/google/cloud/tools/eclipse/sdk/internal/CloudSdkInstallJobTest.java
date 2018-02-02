/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.sdk.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Semaphore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsoleStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkInstallJobTest {

  @Mock private MessageConsoleStream consoleStream;

  private CloudSdkInstallJob installJob;

  @Before
  public void setUp() {
    installJob = new FakeInstallJob(consoleStream, false /* blockOnStart */);
  }

  @After
  public void tearDown() {
    assertEquals(Job.NONE, installJob.getState());
  }

  @Test
  public void testBelongsTo() {
    assertTrue(installJob.belongsTo(CloudSdkInstallJob.CLOUD_SDK_MODIFY_JOB_FAMILY));
  }

  @Test
  public void testMutexRuleSet() {
    assertEquals(CloudSdkInstallJob.MUTEX_RULE, installJob.getRule());
  }

  @Test
  public void testRun_mutualExclusion() throws InterruptedException {
    FakeInstallJob job1 = new FakeInstallJob(null, true /* blockOnStart */);
    FakeInstallJob job2 = new FakeInstallJob(null, true);

    job1.schedule();
    while (job1.getState() != Job.RUNNING) {
      Thread.sleep(10);
    }

    job2.schedule();
    // Incomplete test, but if it ever fails, something is surely broken.
    assertNotEquals(Job.RUNNING, job2.getState());

    job1.unblock();
    job2.unblock();
    job1.join();
    job2.join();
  }

  private class FakeInstallJob extends CloudSdkInstallJob {

    private final Semaphore blocker = new Semaphore(0);
    private final boolean blockOnStart;

    private FakeInstallJob(MessageConsoleStream consoleStream, boolean blockOnStart) {
      super(consoleStream);
      this.blockOnStart = blockOnStart;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        if (blockOnStart) {
          blocker.acquire();
        }
        return Status.OK_STATUS;
      } catch (InterruptedException e) {
        return Status.CANCEL_STATUS;
      }
    }

    private void unblock() {
      blocker.release();
    }
  };
}
