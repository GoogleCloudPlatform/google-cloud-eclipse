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

import com.google.cloud.tools.eclipse.appengine.deploy.DeployEnvironmentDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployEnvironmentDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployCommandHandler;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployPreferencesDialog;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Shell;

public class FlexDeployCommandHandler extends DeployCommandHandler {

  @Override
  protected DeployPreferencesDialog newDeployPreferencesDialog(Shell shell, IProject project,
      IGoogleLoginService loginService, IGoogleApiFactory googleApiFactory) {
    String title = Messages.getString("deploy.preferences.dialog.title.flexible");
    return new FlexDeployPreferencesDialog(shell, title, project, loginService, googleApiFactory);
  }

  @Override
  protected DeployEnvironmentDelegate getDeployEnvironmentDelegate(IProject project,
      DeployPreferences preferences) {
    // TODO(chanseok): appEngineDirectory should come from "preferences".
    //
    String appEngineDirectory = FlexDeployPreferences.DEFAULT.getAppEngineDirectory();
    IPath appEngineDirectoryPath = project.getFolder("src/main/appengine").getLocation();
    return new FlexDeployEnvironmentDelegate(appEngineDirectoryPath);
  }

}
