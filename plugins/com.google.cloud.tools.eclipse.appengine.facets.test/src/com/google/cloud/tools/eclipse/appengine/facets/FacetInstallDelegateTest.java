package com.google.cloud.tools.eclipse.appengine.facets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.mockito.Mockito;

public class FacetInstallDelegateTest {

  private FacetInstallDelegate delegate = new FacetInstallDelegate();
  
  @Test
  public void testMavenNature() throws CoreException {
    IProject project = Mockito.mock(IProject.class);
    Mockito.when(project.hasNature("org.eclipse.m2e.core.maven2Nature")).thenReturn(true);
    Mockito.when(project.isAccessible()).thenReturn(true);
    
    delegate.execute(project, null, null, null);
  }

}
