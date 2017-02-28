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
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectWizard;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineWizardPage;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

// TODO: update functions
public class AppEngineFlexProjectWizard extends AppEngineProjectWizard {

  @Override
  public AppEngineWizardPage getWizardPage() {
    return new AppEngineFlexWizardPage();
  }

  @Override
  public void sendAnalyticsPing() {
    // TODO: send anayltics
  }

  @Override
  public IStatus validateDependencies(boolean fork, boolean cancelable) {
    return Status.OK_STATUS;
  }

  @Override
  public CreateAppEngineWtpProject getAppEngineProjectCreationOperation(AppEngineProjectConfig config,
      IAdaptable uiInfoAdapter) {
    return new CreateAppEngineFlexWtpProject(config, uiInfoAdapter);
  }

}
