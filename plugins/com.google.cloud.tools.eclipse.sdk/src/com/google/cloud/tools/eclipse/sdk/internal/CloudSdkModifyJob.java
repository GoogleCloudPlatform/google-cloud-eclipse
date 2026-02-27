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

import com.google.cloud.tools.appengine.operations.CloudSdk;
import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkVersionFileException;
import com.google.cloud.tools.eclipse.sdk.Messages;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities;
import com.google.cloud.tools.eclipse.util.jobs.MutexRule;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * A base class for any jobs that seek to modify the managed Google Cloud SDK, ensuring that
 * modification changes are serialized.
 */
public abstract class CloudSdkModifyJob extends Job {

  public static final Object CLOUD_SDK_MODIFY_JOB_FAMILY = new Object();

  /** Scheduling rule to prevent running {@link CloudSdkModifyJob} concurrently. */
  @VisibleForTesting
  static final MutexRule MUTEX_RULE =
      new MutexRule("for " + CloudSdkModifyJob.class); // $NON-NLS-1$

  protected final MessageConsoleStream consoleStream;
  private final ReadWriteLock cloudSdkLock;

  /** The severity reported on modification failure. */
  private final int failureSeverity;

  public CloudSdkModifyJob(MessageConsoleStream consoleStream, ReadWriteLock cloudSdkLock) {
    this(consoleStream, cloudSdkLock, IStatus.ERROR);
  }
  
  public CloudSdkModifyJob(MessageConsoleStream consoleStream, ReadWriteLock cloudSdkLock, int failureSeverity) {
    super(Messages.getString("configuring.cloud.sdk")); // $NON-NLS-1$
    this.consoleStream = consoleStream != null ? consoleStream : createNewMessageConsole();
    this.cloudSdkLock = cloudSdkLock;
    this.failureSeverity = failureSeverity;
    setRule(MUTEX_RULE);
  }

  @VisibleForTesting
  MessageConsoleStream createNewMessageConsole() {
    MessageConsole console = MessageConsoleUtilities.getMessageConsole(
        Messages.getString("configuring.cloud.sdk"), // $NON-NLS-1$
        null /* imageDescriptor */);

    setProperty(IProgressConstants.ACTION_PROPERTY, new ShowConsoleViewAction(console));

    return console.newMessageStream();
  }

  @Override
  public boolean belongsTo(Object family) {
    return super.belongsTo(family) || family == CLOUD_SDK_MODIFY_JOB_FAMILY;
  }

  @Override
  protected final IStatus run(IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }

    try {
      markBlocked(monitor);  // for better UI reporting of lock-waiting.
      cloudSdkLock.writeLock().lockInterruptibly();
    } catch (InterruptedException e) {
      return Status.CANCEL_STATUS;
    } finally {
      clearBlocked(monitor);
    }

    try {
      return modifySdk(monitor);
    } finally {
      cloudSdkLock.writeLock().unlock();
    }
  }

  protected abstract IStatus modifySdk(IProgressMonitor monitor);

  /** Retrieve the version of the Cloud SDK at the provided location. */
  protected static String getVersion(Path sdkPath) 
      throws CloudSdkVersionFileException, CloudSdkNotFoundException {
    CloudSdk sdk = new CloudSdk.Builder().sdkPath(sdkPath).build();
    return sdk.getVersion().toString();
  }

  @VisibleForTesting
  static void markBlocked(IProgressMonitor monitor) {
    if (monitor instanceof IProgressMonitorWithBlocking) {
      IStatus reason =
          StatusUtil.info(
              CloudSdkModifyJob.class, Messages.getString("cloud.sdk.in.use")); // $NON-NLS-1$
      ((IProgressMonitorWithBlocking) monitor).setBlocked(reason);
    }
  }

  @VisibleForTesting
  static void clearBlocked(IProgressMonitor monitor) {
    if (monitor instanceof IProgressMonitorWithBlocking) {
      ((IProgressMonitorWithBlocking) monitor).clearBlocked();
    }
  }

  protected void subTask(IProgressMonitor monitor, String description) {
    monitor.subTask(description);
    // make output headers distinguishable on the console
    String section = "[" + description + "]"; // $NON-NLS-1$ // $NON-NLS-2$
    consoleStream.println(section);
  }

  @VisibleForTesting
  protected ManagedCloudSdk getManagedCloudSdk() throws UnsupportedOsException {
    return ManagedCloudSdk.newManagedSdk();
  }

  @Override
  protected void canceling() {
    // By the design of the appengine-plugins-core SDK downloader, cancellation support is
    // implemented through the Java thread interruption facility.
    Thread jobThread = getThread();
    if (jobThread != null) {
      jobThread.interrupt();
    }
  }

  /** 
   * Returns the severity reported on modification failure.  This is useful for
   * situations where the Cloud SDK modification is a step of some other work, and the modification
   * failure should be surfaced to the user in the context of that work. If reported as {@link
   * IStatus#ERROR} then the Eclipse UI ProgressManager will report the modification failure
   * directly.
   */
  protected int getFailureSeverity() {
    return failureSeverity;
  }
}
