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
import com.google.cloud.tools.eclipse.util.jobs.MutexRule;
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
import com.google.cloud.tools.managedcloudsdk.install.UnknownArchiveTypeException;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsoleStream;

public class CloudSdkInstallJob extends Job {

  public static final Object CLOUD_SDK_MODIFY_JOB_FAMILY = new Object();

  /** Scheduling rule to prevent running {@code CloudSdkInstallJob} concurrently. */
  @VisibleForTesting
  static final MutexRule MUTEX_RULE = new MutexRule("for " + CloudSdkInstallJob.class);

  private final MessageConsoleStream consoleStream;

  public CloudSdkInstallJob(MessageConsoleStream consoleStream) {
    super(Messages.getString("installJobName"));
    this.consoleStream = consoleStream;
    setRule(MUTEX_RULE);
  }

  @Override
  public boolean belongsTo(Object family) {
    return super.belongsTo(family) || family == CLOUD_SDK_MODIFY_JOB_FAMILY;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }

    try {
      ManagedCloudSdk managedSdk = getManagedCloudSdk();
      if (!managedSdk.isInstalled()) {
        SdkInstaller installer = managedSdk.newInstaller();
        installer.install(new MessageConsoleWriterListener(consoleStream));
      }

      if (!managedSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)) {
        SdkComponentInstaller componentInstaller = managedSdk.newComponentInstaller();
        componentInstaller.installComponent(
            SdkComponent.APP_ENGINE_JAVA, new MessageConsoleWriterListener(consoleStream));
      }
      return Status.OK_STATUS;

    } catch (InterruptedException | ClosedByInterruptException e) {
      return Status.CANCEL_STATUS;
    } catch (IOException | ManagedSdkVerificationException | SdkInstallerException |
        CommandExecutionException | CommandExitException e) {
      return StatusUtil.error(this, "Failed to install the Google Cloud SDK.", e);
    } catch (UnsupportedOsException e) {
      return StatusUtil.error(
          this, "Google Cloud SDK installation only supported on Windows, Linux, and MacOS.");
      
    } catch (ManagedSdkVersionMismatchException e) {
      throw new IllegalStateException("This is never thrown because we always use LATEST.", e);
    } catch (UnknownArchiveTypeException e) {
      throw new IllegalStateException(
          "The next appengine-plugins-core release will remove this.", e);
    }
  }

  @VisibleForTesting
  protected ManagedCloudSdk getManagedCloudSdk() throws UnsupportedOsException {
    return ManagedCloudSdk.newManagedSdk();
  }

  @Override
  protected void canceling() {
    // By the design of the appengine-plugins-core SDK downloader, cancellation support is
    // implemented through the Java thread interruption facility.
    Thread jobThread = getThread();
    if (jobThread != null) {
      jobThread.interrupt();
    }
  }
}
