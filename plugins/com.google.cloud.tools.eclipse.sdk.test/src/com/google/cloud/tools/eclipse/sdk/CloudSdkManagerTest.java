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

package com.google.cloud.tools.eclipse.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkModifyJob;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsoleStream;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CloudSdkManagerTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final ReadWriteLock modifyLock = new ReentrantReadWriteLock();
  private final CloudSdkManager fixture = new CloudSdkManager(modifyLock);

  @After
  public void tearDown() {
    assertTrue("write lock not available", modifyLock.writeLock().tryLock());
  }

  @Test
  public void testRunInstallJob_blocking() {
    CloudSdkModifyJob okJob = new FakeModifyJob(Status.OK_STATUS);
    IStatus result = CloudSdkManager.runInstallJob(null, okJob, null);
    // Incomplete test, but if it ever fails, something is surely broken.
    assertEquals(Job.NONE, okJob.getState());
    assertTrue(result.isOK());
  }

  @Test
  public void testRunInstallJob_canceled() {
    CloudSdkModifyJob cancelJob = new FakeModifyJob(Status.CANCEL_STATUS);
    IStatus result = CloudSdkManager.runInstallJob(null, cancelJob, null);
    assertEquals(Job.NONE, cancelJob.getState());
    assertEquals(Status.CANCEL, result.getSeverity());
  }

  @Test
  public void testRunInstallJob_installError() {
    IStatus error = StatusUtil.error(this, "awesome install error in unit test");
    CloudSdkModifyJob errorJob = new FakeModifyJob(error);
    IStatus result = CloudSdkManager.runInstallJob(null, errorJob, null);
    assertEquals(Job.NONE, errorJob.getState());
    assertEquals(Status.ERROR, result.getSeverity());
    assertEquals("awesome install error in unit test", result.getMessage());
  }

  @Test
  public void testPreventModifyingSdk_cannotWrite() throws InterruptedException {
    fixture.preventModifyingSdk();
    try {
      assertFalse(modifyLock.writeLock().tryLock());
    } finally {
      fixture.allowModifyingSdk();
    }
  }

  @Test
  public void testPreventModifyingSdk_canRead() throws InterruptedException {
    fixture.preventModifyingSdk();
    try {
      Lock readLock = modifyLock.readLock();
      assertTrue(readLock.tryLock());
      readLock.unlock();
    } finally {
      fixture.allowModifyingSdk();
    }
  }

  @Test
  public void testAllowModifyingSdk_allowsWrite() throws InterruptedException {
    fixture.preventModifyingSdk();
    fixture.allowModifyingSdk();

    Lock writeLock = modifyLock.writeLock();
    assertTrue(writeLock.tryLock());
    writeLock.unlock();
  }

  @Test
  public void testPreventModifyingSdk_doesNotBlockSimultaneousCalls() throws InterruptedException {
    fixture.preventModifyingSdk();

    try {
      Job job =
          new Job("another caller") {
            @Override
            public IStatus run(IProgressMonitor monitor) {
              try {
                fixture.preventModifyingSdk();
                return Status.OK_STATUS;
              } catch (InterruptedException e) {
                return Status.CANCEL_STATUS;
              } finally {
                fixture.allowModifyingSdk();
              }
            }
          };
      job.schedule();
      job.join();

      assertTrue(job.getResult().isOK());
    } finally {
      fixture.allowModifyingSdk();
    }
  }

  @Test
  public void testPreventModifyingSdk_blocksRunInstallJob() throws InterruptedException {
    fixture.preventModifyingSdk();
    boolean prevented = true;

    try {
      final CloudSdkModifyJob installJob = new FakeModifyJob(Status.OK_STATUS);

      Job concurrentLauncher =
          new Job("concurrent thread attempting runInstallJob()") {
            @Override
            public IStatus run(IProgressMonitor monitor) {
              // Should block until we allow SDK modification below.
              CloudSdkManager.runInstallJob(null, installJob, monitor);
              return Status.OK_STATUS;
            }
          };
      concurrentLauncher.schedule();

      while (installJob.getState() != Job.RUNNING) {
        Thread.sleep(50);
      }
      // Incomplete test, but if it ever fails, something is surely broken.
      assertEquals(Job.RUNNING, concurrentLauncher.getState());

      fixture.allowModifyingSdk();
      prevented = false;
      concurrentLauncher.join();

      // Incomplete test, but if it ever fails, something is surely broken.
      assertTrue(installJob.getResult().isOK());
      assertTrue(concurrentLauncher.getResult().isOK());
    } finally {
      if (prevented) {
        fixture.allowModifyingSdk();
      }
    }
  }

  private class FakeModifyJob extends CloudSdkModifyJob {

    private final IStatus result;

    private FakeModifyJob(IStatus result) {
      super(mock(MessageConsoleStream.class), modifyLock);
      this.result = result;
    }

    @Override
    protected IStatus modifySdk(IProgressMonitor monitor) {
      return result;
    } 
  }
}
