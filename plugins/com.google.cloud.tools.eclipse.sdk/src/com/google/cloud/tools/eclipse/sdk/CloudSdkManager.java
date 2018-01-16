/*
 * Copyright 2017 Google Inc.
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
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkInstallJob;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import java.nio.file.Path;
import org.eclipse.core.runtime.jobs.Job;

public class CloudSdkManager {

  /**
   * Returns {@code CloudSdk} if there exists an up-to-date Cloud SDK with the App Engine Java
   * component installed at the managed location. Otherwise, throws {@code
   * CloudSdkNotReadyException} after triggering a background installation task. Callers who get the
   * exception may need to try this method at later points.
   *
   * Note that even if the Cloud SDK is already installed, this method executes {@code gcloud} twice
   * to check components and versions, which may incur a noticeable delay. If possible, avoid
   * unnecessary repetitive calls.
   *
   * @throws CloudSdkNotReadyException if an up-to-date Cloud SDK with the App Engine Java component
   *     is not yet installed at the managed location
   * @throws ManagedSdkVerificationException if unable to verify SDK installation; possible causes
   *     include corrupted installation, a problem executing binaries, or {@code gcloud} execution
   *     failing with an error
   */
  public static CloudSdk getCloudSdk()
      throws CloudSdkNotReadyException, ManagedSdkVerificationException {
    try {
      ManagedCloudSdk managedSdk = ManagedCloudSdk.newManagedSdk();
      if (!managedSdk.isInstalled()
          || !managedSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)
          || !managedSdk.isUpToDate()) {
        scheduleInstallJob(managedSdk);
        throw new CloudSdkNotReadyException();
      }

      Path sdkHome = managedSdk.getSdkHome();
      CloudSdk sdk = new CloudSdk.Builder().sdkPath(sdkHome).build();
      return sdk;

    } catch (UnsupportedOsException e) {
      throw new RuntimeException("Cloud Tools for Eclipse supports Windows, Linux, and Mac only.");
    } catch (ManagedSdkVersionMismatchException e) {
      throw new RuntimeException("This is never thrown because we always use LATEST.");
    }
  }

  private static void scheduleInstallJob(ManagedCloudSdk managedSdk) {
    Job[] jobs = Job.getJobManager().find(CloudSdkInstallJob.JOB_FAMILY);
    if (jobs.length == 0) {  // Just a best effort; this cannot prevent concurrent scheduling.
      new CloudSdkInstallJob(managedSdk).schedule();
    }
  }
}
