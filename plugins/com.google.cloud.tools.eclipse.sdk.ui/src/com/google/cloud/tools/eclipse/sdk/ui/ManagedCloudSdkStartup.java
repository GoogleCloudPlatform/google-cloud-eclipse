/*
 * Copyright 2018 Google LLC
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

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkVersionFileException;
import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A simple startup task for triggering events relating to the managed Google Cloud SDK, when
 * enabled. If the Google Cloud SDK is not installed, then we silently trigger installation of the
 * Google Cloud SDK. Otherwise we check if the managed installation is up-to-date and if not, notify
 * the user of the update.
 *
 * <p>earlyStartup() is called on a non-UI worker thread and runs before the Eclipse Workbench is
 * actually rendered. Since installing the SDK takes some time, we launch a WorkspaceJob to wait
 * until the workbench is actually up and running, especially so we don't slow down startup. Our use
 * of a wait and WorkbenchJob also ensures that our job doesn't start if the user realizes they made
 * a mistake and quickly exits.
 */
public class ManagedCloudSdkStartup implements IStartup {
  private static final Logger logger = Logger.getLogger(ManagedCloudSdkStartup.class.getName());

  private final IWorkbench workbench;

  public ManagedCloudSdkStartup() {
    this(PlatformUI.getWorkbench());
  }

  @VisibleForTesting
  ManagedCloudSdkStartup(IWorkbench workbench) {
    this.workbench = workbench;
  }

  @Override
  public void earlyStartup() {
    // TODO: notify the user of updates for manual installs too
    if (!CloudSdkPreferences.isAutoManaging()) {
      return;
    }

    Job checkInstallationJob = new Job(Messages.getString("CheckUpToDateJobTitle")) { //$NON-NLS-1$
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          CloudSdkManager sdkManager = CloudSdkManager.getInstance();
          ManagedCloudSdk installer = ManagedCloudSdk.newManagedSdk();
          checkInstallation(sdkManager, installer, monitor);
        } catch (UnsupportedOsException ex) {
          logger.log(Level.FINE, "Unable to check Cloud SDK installation", ex); //$NON-NLS-1$
        } catch (ManagedSdkVerificationException ex) {
          logger.log(Level.SEVERE, "Unable to check Cloud SDK installation. Possible causes include" //$NON-NLS-1$
              + " network connection problem or corrupt Cloud SDK installation.", ex); //$NON-NLS-1$
        } catch (ManagedSdkVersionMismatchException ex) {
          throw new IllegalStateException("This is never thrown because we always use LATEST.", ex); //$NON-NLS-1$
        }
        return Status.OK_STATUS;
      }
    };

    // Use a WorkbenchJob to trigger the check to ensure we start after the workbench window has
    // appeared, but perform the actual check within a normal Job so that we don't monopolize the
    // display thread.
    Job triggerInstallationJob = new WorkbenchJob(Messages.getString("CheckUpToDateJobTitle")) { //$NON-NLS-1$
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        checkInstallationJob.setSystem(true);
        checkInstallationJob.schedule(); // immediately
        return Status.OK_STATUS;
      }
    };
    triggerInstallationJob.setSystem(true);
    triggerInstallationJob.schedule(2000 /*ms*/);
  }

  @VisibleForTesting
  void checkInstallation(
      CloudSdkManager manager, ManagedCloudSdk installation, IProgressMonitor monitor)
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    if (!installation.isInstalled()) {
      manager.installManagedSdkAsync();
    } else if (!installation.isUpToDate()) {
      CloudSdkVersion currentVersion = getVersion(installation.getSdkHome());
      CloudSdkUpdateNotification.showNotification(
          workbench, currentVersion, manager::updateManagedSdkAsync);
    }
  }

  /**
   * Return the version of the Google Cloud SDK at the given location, or {@code null} if it cannot
   * be determined.
   */
  private static CloudSdkVersion getVersion(Path sdkHome) {
    try {
      CloudSdk sdk = new CloudSdk.Builder().sdkPath(sdkHome).build();
      return sdk.getVersion();
    } catch (CloudSdkVersionFileException | CloudSdkNotFoundException ex) {
      return null;
    }
  }
}
