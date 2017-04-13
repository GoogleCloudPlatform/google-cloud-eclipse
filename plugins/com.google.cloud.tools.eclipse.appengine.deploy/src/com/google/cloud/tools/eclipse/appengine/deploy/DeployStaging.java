/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.deploy;

import com.google.cloud.tools.appengine.api.deploy.DefaultStageFlexibleConfiguration;
import com.google.cloud.tools.appengine.api.deploy.DefaultStageStandardConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineFlexibleStaging;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineStandardStaging;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Calls the staging operation on an App Engine project.
 */
public class DeployStaging {

  public static final String STANDARD_STAGING_GENERATED_FILES_DIRECTORY =
      "WEB-INF/appengine-generated";

  /**
   * @param explodedWarDirectory the input of the staging operation
   * @param stagingDirectory where the result of the staging operation will be written
   * @param cloudSdk executes the staging operation
   */
  public static void stageStandard(IPath explodedWarDirectory, IPath stagingDirectory,
      CloudSdk cloudSdk, IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    SubMonitor progress = SubMonitor.convert(monitor, 1);
    progress.setTaskName(Messages.getString("task.name.stage.project")); //$NON-NLS-1$

    DefaultStageStandardConfiguration stagingConfig = new DefaultStageStandardConfiguration();
    stagingConfig.setSourceDirectory(explodedWarDirectory.toFile());
    stagingConfig.setStagingDirectory(stagingDirectory.toFile());
    stagingConfig.setEnableJarSplitting(true);
    stagingConfig.setDisableUpdateCheck(true);

    CloudSdkAppEngineStandardStaging staging = new CloudSdkAppEngineStandardStaging(cloudSdk);
    staging.stageStandard(stagingConfig);

    progress.worked(1);
  }

  /**
   * @param appEngineDirectory directory containing {@code app.yaml}
   * @param deployArtifact project to be deploy (such as WAR or JAR)
   * @param stagingDirectory where the result of the staging operation will be written
   */
  public static void stageFlexible(IPath appEngineDirectory, IPath deployArtifact,
      IPath stagingDirectory, IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    SubMonitor progress = SubMonitor.convert(monitor, 1);
    progress.setTaskName(Messages.getString("task.name.stage.project")); //$NON-NLS-1$

    DefaultStageFlexibleConfiguration stagingConfig = new DefaultStageFlexibleConfiguration();
    stagingConfig.setAppEngineDirectory(appEngineDirectory.toFile());
    stagingConfig.setArtifact(deployArtifact.toFile());
    stagingConfig.setStagingDirectory(stagingDirectory.toFile());

    CloudSdkAppEngineFlexibleStaging staging = new CloudSdkAppEngineFlexibleStaging();
    staging.stageFlexible(stagingConfig);

    progress.worked(1);
  }
}
