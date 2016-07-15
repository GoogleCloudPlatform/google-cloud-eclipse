package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineProjectDeployerTest {

  @Mock IPath stagingDirectory;
  @Mock CloudSdk cloudSdk;
  @Mock IProgressMonitor monitor;
  @Mock AppEngineDeployInfo deployInfo;

  @Test(expected = CoreException.class)
  public void testDeploy_NoAppEngineProjectId() throws CoreException {
    when(deployInfo.getProjectId()).thenReturn(null);
    new AppEngineProjectDeployer(deployInfo).deploy(new Path("/non/existing/path"), cloudSdk, monitor);
  }

  @Test(expected = CoreException.class)
  public void testDeploy_NoAppEngineProjectVersion() throws CoreException {
    when(deployInfo.getProjectId()).thenReturn("fooProjectId");
    when(deployInfo.getProjectVersion()).thenReturn(null);
    new AppEngineProjectDeployer(deployInfo).deploy(new Path("/non/existing/path"), cloudSdk, monitor);
  }
}
