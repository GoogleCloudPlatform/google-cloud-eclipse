package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;

@RunWith(MockitoJUnitRunner.class)
public class StandardProjectStagingTest {

  @Mock private IPath warDirectory;
  @Mock private IPath stagingDirectory;
  @Mock private CloudSdk cloudSdk;
  @Mock private IProgressMonitor monitor;

  @Test(expected = OperationCanceledException.class)
  public void testStage_cancelled() {
    when(monitor.isCanceled()).thenReturn(true);
    new StandardProjectStaging().stage(warDirectory, stagingDirectory, cloudSdk, monitor);
  }

}
