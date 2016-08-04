package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExplodedWarPublisherTest {

  @Mock IProgressMonitor monitor;
  
  @Test(expected = NullPointerException.class)
  public void testWriteProjectToStageDir_nullProject() throws CoreException {
    new ExplodedWarPublisher().publish(null, null, monitor);
  }

  @Test(expected = NullPointerException.class)
  public void testWriteProjectToStageDir_nullStagingDir() throws CoreException {
    new ExplodedWarPublisher().publish(mock(IProject.class), null, monitor);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testWriteProjectToStageDir_emptyStagingDir() throws CoreException {
    new ExplodedWarPublisher().publish(mock(IProject.class), new Path(""), monitor);
  }
  
  @Test(expected = OperationCanceledException.class)
  public void testWriteProjectToStageDir_cancelled() throws CoreException {
    when(monitor.isCanceled()).thenReturn(true);
    new ExplodedWarPublisher().publish(mock(IProject.class), new Path(""), monitor);
  }
}
