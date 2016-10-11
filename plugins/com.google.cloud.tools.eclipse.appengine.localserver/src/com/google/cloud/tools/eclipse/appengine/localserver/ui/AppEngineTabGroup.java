package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.wst.server.ui.ServerLaunchConfigurationTab;

public class AppEngineTabGroup extends AbstractLaunchConfigurationTabGroup {

  private static final String[] SERVER_TYPE_IDS = new String[]{
      "com.google.cloud.tools.eclipse.appengine.standard.server"
  };

  @Override
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[2];
    tabs[0] = new ServerLaunchConfigurationTab(SERVER_TYPE_IDS);
    tabs[0].setLaunchConfigurationDialog(dialog);
    tabs[1] = new EnvironmentTab();
    tabs[1].setLaunchConfigurationDialog(dialog);

    // see
    // http://git.eclipse.org/c/jetty/org.eclipse.jetty.wtp.git/tree/org.eclipse.jst.server.jetty.ui/src/org/eclipse/jst/server/jetty/ui/internal/JettyLaunchConfigurationTabGroup.java
    // for examples of other tabs we might want to add

    setTabs(tabs);
  }

}
