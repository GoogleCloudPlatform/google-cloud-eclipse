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
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences;
import com.google.common.annotations.VisibleForTesting;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
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

  /**
   * Installs a Cloud SDK, if the preferences are configured to auto-manage the SDK. Blocks callers
   * 1) if the managed SDK is being installed concurrently by others; and 2) until the installation
   * is complete.
   *
   * @param consoleStream stream to which the install output is written
   */
  public static void installManagedSdk(MessageConsoleStream consoleStream)
      throws CoreException, InterruptedException {
    if (isManagedSdkFeatureEnabled()) {
      if (CloudSdkPreferences.isAutoManaging()) {
        // We don't check if the Cloud SDK installed but always schedule the install job; such check
        // may pass while the SDK is being installed and in an incomplete state.
        runInstallJob(consoleStream, new CloudSdkInstallJob(consoleStream));
      }
    }
  }

  @VisibleForTesting
  static void runInstallJob(MessageConsoleStream consoleStream, CloudSdkInstallJob installJob)
      throws CoreException, InterruptedException {
    installJob.setSystem(true);
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
        Job installJob = new CloudSdkInstallJob(null /* no console output */);
        installJob.schedule();
      }
    }
  }
}
