/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.sdk.ui;

import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A simple startup task that triggers installation of the Google Cloud SDK if applicable.
 *
 * <p>earlyStartup() is called on a non-UI worker thread and runs before the Eclipse Workbench is
 * actually rendered. Since installing the SDK takes some time, we launch a WorkspaceJob to wait
 * until the workbench is actually up and running, especially so we don't slow down startup. Our use
 * of a wait and WorkbenchJob also ensures that our job doesn't start if the user realizes they made
 * a mistake and quickly exits.
 */
public class InstallManagedCloudSdkStartup implements IStartup {

  @Override
  public void earlyStartup() {
    Job triggerInstallationJob =
        new WorkbenchJob("Check Google Cloud SDK") {
          @Override
          public IStatus runInUIThread(IProgressMonitor monitor) {
            CloudSdkManager.installManagedSdkAsync();
            return Status.OK_STATUS;
          }
        };
    triggerInstallationJob.setSystem(true);
    triggerInstallationJob.schedule(2000 /*ms*/);
  }
}
