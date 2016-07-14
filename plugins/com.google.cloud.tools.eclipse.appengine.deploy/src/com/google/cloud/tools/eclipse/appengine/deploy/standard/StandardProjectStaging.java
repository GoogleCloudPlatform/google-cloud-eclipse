package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.google.cloud.tools.appengine.api.deploy.DefaultStageStandardConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineStandardStaging;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;

public class StandardProjectStaging {

  public void stage(IPath explodedWarDir, IPath stagingDir, CloudSdk cloudSdk, IProgressMonitor monitor) {
    SubMonitor progress = SubMonitor.convert(monitor, 1);
    progress.setTaskName(Messages.getString("task.name.stage.project")); //$NON-NLS-1$

    DefaultStageStandardConfiguration stagingConfig = new DefaultStageStandardConfiguration();
    stagingConfig.setSourceDirectory(explodedWarDir.toFile());
    stagingConfig.setStagingDirectory(stagingDir.toFile());
    stagingConfig.setEnableJarSplitting(true);

    CloudSdkAppEngineStandardStaging staging = new CloudSdkAppEngineStandardStaging(cloudSdk);
    staging.stageStandard(stagingConfig);

    progress.worked(1);
  }
}
