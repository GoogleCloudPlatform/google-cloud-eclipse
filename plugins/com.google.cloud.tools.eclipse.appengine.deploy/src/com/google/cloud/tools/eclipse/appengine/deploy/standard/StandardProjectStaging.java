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
