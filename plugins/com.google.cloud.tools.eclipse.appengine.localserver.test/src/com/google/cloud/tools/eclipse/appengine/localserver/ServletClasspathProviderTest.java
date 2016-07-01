package com.google.cloud.tools.eclipse.appengine.localserver;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

// Must Run as JUnit Plug-in Test in Eclipse.
public class ServletClasspathProviderTest {

  private ServletClasspathProvider provider = new ServletClasspathProvider();
  
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
