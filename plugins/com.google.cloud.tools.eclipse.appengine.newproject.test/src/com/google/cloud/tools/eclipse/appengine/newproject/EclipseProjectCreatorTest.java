package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Test;

public class EclipseProjectCreatorTest {

  @Test
  public void testMakeNewProject() {
    AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
    config.setEclipseProjectName("foo");
    Assert.assertTrue(EclipseProjectCreator.makeNewProject(config, null).isOK());
  }
  
}
