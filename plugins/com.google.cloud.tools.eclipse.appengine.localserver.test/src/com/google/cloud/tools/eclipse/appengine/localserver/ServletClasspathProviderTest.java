package com.google.cloud.tools.eclipse.appengine.localserver;

import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Paths;

@RunWith(MockitoJUnitRunner.class)
public class ServletClasspathProviderTest {

  private ServletClasspathProvider provider = new ServletClasspathProvider();
  @Mock private CloudSdk cloudSdk;
  
  @Before
  public void setUp() {
    when(cloudSdk.getJarPath("servlet-api.jar")).thenReturn(Paths.get("/path/to/servlet-api.jar"));
    when(cloudSdk.getJarPath("jsp-api.jar")).thenReturn(Paths.get("/path/to/jsp-api.jar"));
    provider.setCloudSdk(cloudSdk);
  }

  @Test
  public void testResolveClasspathContainer() {
    IClasspathEntry[] result = provider.resolveClasspathContainer(null, null);
    Assert.assertTrue(result[0].getPath().toString().endsWith("servlet-api.jar"));
    Assert.assertTrue(result[1].getPath().toString().endsWith("jsp-api.jar"));
  }

  @Test
  public void testResolveClasspathContainer_mavenProject() throws CoreException {
    IProject project = Mockito.mock(IProject.class);
    Mockito.when(project.hasNature("org.eclipse.m2e.core.maven2Nature")).thenReturn(true);
    Mockito.when(project.isAccessible()).thenReturn(true);
    IClasspathEntry[] result = provider.resolveClasspathContainer(project, null);
    Assert.assertEquals(0, result.length);
  }

}
