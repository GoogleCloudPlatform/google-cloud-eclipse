package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineTabGroupTest {
  
  @Test
  public void testCreateTabs() {
    AppEngineTabGroup group = new AppEngineTabGroup();
    group.createTabs(null, "");
    Assert.assertEquals(0, group.getTabs().length);
  }

}
