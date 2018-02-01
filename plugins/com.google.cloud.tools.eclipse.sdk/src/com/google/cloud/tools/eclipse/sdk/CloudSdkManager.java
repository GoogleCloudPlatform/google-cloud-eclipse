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

import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkInstallJob;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkModifyJob;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences;
import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class CloudSdkManager {

  private static final String OPTION_MANAGED_CLOUD_SDK =
      "com.google.cloud.tools.eclipse.sdk/enable.managed.cloud.sdk";

  // To be able to write tests for the managed Cloud SDK feature, which is disabled at the moment.
  @VisibleForTesting
  public static boolean forceManagedSdkFeature;

  public static boolean isManagedSdkFeatureEnabled() {
    if (forceManagedSdkFeature) {
      return true;
    }

    BundleContext context = FrameworkUtil.getBundle(CloudSdkManager.class).getBundleContext();
    DebugOptions debugOptions = context.getService(context.getServiceReference(DebugOptions.class));
    if (debugOptions != null) {
      return debugOptions.getBooleanOption(OPTION_MANAGED_CLOUD_SDK, false);
    }
    return false;
  }

  // readers = using SDK, writers = modifying SDK
  @VisibleForTesting
  static final ReadWriteLock modifyLock = new ReentrantReadWriteLock();

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
  public static void preventModifyingSdk() throws InterruptedException {
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
  public static void allowModifyingSdk() {
    modifyLock.readLock().unlock();
  }

  /**
   * Installs the managed Cloud SDK, if the preferences are configured to auto-managed the SDK.
   * Blocks callers 1) if the managed SDK is being installed or updated concurrently by others; and
   * 2) until the installation is complete.
   *
   * @param consoleStream stream to which the install output is written
   */
  public static void installManagedSdk(MessageConsoleStream consoleStream)
      throws CoreException, InterruptedException {
    if (isManagedSdkFeatureEnabled()) {
      if (CloudSdkPreferences.isAutoManaging()) {
        runInstallJob(consoleStream, new CloudSdkInstallJob(consoleStream, modifyLock));
      }
    }
  }

  @VisibleForTesting
  static void runInstallJob(MessageConsoleStream consoleStream, CloudSdkModifyJob installJob)
      throws CoreException, InterruptedException {
    installJob.schedule();
    installJob.join();

    IStatus status = installJob.getResult();
    if (!status.isOK()) {
      throw new CoreException(status);
    }
  }

  public static void installManagedSdkAsync() {
    if (isManagedSdkFeatureEnabled()) {
      if (CloudSdkPreferences.isAutoManaging()) {
        CloudSdkModifyJob installJob =
            new CloudSdkInstallJob(null /* no console output */, modifyLock);
        installJob.schedule();
      }
    }
  }

  public static void updateManagedSdkAsync() {
    if (isManagedSdkFeatureEnabled()) {
      if (CloudSdkPreferences.isAutoManaging()) {
        // TODO(chanseok): to be implemented: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2753
        // CloudSdkUpdateJob udpateJob =
        //    new CloudSdkUpdateJob(null /* no console output */, modifyLock);
        // updateJob.schedule();
      }
    }
  }
}
