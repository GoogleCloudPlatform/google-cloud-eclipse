package com.google.cloud.tools.eclipse.appengine.newproject;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineStandardProjectConfigTest {

  private AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();

  @Test
  public void testProjectId() {
    config.setAppEngineProjectId("playbook");
    Assert.assertEquals("playbook", config.getAppEngineProjectId());
  }
  
  @Test
  public void testEclipseProjectName() {
    config.setEclipseProjectName("My Cool App Engine Project");
    Assert.assertEquals("My Cool App Engine Project", config.getEclipseProjectName());
  }

  @Test
  public void testEclipseProjectDirectory() {
    config.setEclipseProjectName("/foo/bar");
    Assert.assertEquals("/foo/bar", config.getEclipseProjectName());
  }
  
  @Test
  public void testEclipseProjectLocationUri() throws URISyntaxException {
    config.setEclipseProjectLocationUri(new URI("file://foo/bar"));
    Assert.assertEquals(new URI("file://foo/bar"), config.getEclipseProjectLocationUri());
  }
  
  @Test
  public void testPackageName() {
    config.setPackageName("com.foo.bar");
    Assert.assertEquals("com.foo.bar", config.getPackageName());
  }

}
