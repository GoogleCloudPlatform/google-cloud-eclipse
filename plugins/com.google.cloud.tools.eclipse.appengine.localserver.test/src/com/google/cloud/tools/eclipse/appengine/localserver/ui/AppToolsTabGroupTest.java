package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.junit.Assert;
import org.junit.Test;

public class AppToolsTabGroupTest {
  
  @Test
  public void testCreateTabs() {
    AppToolsTabGroup group = new AppToolsTabGroup();
    group.createTabs(null, "");
    Assert.assertEquals(0, group.getTabs().length);
  }

}
