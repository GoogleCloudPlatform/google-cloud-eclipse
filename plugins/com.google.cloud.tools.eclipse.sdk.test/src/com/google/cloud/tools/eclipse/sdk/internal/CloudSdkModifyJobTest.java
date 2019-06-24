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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.progress.IProgressConstants;
import org.junit.After;
import org.junit.Test;

public class CloudSdkModifyJobTest {

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  @After
  public void tearDown() {
    assertTrue(readWriteLock.writeLock().tryLock());
    readWriteLock.writeLock().unlock();

    IConsole console = findAutoCreatedConsole();
    if (console != null) {
      ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] {console});
    }
  }

  @Test
  public void testBelongsTo() {
    Job installJob = new FakeModifyJob(false /* blockOnStart */);
    assertTrue(installJob.belongsTo(CloudSdkModifyJob.CLOUD_SDK_MODIFY_JOB_FAMILY));
  }

  @Test
  public void testMutexRuleSet() {
    Job installJob = new FakeModifyJob(false /* blockOnStart */);
    assertEquals(CloudSdkModifyJob.MUTEX_RULE, installJob.getRule());
  }

  @Test
  public void testMarkBlocked() {
    IProgressMonitorWithBlocking monitor = mock(IProgressMonitorWithBlocking.class);
    CloudSdkModifyJob.markBlocked(monitor);
    verify(monitor).setBlocked(any(IStatus.class));
  }

  @Test
  public void testClearBlocked() {
    IProgressMonitorWithBlocking monitor = mock(IProgressMonitorWithBlocking.class);
    CloudSdkModifyJob.clearBlocked(monitor);
    verify(monitor).clearBlocked();
  }

  @Test
  public void testConsoleNotCreatedIfGiven() {
    new FakeModifyJob(mock(MessageConsoleStream.class));
    assertNull(findAutoCreatedConsole());
  }

  @Test
  public void testConsoleCreatedIfNotGiven() {
    assertNull(findAutoCreatedConsole());

    new FakeModifyJob(null /* no console stream given */);
    assertNotNull(findAutoCreatedConsole());
  }

  private static IConsole findAutoCreatedConsole() {
    IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
    for (IConsole console : consoles) {
      if ("Configuring Google Cloud SDK".equals(console.getName())) {
        return console;
      }
    }
    return null;
  }

  @Test
  public void testNoActionPropertyIfConsoleGiven() {
    Job job = new FakeModifyJob(mock(MessageConsoleStream.class));
    Object actionProperty = job.getProperty(IProgressConstants.ACTION_PROPERTY);
    assertNull(actionProperty);
  }

  @Test
  public void testShowConsoleActionPropertyIfConsoleCreated() {
    Job job = new FakeModifyJob(null /* no console stream given */);
    Object actionProperty = job.getProperty(IProgressConstants.ACTION_PROPERTY);
    assertTrue(actionProperty instanceof ShowConsoleViewAction);
  }

  @Test
  public void testRun_unlocksAfterReturn() throws InterruptedException {
    Job installJob = new FakeModifyJob(false /* blockOnStart */);
    installJob.schedule();
    installJob.join();

    assertTrue(installJob.getResult().isOK());

    assertTrue(readWriteLock.writeLock().tryLock());
    readWriteLock.writeLock().unlock();
  }

  @Test
  public void testRun_mutualExclusion() throws InterruptedException {
    FakeModifyJob job1 = new FakeModifyJob(true /* blockOnStart */);
    FakeModifyJob job2 = new FakeModifyJob(true /* blockOnStart */);

    job1.schedule();
    while (job1.getState() != Job.RUNNING) {
      Thread.sleep(50);
    }

    job2.schedule();
    // Incomplete test, but if it ever fails, something is surely broken.
    assertNotEquals(Job.RUNNING, job2.getState());

    job1.unblock();
    job2.unblock();
    job1.join();
    job2.join();
  }

  @Test
  public void testRun_blockedUntilWritable() throws InterruptedException {
    assertTrue(readWriteLock.readLock().tryLock());
    boolean locked = true;

    try {
      FakeModifyJob job = new FakeModifyJob(true /* blockOnStart */);
      job.schedule();
      while (job.getState() != Job.RUNNING) {
        Thread.sleep(50);
      }
      // Incomplete test, but if it ever fails, something is surely broken.
      assertFalse(job.modifiedSdk);

      readWriteLock.readLock().unlock();
      locked = false;
      job.unblock();
      job.join();
    } finally {
      if (locked) {
        readWriteLock.readLock().unlock();
      }
    }
  }

  private class FakeModifyJob extends CloudSdkModifyJob {

    private final Semaphore blocker = new Semaphore(0);
    private final boolean blockOnStart;
    private boolean modifiedSdk;

    private FakeModifyJob(boolean blockOnStart) {
      super(mock(MessageConsoleStream.class), readWriteLock);
      this.blockOnStart = blockOnStart;
    }

    private FakeModifyJob(MessageConsoleStream consoleStream) {
      super(consoleStream, readWriteLock);
      blockOnStart = false;
    }

    @Override
    protected IStatus modifySdk(IProgressMonitor monitor) {
      modifiedSdk = true;
      try {
        if (blockOnStart) {
          blocker.acquire();
        }
      } catch (InterruptedException e) {
        return Status.CANCEL_STATUS;
      }
      return Status.OK_STATUS;
    }

    private void unblock() {
      blocker.release();
    }
  };
}
