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
