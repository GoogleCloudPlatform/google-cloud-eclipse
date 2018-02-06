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
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import java.util.concurrent.Semaphore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public class MockedSdkInstallJob extends CloudSdkInstallJob {

  private final Semaphore blocker = new Semaphore(0);
  private final ManagedCloudSdk managedCloudSdk;
  private final boolean blockBeforeExit;

  public MockedSdkInstallJob(boolean blockBeforeExit, ManagedCloudSdk managedCloudSdk) {
    super(null);
    this.blockBeforeExit = blockBeforeExit;
    this.managedCloudSdk = managedCloudSdk;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IStatus status = super.run(monitor);
    if (blockBeforeExit) {
      blocker.acquireUninterruptibly();
    }
    return status;
  }

  @Override
  protected ManagedCloudSdk getManagedCloudSdk() {
    return managedCloudSdk;
  }

  public void unblock() {
    blocker.release();
  }
}
