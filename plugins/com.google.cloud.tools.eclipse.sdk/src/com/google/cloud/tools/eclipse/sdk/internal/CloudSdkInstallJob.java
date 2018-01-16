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

package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Wraps {@code CloudSdkInstallTask} in {@code Job} mainly to report install progress to users.
 */
public class CloudSdkInstallJob extends Job {

  public static final String JOB_FAMILY = CloudSdkInstallJob.class.getName();

  /** Scheduling rule to prevent running CloudSdkInstallTask concurrently. */
  private static final MutexRule MUTEX_RULE = new MutexRule();

  private final ManagedCloudSdk managedSdk;

  public CloudSdkInstallJob(ManagedCloudSdk managedSdk) {
    super("Installing the Google Cloud SDK... (may take up to several minutes)");
    this.managedSdk = managedSdk;
    setUser(true);
    setRule(MUTEX_RULE);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    CloudSdkInstallTask installTask = new CloudSdkInstallTask(managedSdk, monitor);
    Future<Void> futureTask = Executors.newSingleThreadExecutor().submit(installTask);

    try {
      while (true) {
        if (monitor.isCanceled()) {
          // By the design of the appengine-plugins-core SDK downloader, cancellation support is
          // implemented through the Java thread interruption facility.
          boolean interruptThread = true;
          futureTask.cancel(interruptThread);
          return Status.CANCEL_STATUS;
        }

        try {
          futureTask.get(1, TimeUnit.SECONDS);
          return Status.OK_STATUS;
        } catch (TimeoutException e) {
          // Install still in progress. Just check completion again.
        }
      }
    } catch (CancellationException | InterruptedException e) {
      return Status.CANCEL_STATUS;
    } catch (ExecutionException e) {
      return StatusUtil.error(this, "Failed to install the Google Cloud SDK.", e);
    }
  }

  @Override
  public boolean belongsTo(Object family) {
    return JOB_FAMILY.equals(family);
  }
}
