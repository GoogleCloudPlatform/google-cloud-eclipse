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

package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkVersionFileException;
import com.google.cloud.tools.eclipse.sdk.MessageConsoleWriterListener;
import com.google.cloud.tools.eclipse.sdk.Messages;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExitException;
import com.google.cloud.tools.managedcloudsdk.components.SdkUpdater;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.console.MessageConsoleStream;

/** Updates the Managed Google Cloud SDK, if installed. */
public class CloudSdkUpdateJob extends CloudSdkModifyJob {
  private static final Logger logger = Logger.getLogger(CloudSdkUpdateJob.class.getName());

  public CloudSdkUpdateJob(MessageConsoleStream consoleStream, ReadWriteLock cloudSdkLock) {
    super(consoleStream, cloudSdkLock);
  }

  /**
   * Perform the installation and configuration of the managed Cloud SDK. Any errors are returned as
   * {@link IStatus#WARNING} to avoid the Eclipse UI ProgressManager reporting the error with no
   * context (e.g., that deployment fails as the Cloud SDK could not be installed).
   */
  @Override
  protected IStatus modifySdk(IProgressMonitor monitor) {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 
        Messages.getString("configuring.cloud.sdk"), 10); //$NON-NLS-1$
    
    try {
      ManagedCloudSdk managedSdk = getManagedCloudSdk();
      if (!managedSdk.isInstalled()) {
        AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.CLOUD_SDK_UPDATE_FAILURE,
            AnalyticsEvents.CLOUD_SDK_FAILURE_CAUSE, "sdk_not_installed"); //$NON-NLS-1$
        logger.info("Google Cloud SDK is not installed"); //$NON-NLS-1$
        return StatusUtil.create(getFailureSeverity(),
            this, Messages.getString("cloud.sdk.not.installed")); //$NON-NLS-1$
      } else if (!managedSdk.isUpToDate()) {
        subTask(subMonitor, Messages.getString("updating.cloud.sdk")); //$NON-NLS-1$
        String oldVersion = getVersion(managedSdk.getSdkHome());
        SdkUpdater updater = managedSdk.newUpdater();
        updater.update(new ProgressWrapper(subMonitor.split(10)),
            new MessageConsoleWriterListener(consoleStream));
        String newVersion = getVersion(managedSdk.getSdkHome());
        logger.info(
            "Managed Google Cloud SDK updated from " //$NON-NLS-1$
                + oldVersion
                + " to " //$NON-NLS-1$
                + newVersion);
        AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.CLOUD_SDK_UPDATE_SUCCESS);
      } else {
        logger.info(
            "Managed Google Cloud SDK remains at version " + getVersion(managedSdk.getSdkHome()));
      }
      return Status.OK_STATUS;

    } catch (InterruptedException ex) {
      AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.CLOUD_SDK_UPDATE_CANCELED);
      return Status.CANCEL_STATUS;
    } catch (ManagedSdkVerificationException | CommandExecutionException | CommandExitException ex) {
      AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.CLOUD_SDK_UPDATE_FAILURE,
          AnalyticsEvents.CLOUD_SDK_FAILURE_CAUSE, ex.getClass().getName());
      logger.log(Level.WARNING, "Could not update Cloud SDK", ex); //$NON-NLS-1$
      String message = Messages.getString("installing.cloud.sdk.failed"); //$NON-NLS-1$
      return StatusUtil.create(getFailureSeverity(), this, message, ex);
    } catch (UnsupportedOsException ex) {
      AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.CLOUD_SDK_UPDATE_FAILURE,
          AnalyticsEvents.CLOUD_SDK_FAILURE_CAUSE, ex.getClass().getName());
      logger.log(Level.WARNING, "Could not update Cloud SDK", ex); // $NON-NLS-1$
      String message = Messages.getString("unsupported.os.installation"); //$NON-NLS-1$
      return StatusUtil.create(getFailureSeverity(), this, message, ex);
    } catch (CloudSdkVersionFileException | CloudSdkNotFoundException ex) {
      AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.CLOUD_SDK_UPDATE_FAILURE,
          AnalyticsEvents.CLOUD_SDK_FAILURE_CAUSE, ex.getClass().getName());
      logger.log(Level.WARNING, "Cloud SDK not found where expected", ex); // $NON-NLS-1$
      String message = Messages.getString("corrupt.cloud.sdk"); //$NON-NLS-1$
      return StatusUtil.create(getFailureSeverity(), this, message, ex);
    } catch (ManagedSdkVersionMismatchException ex) {
      throw new IllegalStateException(
          "This is never thrown because we always use LATEST.", ex); //$NON-NLS-1$
    }
  }
}
