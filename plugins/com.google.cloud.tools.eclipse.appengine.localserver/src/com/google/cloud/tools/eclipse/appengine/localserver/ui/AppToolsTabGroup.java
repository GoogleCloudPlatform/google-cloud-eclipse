package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class AppToolsTabGroup extends AbstractLaunchConfigurationTabGroup {

  @Override
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {};
    setTabs(tabs);
  }

}
