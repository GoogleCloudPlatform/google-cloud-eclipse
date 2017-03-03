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

package com.google.cloud.tools.eclipse.appengine.newproject.flex;

import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
* Utility to make a new Eclipse project with the App Engine Flexible facets in the workspace.
*/
public class CreateAppEngineFlexWtpProject extends CreateAppEngineWtpProject {

  CreateAppEngineFlexWtpProject(AppEngineProjectConfig config, IAdaptable uiInfoAdapter) {
    super(config, uiInfoAdapter);
  }

  @Override
  public void addAppEngineFacet(IProject newProject, IProgressMonitor monitor) throws CoreException {
    // TODO add flex facet
  }

  @Override
  public String getDescription() {
    return Messages.getString("creating.app.engine.flex.project"); //$NON-NLS-1$
  }

  @Override
  public IFile createProjectFiles(IProject newProject, AppEngineProjectConfig config, IProgressMonitor monitor)
      throws CoreException {
    // TODO materizalize flex files
    return null;
  }

}
