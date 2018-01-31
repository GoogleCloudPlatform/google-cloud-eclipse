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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.locks.Lock;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Test;

public class CloudSdkManagerTest {

  @After
  public void tearDown() {
    CloudSdkManager.forceManagedSdkFeature = false;
  }

  @Test
  public void testManagedSdkOption() {
    assertFalse(CloudSdkManager.isManagedSdkFeatureEnabled());
  }

  @Test
  public void testManagedSdkOption_featureForced() {
    CloudSdkManager.forceManagedSdkFeature = true;
    assertTrue(CloudSdkManager.isManagedSdkFeatureEnabled());
  }

  @Test
  public void testPreventModifyingSdk_cannotWrite() throws InterruptedException {
    CloudSdkManager.preventModifyingSdk();
    try {
      assertFalse(CloudSdkManager.useModifyLock.writeLock().tryLock());
    } finally {
      CloudSdkManager.allowModifyingSdk();
    }
  }

  @Test
  public void testPreventModifyingSdk_canRead() throws InterruptedException {
    CloudSdkManager.preventModifyingSdk();
    try {
      Lock readLock = CloudSdkManager.useModifyLock.readLock();
      assertTrue(readLock.tryLock());
      readLock.unlock();
    } finally {
      CloudSdkManager.allowModifyingSdk();
    }
  }

  @Test
  public void testAllowModifyingSdk_allowsWrite() throws InterruptedException {
    CloudSdkManager.preventModifyingSdk();
    CloudSdkManager.allowModifyingSdk();

    Lock writeLock = CloudSdkManager.useModifyLock.writeLock();
    assertTrue(writeLock.tryLock());
    writeLock.unlock();
  }

  @Test
  public void testPreventModifyingSdk_doesNotBlockSimultaneousCalls() throws InterruptedException {
    CloudSdkManager.preventModifyingSdk();

    try {
      Job job = new Job("another caller") {
        @Override
        public IStatus run(IProgressMonitor monitor) {
          try {
            CloudSdkManager.preventModifyingSdk();
            return Status.OK_STATUS;
          } catch (InterruptedException e) {
            return Status.CANCEL_STATUS;
          } finally {
            CloudSdkManager.allowModifyingSdk();
          }
        }
      };
      job.schedule();
      job.join();

      assertTrue(job.getResult().isOK());
    } finally {
      CloudSdkManager.allowModifyingSdk();
    }
  }
}
