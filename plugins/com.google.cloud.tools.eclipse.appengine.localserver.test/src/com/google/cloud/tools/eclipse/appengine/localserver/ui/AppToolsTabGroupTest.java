package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AppToolsTabGroupTest {

  @Before 
  public void createWorkbench() {
    if (!PlatformUI.isWorkbenchRunning()) {
      WorkbenchAdvisor workbenchAdvisor = new WorkbenchAdvisor() {
        @Override
        public String getInitialWindowPerspectiveId() {
          return null;
        }
      };
      PlatformUI.createAndRunWorkbench(PlatformUI.createDisplay(), workbenchAdvisor); 
    }
  }
  
  @Test
  public void testCreateTabs() {
    AppToolsTabGroup group = new AppToolsTabGroup();
    group.createTabs(null, "");
    Assert.assertEquals(0, group.getTabs().length);
  }

}
