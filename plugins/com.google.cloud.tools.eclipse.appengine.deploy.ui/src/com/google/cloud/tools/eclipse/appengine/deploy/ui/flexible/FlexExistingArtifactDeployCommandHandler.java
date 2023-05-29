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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible;

import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexExistingArtifactDeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexExistingDeployArtifactStagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployPreferencesDialog;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Shell;

public class FlexExistingArtifactDeployCommandHandler extends FlexDeployCommandHandler {

  @Override
  protected DeployPreferencesDialog newDeployPreferencesDialog(Shell shell, IProject project) {
    String title = Messages.getString("deploy.preferences.dialog.title.flexible");
    return new FlexExistingArtifactDeployPreferencesDialog(shell, title);
  }

  @Override
  protected StagingDelegate getStagingDelegate(IProject project) throws CoreException {
    IPath workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation();

    String appYamlPath = new FlexExistingArtifactDeployPreferences().getAppYamlPath();
    IPath appYaml = resolveFile(appYamlPath, workspaceRoot);
    IPath appEngineDirectory = appYaml.removeLastSegments(1);

    String deployArtifactPath = new FlexExistingArtifactDeployPreferences().getDeployArtifactPath();
    IPath deployArtifact = resolveFile(deployArtifactPath, workspaceRoot);

    return new FlexExistingDeployArtifactStagingDelegate(deployArtifact, appEngineDirectory);
  }

  @Override
  protected DeployPreferences getDeployPreferences(IProject project) {
    return new FlexExistingArtifactDeployPreferences();
  }

  @Override
  protected IProject getSelectedProject(ExecutionEvent event)
      throws ExecutionException, CoreException {
    return null;
  }
}
