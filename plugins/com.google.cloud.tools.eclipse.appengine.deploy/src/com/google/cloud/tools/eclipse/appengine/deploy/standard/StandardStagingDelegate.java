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

package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.CloudSdkStagingHelper;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.cloud.tools.eclipse.appengine.deploy.WarPublisher;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

public class StandardStagingDelegate implements StagingDelegate {

  private IPath optionalConfigurationFilesDirectory;

  @Override
  public IStatus stage(IProject project, IPath stagingDirectory, IPath safeWorkDirectory,
      CloudSdk cloudSdk, IProgressMonitor monitor) {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

    try {
      WarPublisher.publishExploded(project, safeWorkDirectory, subMonitor.newChild(40));
      CloudSdkStagingHelper.stageStandard(
          safeWorkDirectory, stagingDirectory, cloudSdk, subMonitor.newChild(60));

      optionalConfigurationFilesDirectory =
          stagingDirectory.append(CloudSdkStagingHelper.STANDARD_STAGING_GENERATED_FILES_DIRECTORY);
      return Status.OK_STATUS;
    } catch (AppEngineException ex) {
      return StatusUtil.error(this, Messages.getString("deploy.job.staging.failed"), ex);
    } catch (CoreException ex) {
      return StatusUtil.error(this, Messages.getString("war.publishing.failed"), ex);
    } catch (OperationCanceledException ex) {
      return Status.CANCEL_STATUS;
    }
  }

  @Override
  public IPath getOptionalConfigurationFilesDirectory() {
    return optionalConfigurationFilesDirectory;
  }

}
