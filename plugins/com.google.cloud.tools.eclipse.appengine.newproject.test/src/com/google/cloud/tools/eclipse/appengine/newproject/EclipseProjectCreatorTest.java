package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EclipseProjectCreatorTest {

  @Mock IProject project;
  @Mock IAdaptable adapter;
  
  @Before
  public void setUp() {
    Mockito.when(project.getName()).thenReturn("MockProject");
  }
  
  @Test
  public void testMakeNewProject() {
    AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
    config.setEclipseProjectName("foo");
    config.setProject(project);
    
    IRunnableWithProgress runnable = EclipseProjectCreator.makeNewProject(config, adapter);
    Assert.assertNotNull(runnable);
  }
  
}
