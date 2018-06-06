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

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.InvalidJavaSdkException;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkInstallJob;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkModifyJob;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkUpdateJob;
import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsoleStream;

public class CloudSdkManager {

  private static CloudSdkManager instance;

  // readers = using SDK, writers = modifying SDK
  private final ReadWriteLock modifyLock;

  public static synchronized CloudSdkManager getInstance() {
    if (instance == null) {
      instance = new CloudSdkManager(new ReentrantReadWriteLock());
    }
    return instance;
  }

  @VisibleForTesting
  CloudSdkManager(ReadWriteLock modifyLock) {
    this.modifyLock = modifyLock;
  }

  /**
   * Prevents potential future SDK auto-install or auto-update functionality to allow safely using
   * the managed Cloud SDK for some period of time. Blocks if an install or update is in progress.
   * Callers must call {@code CloudSdkManager#allowModifyingSdk} eventually to lift the suspension.
   * Any callers that intend to use {@code CloudSdk} must always call this before staring work, even
   * if the Cloud SDK preferences are configured not to auto-managed the SDK.
   *
   * <p>Must not be called from the UI thread, because the method can block.
   *
   * @see CloudSdkManager#allowModifyingSdk
   */
  public void preventModifyingSdk() throws InterruptedException {
    do {
      IJobManager jobManager = Job.getJobManager();
      // The join is to improve UI reporting of blocked jobs. Most of the waiting should be here.
      jobManager.join(CloudSdkModifyJob.CLOUD_SDK_MODIFY_JOB_FAMILY, null /* no monitor */);
    } while (!modifyLock.readLock().tryLock(10, TimeUnit.MILLISECONDS));
    // We have acquired the read lock; all further install/update should be blocked, while others
    // can still grab a read lock and use the Cloud SDK.
  }

  /**
   * Allows future SDK auto-install or auto-update temporarily prevented by {@code
   * CloudSdkManager#preventModifyingSdk}.
   *
   * @see CloudSdkManager#preventModifyingSdk
   */
  public void allowModifyingSdk() {
    modifyLock.readLock().unlock();
  }

  /**
   * Triggers the installation of a Cloud SDK, if the preferences are configured to auto-manage the
   * SDK.
   */
  public void installManagedSdkAsync() {
    if (CloudSdkPreferences.isAutoManaging()) {
      // Keep installation failure as ERROR so that failures are reported
      Job installJob = new CloudSdkInstallJob(null /* no console output */, modifyLock);
      installJob.setUser(false);
      installJob.schedule();
    }
  }

  /**
   * Installs a Cloud SDK, if the preferences are configured to auto-manage the SDK. Blocks callers
   * 1) if the managed SDK is being installed concurrently by others; and 2) until the installation
   * is complete.
   *
   * @param consoleStream stream to which the install output is written
   * @param monitor the progress monitor that can also be used to cancel the installation
   */
  public IStatus installManagedSdk(MessageConsoleStream consoleStream, IProgressMonitor monitor) {
    if (CloudSdkPreferences.isAutoManaging()) {
      // We don't check if the Cloud SDK installed but always schedule the install job; such check
      // may pass while the SDK is being installed and in an incomplete state.
      // Mark installation failure as non-ERROR to avoid job failure reporting dialogs from the
      // overly helpful Eclipse UI ProgressManager
      CloudSdkInstallJob installJob = new CloudSdkInstallJob(
          consoleStream, modifyLock, IStatus.WARNING);

      IStatus result = runInstallJob(consoleStream, installJob, monitor);
      if (!result.isOK()) {
        // recast result as an IStatus.ERROR
        return new Status(
            IStatus.ERROR,
            result.getPlugin(),
            result.getCode(),
            result.getMessage(),
            result.getException());
      }
    }
    return Status.OK_STATUS;
  }

  /**
   * Triggers the update of a managed Cloud SDK, if the preferences are configured to auto-manage
   * the SDK.
   */
  public void updateManagedSdkAsync() {
    if (CloudSdkPreferences.isAutoManaging()) {
      // Keep installation failure as ERROR so that failures are reported
      Job updateJob = new CloudSdkUpdateJob(null /* create new message console */, modifyLock);
      updateJob.setUser(false);
      updateJob.schedule();
    }
  }

  @VisibleForTesting
  static IStatus runInstallJob(
      MessageConsoleStream consoleStream,
      CloudSdkModifyJob installJob,
      IProgressMonitor cancelMonitor) {
    installJob.schedule();

    try {
      Job.getJobManager().join(CloudSdkInstallJob.CLOUD_SDK_MODIFY_JOB_FAMILY, cancelMonitor);
      if (!installJob.join(0, cancelMonitor)) {
        return Status.CANCEL_STATUS;
      }
      return installJob.getResult();
    } catch (OperationCanceledException | InterruptedException e) {
      installJob.cancel();
      // Could wait to verify job termination, but doesn't seem necessary.
      return Status.CANCEL_STATUS;
    }
  }

  // TODO(chanseok): remove after CloudSdk.validateJdk() becomes public (code duplicate).
  public static void validateJdk(CloudSdk sdk) throws InvalidJavaSdkException {
    boolean isWindows = System.getProperty("os.name").contains("Windows");
    Path javaHome = sdk.getJavaHomePath();
    Path executable = javaHome.toAbsolutePath().resolve(isWindows ? "bin/java.exe" : "bin/java");
    if (!Files.exists(executable)) {
      throw new InvalidJavaSdkException(
          "Invalid Java SDK. " + executable.toString() + " does not exist.");
    }
  }
}
