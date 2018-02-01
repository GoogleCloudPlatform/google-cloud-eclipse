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
import static org.mockito.Mockito.verify;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
public class CloudSdkModifyJobTest {

  @Mock private MessageConsoleStream consoleStream;

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private CloudSdkModifyJob installJob;

  @Before
  public void setUp() {
    installJob = new FakeInstallJob(consoleStream);
  }

  @After
  public void tearDown() {
    assertEquals(Job.NONE, installJob.getState());

    assertTrue(readWriteLock.writeLock().tryLock());
    readWriteLock.writeLock().unlock();
  }

  @Test
  public void testBelongsTo() {
    assertTrue(installJob.belongsTo(CloudSdkModifyJob.CLOUD_SDK_MODIFY_JOB_FAMILY));
  }

  @Test
  public void testMutexRuleSet() {
    assertEquals(CloudSdkModifyJob.MUTEX_RULE, installJob.getRule());
  }

  @Test
  public void testRun_consoleStreamOutput() throws InterruptedException {
    installJob.schedule();
    installJob.join();

    verify(consoleStream).println(
        "Installing/upgrading the Cloud SDK... (may take several minutes)");
  }

  @Test
  public void testRun_mutualExclusion() throws InterruptedException {
    installJob.schedule();

    Job job2 = new FakeInstallJob(null);
    Job job3 = new FakeInstallJob(null);
    job2.schedule();
    job3.schedule();
    // Incomplete test, but if it ever fails, something is surely broken.
    assertNotEquals(Job.RUNNING, job3.getState());
    assertNotEquals(Job.RUNNING, job2.getState());

    installJob.join();
    job3.join();
    job2.join();
  }

  private class FakeInstallJob extends CloudSdkModifyJob {

    public FakeInstallJob(MessageConsoleStream consoleStream) {
      super("fake job", consoleStream);
    }

    @Override
    protected IStatus modifySdk() {
      return Status.OK_STATUS;
    }
  };
}
