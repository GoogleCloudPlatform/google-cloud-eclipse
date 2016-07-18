package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import static org.mockito.Mockito.mock;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.junit.Test;

public class ExplodedWarPublisherTest {

  @Test(expected = NullPointerException.class)
  public void testWriteProjectToStageDir_nullProject() throws CoreException {
    new ExplodedWarPublisher().publish(null, null, null);
  }

  @Test(expected = NullPointerException.class)
  public void testWriteProjectToStageDir_nullStagingDir() throws CoreException {
    new ExplodedWarPublisher().publish(mock(IProject.class), null, null);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testWriteProjectToStageDir_emptyStagingDir() throws CoreException {
    new ExplodedWarPublisher().publish(mock(IProject.class), new Path(""), null);
  }
}
