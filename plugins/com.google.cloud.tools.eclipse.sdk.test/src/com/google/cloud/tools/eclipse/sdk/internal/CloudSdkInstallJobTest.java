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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
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

  @Mock private Lock writeLock;
  @Mock private ReadWriteLock readWriteLock;
  @Mock private MessageConsoleStream consoleStream;

  private CloudSdkInstallJob installJob;

  @Before
  public void setUp() {
    when(readWriteLock.writeLock()).thenReturn(writeLock);

    installJob = new CloudSdkInstallJob(consoleStream, readWriteLock);
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
  public void testRun_consoleStreamOutput() throws InterruptedException {
    installJob.schedule();
    installJob.join();

    verify(consoleStream).println("Installing the Cloud SDK... (may take up to several minutes)");
  }

  @Test
  public void testRun_unlockAfterReturn() throws InterruptedException {
    installJob.schedule();
    installJob.join();

    assertTrue(installJob.getResult().isOK());
    verify(writeLock, times(1)).unlock();
  }

  @Test
  public void testRun_mutualExclusion() throws InterruptedException {
    installJob.schedule();

    Job secondJob = new CloudSdkInstallJob(null, readWriteLock);
    secondJob.schedule();
    // Incomplete test, but if it ever fails, something is surely broken.
    assertNotEquals(Job.RUNNING, secondJob.getState());

    installJob.join();
    secondJob.join();
  }
}
