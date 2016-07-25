package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import com.google.cloud.tools.appengine.api.deploy.DefaultStageStandardConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineStandardStaging;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;

/**
 * Calls the staging operation on an App Engine Standard project using the {@link CloudSdk}
 */
public class StandardProjectStaging {

  /**
   * @param explodedWarDirectory the input of the staging operation
   * @param stagingDirectory where the result of the staging operation will be written
   * @param cloudSdk executes the staging operation
   */
  public void stage(IPath explodedWarDirectory, IPath stagingDirectory, CloudSdk cloudSdk, IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    SubMonitor progress = SubMonitor.convert(monitor, 1);
    progress.setTaskName(Messages.getString("task.name.stage.project")); //$NON-NLS-1$

    DefaultStageStandardConfiguration stagingConfig = new DefaultStageStandardConfiguration();
    stagingConfig.setSourceDirectory(explodedWarDirectory.toFile());
    stagingConfig.setStagingDirectory(stagingDirectory.toFile());
    stagingConfig.setEnableJarSplitting(true);

    CloudSdkAppEngineStandardStaging staging = new CloudSdkAppEngineStandardStaging(cloudSdk);
    staging.stageStandard(stagingConfig);

    progress.worked(1);
  }
}
