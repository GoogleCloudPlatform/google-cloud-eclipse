package com.google.cloud.tools.eclipse.appengine.newproject;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Test;

public class AppEngineStandardProjectConfigTest {

  private AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
  
  public void testProject() throws CoreException {
    try {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IProject project = workspace.getRoot().getProject("foobar");
      config.setProject(project);
      Assert.assertTrue(project.getRawLocationURI().getPath().endsWith("foobar"));
    } finally {
      config.getProject().delete(true, new NullProgressMonitor());
    }
  }

  @Test
  public void testProjectId() {
    config.setAppEngineProjectId("playbook");
    Assert.assertEquals("playbook", config.getAppEngineProjectId());
  }
  
  @Test
  public void testPackageName() {
    config.setPackageName("com.foo.bar");
    Assert.assertEquals("com.foo.bar", config.getPackageName());
  }
  
  @Test
  public void testEclipseProjectLocationUri() throws URISyntaxException {   
    config.setEclipseProjectLocationUri(new URI("file://foo/bar"));   
    Assert.assertEquals(new URI("file://foo/bar"), config.getEclipseProjectLocationUri());    
  }

}
