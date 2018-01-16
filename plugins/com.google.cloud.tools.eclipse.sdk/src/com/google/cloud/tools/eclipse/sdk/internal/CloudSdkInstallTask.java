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

import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.MessageListener;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExitException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponentInstaller;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstaller;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstallerException;
import com.google.cloud.tools.managedcloudsdk.install.UnknownArchiveTypeException;
import com.google.cloud.tools.managedcloudsdk.update.SdkUpdater;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Implementation is not thread-safe. {@code #call()} should not be executing concurrently.
 */
class CloudSdkInstallTask implements Callable<Void> {

  private final ManagedCloudSdk managedSdk;
  private final IProgressMonitor monitor;

  public CloudSdkInstallTask(ManagedCloudSdk managedSdk, IProgressMonitor monitor) {
    this.managedSdk = managedSdk;
    this.monitor = monitor;
  }

  @Override
  public Void call() throws ManagedSdkVerificationException, IOException, InterruptedException,
      SdkInstallerException, CommandExecutionException, CommandExitException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 50);

    try {
      if (!managedSdk.isInstalled()) {
        subMonitor.setTaskName("Installing the core SDK...");
        SdkInstaller installer = managedSdk.newInstaller();
        installer.install(new NoOpMessageListener());
        subMonitor.worked(10);
      }

      if (!managedSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)) {
        subMonitor.setTaskName("Installing the App Engine Java component...");
        SdkComponentInstaller componentInstaller = managedSdk.newComponentInstaller();
        componentInstaller.installComponent(
            SdkComponent.APP_ENGINE_JAVA, new NoOpMessageListener());
        subMonitor.worked(30);
      }

      if (!managedSdk.isUpToDate()) {
        subMonitor.setTaskName("Updating the SDK...");
        SdkUpdater updater = managedSdk.newUpdater();
        updater.update(new NoOpMessageListener());
        subMonitor.worked(10);
      }

    } catch (UnsupportedOsException e) {
      throw new RuntimeException("Cloud Tools for Eclipse supports Windows, Linux, and Mac only.");
    } catch (ManagedSdkVersionMismatchException e) {
      throw new RuntimeException("This is never thrown because we always use LATEST.");
    } catch (UnknownArchiveTypeException e) {
      throw new RuntimeException("Never thrown.");
    }
    return null;
  }

  private static class NoOpMessageListener implements MessageListener {
    @Override
    public void message(String rawString) {
      // Do nothing.
    }
  }
}
