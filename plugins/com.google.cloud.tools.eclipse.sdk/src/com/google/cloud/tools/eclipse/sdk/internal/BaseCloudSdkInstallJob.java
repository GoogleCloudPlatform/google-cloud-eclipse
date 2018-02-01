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

import com.google.cloud.tools.eclipse.sdk.Messages;
import com.google.cloud.tools.eclipse.util.jobs.MutexRule;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.locks.ReadWriteLock;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsoleStream;

public abstract class BaseCloudSdkInstallJob extends Job {

  public static final Object CLOUD_SDK_MODIFY_JOB_FAMILY = new Object();

  /** Scheduling rule to prevent running {@code CloudSdkInstallJob} concurrently. */
  @VisibleForTesting
  static final MutexRule MUTEX_RULE = new MutexRule();

  private final MessageConsoleStream consoleStream;
  private final ReadWriteLock cloudSdkLock;

  public BaseCloudSdkInstallJob(MessageConsoleStream consoleStream, ReadWriteLock cloudSdkLock) {
    super(Messages.getString("InstallJobName")); //$NON-NLS-1$
    this.consoleStream = consoleStream;
    this.cloudSdkLock = cloudSdkLock;
    setRule(MUTEX_RULE);
  }

  @Override
  public boolean belongsTo(Object family) {
    return super.belongsTo(family) || family == CLOUD_SDK_MODIFY_JOB_FAMILY;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      markBlocked(monitor);  // for better UI reporting of lock-waiting.
      cloudSdkLock.writeLock().lockInterruptibly();
    } catch (InterruptedException e) {
      return Status.CANCEL_STATUS;
    } finally {
      clearBlocked(monitor);
    }

    try {
      if (consoleStream != null) {
        consoleStream.println(Messages.getString("InstallStarting")); //$NON-NLS-1$
      }
      return installSdk();
    } finally {
      cloudSdkLock.writeLock().unlock();
    }
  }

  protected abstract IStatus installSdk();

  @VisibleForTesting
  static void markBlocked(IProgressMonitor monitor) {
    if (monitor instanceof IProgressMonitorWithBlocking) {
      IStatus reason = StatusUtil.info(BaseCloudSdkInstallJob.class,
          Messages.getString("sdkModificationLocked")); //$NON-NLS-1$
      ((IProgressMonitorWithBlocking) monitor).setBlocked(reason);
    }
  }

  @VisibleForTesting
  static void clearBlocked(IProgressMonitor monitor) {
    if (monitor instanceof IProgressMonitorWithBlocking) {
      ((IProgressMonitorWithBlocking) monitor).clearBlocked();
    }
  }
}