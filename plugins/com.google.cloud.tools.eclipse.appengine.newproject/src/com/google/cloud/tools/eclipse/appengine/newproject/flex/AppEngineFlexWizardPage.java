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

import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineWizardPage;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class AppEngineFlexWizardPage extends AppEngineWizardPage {

  public AppEngineFlexWizardPage() {
    setTitle(Messages.getString("app.engine.flex.project")); //$NON-NLS-1$
    setDescription(Messages.getString("create.app.engine.flex.project")); //$NON-NLS-1$
  }

  @Override
  public void setHelp(Composite container) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
        "com.google.cloud.tools.eclipse.appengine.newproject.NewFlexProjectContext"); //$NON-NLS-1$
  }
  
  @Override
  protected String getSupportedLibrariesGroup() {
    return CloudLibraries.APP_ENGINE_FLEXIBLE_GROUP;
  }

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);

    AnalyticsPingManager.getInstance().sendPingOnShell(getShell(),
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_FLEX);
  }
}
