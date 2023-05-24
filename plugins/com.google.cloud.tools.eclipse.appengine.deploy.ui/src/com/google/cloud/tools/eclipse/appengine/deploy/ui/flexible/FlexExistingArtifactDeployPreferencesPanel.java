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

import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexExistingArtifactDeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.internal.DeployArtifactValidator;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class FlexExistingArtifactDeployPreferencesPanel extends FlexDeployPreferencesPanel {

  public FlexExistingArtifactDeployPreferencesPanel(Composite parent,
      Runnable layoutChangedHandler, boolean requireValues,
      ProjectRepository projectRepository) {
    super(parent, null /*project*/, layoutChangedHandler, requireValues,
        projectRepository, new FlexExistingArtifactDeployPreferences());
  }

  @Override
  protected void createCenterArea() {
    Text deployArtifactField = createBrowseFileRow(
        Messages.getString("deploy.preferences.dialog.label.deploy.artifact"),
        Messages.getString("tooltip.deploy.artifact"),
        getWorkingDirectory(), new String[] {"*.war;*.jar"});
    setupPossiblyUnvalidatedTextFieldDataBinding(deployArtifactField, "deployArtifactPath",
        new DeployArtifactValidator(getWorkingDirectory(), deployArtifactField));

    super.createCenterArea();
  }

  @Override
  protected IPath getWorkingDirectory() {
    return ResourcesPlugin.getWorkspace().getRoot().getLocation();
  }
}
