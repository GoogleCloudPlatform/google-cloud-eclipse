package com.google.cloud.tools.eclipse.sdk.ui;

import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.progress.WorkbenchJob;

/** A simple startup task that triggers installation of the Google Cloud SDK if applicable. */
public class InstallManagedCloudSdkStartup implements IStartup {

  @Override
  public void earlyStartup() {
    // Use a WorkbenchJob in case the user decides to exit quickly
    Job triggerInstallationJob =
        new WorkbenchJob("Check Google Cloud SDK") {
          @Override
          public IStatus runInUIThread(IProgressMonitor monitor) {
            CloudSdkManager.installManagedSdkAsync();
            return Status.OK_STATUS;
          }
        };
    triggerInstallationJob.setSystem(true);
    triggerInstallationJob.schedule(200 /*ms*/); // small wait in case user quits
  }
}
