/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerDelegate;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.ServerLaunchConfigurationTab;

public class AppEngineTabGroup extends AbstractLaunchConfigurationTabGroup {

  private static final String[] SERVER_TYPE_IDS = {LocalAppEngineServerDelegate.SERVER_TYPE_ID};

  @Override
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[2];
    tabs[0] = new AppEngineServerLaunchConfigurationTab(SERVER_TYPE_IDS);
    tabs[0].setLaunchConfigurationDialog(dialog);
    tabs[1] = new EnvironmentTab();
    tabs[1].setLaunchConfigurationDialog(dialog);

    // see
    // http://git.eclipse.org/c/jetty/org.eclipse.jetty.wtp.git/tree/org.eclipse.jst.server.jetty.ui/src/org/eclipse/jst/server/jetty/ui/internal/JettyLaunchConfigurationTabGroup.java
    // for examples of other tabs we might want to add

    setTabs(tabs);
  }

  /**
   * To call {@code AbstractLaunchConfigurationTab#scheduleUpdateJob()} as a workaround for
   * https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/481
   */
  private static class AppEngineServerLaunchConfigurationTab extends ServerLaunchConfigurationTab {

    private AppEngineServerLaunchConfigurationTab(String[] serverTypeIds) {
      super(serverTypeIds);
    }

    @Override
    public void createControl(Composite parent) {
      super.createControl(parent);
      scheduleUpdateJob();
    }
  }
}
