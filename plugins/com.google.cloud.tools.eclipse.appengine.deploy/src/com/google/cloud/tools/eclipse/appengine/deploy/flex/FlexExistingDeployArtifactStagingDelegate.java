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
import com.google.common.base.Preconditions;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Copies an existing runnable JAR or a WAR (an App Engine flexible app) to the given staging
 * directory (in addition to copying {@code app.yaml} handled by the base class). This stager is,
 * e.g., for the global WAR/JAR deploy dialog, where users directly specify a JAR/WAR to deploy.
 *
 * See the Javadoc of {@link FlexStagingDelegate} for additional details.
 *
 * @see FlexStagingDelegate
 * @see StagingDelegate
 */
public class FlexExistingDeployArtifactStagingDelegate extends FlexStagingDelegate {

  private final IFile deployArtifact;

  public FlexExistingDeployArtifactStagingDelegate(IFile deployArtifact, IPath appEngineDirectory) {
    super(appEngineDirectory);
    this.deployArtifact = Preconditions.checkNotNull(deployArtifact);
  }

  @Override
  protected IPath getDeployArtifact(IPath safeWorkDirectory, IProgressMonitor monitor)
      throws CoreException {
    return deployArtifact.getLocation();
  }

  @Override
  public ISchedulingRule getSchedulingRule() {
    return deployArtifact;
  }
}
