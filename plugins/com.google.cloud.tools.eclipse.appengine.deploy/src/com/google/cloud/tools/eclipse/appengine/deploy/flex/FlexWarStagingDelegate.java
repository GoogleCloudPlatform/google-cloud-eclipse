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

import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.WarPublisher;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Copies a WAR (an App Engine flexible app) to the given staging directory (in addition to copying
 * {@code app.yaml} handled by the base class) for projects with the App Engine flexible WAR facet
 * (which includes WTP). The stager utilizes WTP to publish the WAR.
 *
 * See the Javadoc of {@link FlexStagingDelegate} for additional details.
 *
 * @see FlexStagingDelegate
 * @see StagingDelegate
 */
public class FlexWarStagingDelegate extends FlexStagingDelegate {

  private final IProject project;

  public FlexWarStagingDelegate(IProject project, IPath appEngineDirectory) {
    super(appEngineDirectory);
    this.project = Preconditions.checkNotNull(project);
  }

  @Override
  protected IPath getDeployArtifact(IPath safeWorkDirectory, IProgressMonitor monitor)
      throws CoreException {
    IPath war = safeWorkDirectory.append("app-to-deploy.war");
    IPath tempDirectory = safeWorkDirectory.append("temp");
    IStatus[] statuses = WarPublisher.publishWar(project, war, tempDirectory, monitor);
    if (statuses.length != 0) {
      MultiStatus multiStatus = StatusUtil.multi(this, "problem publishing WAR", statuses);
      if (!multiStatus.isOK()) {
        throw new CoreException(multiStatus);
      }
    }
    return war;
  }

  @Override
  public ISchedulingRule getSchedulingRule() {
    return project;
  }
}
