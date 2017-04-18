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

package com.google.cloud.tools.eclipse.appengine.deploy.flex;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.appengine.deploy.CloudSdkStagingHelper;
import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.WarPublisher;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

public class FlexStagingDelegate implements StagingDelegate {

  private final IPath appEngineDirectory;

  public FlexStagingDelegate(IPath appEngineDirectory) {
    this.appEngineDirectory = appEngineDirectory;
  }

  @Override
  public IStatus stage(IProject project, IPath stagingDirectory, IPath safeWorkDirectory,
      CloudSdk cloudSdk, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

    stagingDirectory.toFile().mkdirs();
    IPath war = safeWorkDirectory.append("app-to-deploy.war");
    WarPublisher.publishWar(project, war, subMonitor.newChild(40));
    CloudSdkStagingHelper.stageFlexible(appEngineDirectory, war, stagingDirectory,
        subMonitor.newChild(60));

    return Status.OK_STATUS;
  }

  @Override
  public IPath getOptionalConfigurationFilesDirectory() {
    return appEngineDirectory;
  }

}
