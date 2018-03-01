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

import com.google.cloud.tools.eclipse.sdk.MessageConsoleWriterListener;
import com.google.cloud.tools.eclipse.sdk.Messages;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExitException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponentInstaller;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstaller;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstallerException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.console.MessageConsoleStream;

public class CloudSdkInstallJob extends CloudSdkModifyJob {
  private static final Logger logger = Logger.getLogger(CloudSdkInstallJob.class.getName());

  public CloudSdkInstallJob(MessageConsoleStream consoleStream, ReadWriteLock cloudSdkLock) {
    super(Messages.getString("installing.cloud.sdk"), consoleStream, cloudSdkLock); // $NON-NLS-1$
  }

  /** The severity reported on installation failure. */
  private int failureSeverity = IStatus.ERROR;

  /**
   * Perform the installation and configuration of the managed Cloud SDK. Any errors are returned as
   * {@link IStatus#WARNING} to avoid the Eclipse UI ProgressManager reporting the error with no
   * context (e.g., that deployment fails as the Cloud SDK could not be installed).
   */
  @Override
  protected IStatus modifySdk(IProgressMonitor monitor) {
    SubMonitor progress =
        SubMonitor.convert(monitor, Messages.getString("configuring.cloud.sdk"), 30); // $NON-NLS-1$
    if (progress.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    if (consoleStream != null) {
      consoleStream.println(Messages.getString("startModifying"));
    }
    try {
      ManagedCloudSdk managedSdk = getManagedCloudSdk();
      if (!managedSdk.isInstalled()) {
        subTask(progress, Messages.getString("installing.cloud.sdk")); // $NON-NLS-1$
        SdkInstaller installer = managedSdk.newInstaller();
        installer.install(
            new ProgressWrapper(progress.split(10)),
            new MessageConsoleWriterListener(consoleStream));
        String version = getVersion(managedSdk.getSdkHome());
        logger.info("Installed Google Cloud SDK version " + version);
      }

      if (!managedSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)) {
        subTask(
            progress, Messages.getString("installing.cloud.sdk.app.engine.java")); // $NON-NLS-1$
        SdkComponentInstaller componentInstaller = managedSdk.newComponentInstaller();
        componentInstaller.installComponent(
            SdkComponent.APP_ENGINE_JAVA,
            new ProgressWrapper(progress.split(10)),
            new MessageConsoleWriterListener(consoleStream));
        logger.info("Installed Google Cloud SDK component: " + SdkComponent.APP_ENGINE_JAVA.name());
      }
      progress.worked(10);

      return Status.OK_STATUS;

    } catch (InterruptedException | ClosedByInterruptException e) {
      return Status.CANCEL_STATUS;
    } catch (IOException | ManagedSdkVerificationException | SdkInstallerException |
        CommandExecutionException | CommandExitException e) {
      logger.log(Level.WARNING, "Could not install Cloud SDK", e);
      String message = Messages.getString("installing.cloud.sdk.failed");
      return StatusUtil.create(failureSeverity, this, message, e); // $NON-NLS-1$
    } catch (UnsupportedOsException e) {
      logger.log(Level.WARNING, "Could not install Cloud SDK", e);
      String message = Messages.getString("unsupported.os.installation");
      return StatusUtil.create(failureSeverity, this, message, e); // $NON-NLS-1$

    } catch (ManagedSdkVersionMismatchException e) {
      throw new IllegalStateException("This is never thrown because we always use LATEST.", e); //$NON-NLS-1$
    }
  }

  /**
   * Set the {@link IStatus#getSeverity() severity} of installation failure. This is useful for
   * situations where the Cloud SDK installation is a step of some other work, and the installation
   * failure should be surfaced to the user in the context of that work. If reported as {@link
   * IStatus#ERROR} then the Eclipse UI ProgressManager will report the installation failure
   * directly.
   */
  public void setFailureSeverity(int severity) {
    failureSeverity = severity;
  }
}
