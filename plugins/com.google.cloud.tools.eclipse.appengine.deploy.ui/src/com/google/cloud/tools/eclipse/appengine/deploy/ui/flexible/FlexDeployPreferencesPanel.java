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

import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.AppEngineDeployPreferencesPanel;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class FlexDeployPreferencesPanel extends AppEngineDeployPreferencesPanel {

  public FlexDeployPreferencesPanel(Composite parent, IProject project,
      IGoogleLoginService loginService, Runnable layoutChangedHandler, boolean requireValues,
      ProjectRepository projectRepository) {
    super(parent, project, loginService, layoutChangedHandler, requireValues, projectRepository,
        new FlexDeployPreferences(project));
  }

  @Override
  protected void createCenterArea() {
    super.createCenterArea();

    Button includeOptionalConfigurationFilesButton = createCheckBox(
        Messages.getString("deploy.config.files"),
        Messages.getString("tooltip.deploy.config.files.flexible"));
    setupCheckBoxDataBinding(
        includeOptionalConfigurationFilesButton, "includeOptionalConfigurationFiles");
  }

  @Override
  protected String getHelpContextId() {
    return "com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployAppEngineFlexProjectContext";
  }
}
