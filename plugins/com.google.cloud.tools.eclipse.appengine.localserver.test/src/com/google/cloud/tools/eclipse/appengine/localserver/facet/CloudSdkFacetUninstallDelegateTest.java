package com.google.cloud.tools.eclipse.appengine.localserver.facet;

import static org.mockito.Mockito.verifyZeroInteractions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkFacetUninstallDelegateTest {

  @Mock
  private IProject project;
  @Mock
  private IProjectFacetVersion fv;
  @Mock
  private Object config;
  @Mock
  private IProgressMonitor monitor;

  @Test
  public void testExecute_doesNothingWithTheArguments() throws CoreException {
    new CloudSdkFacetUninstallDelegate().execute(project, fv, config, monitor);
    verifyZeroInteractions(project, fv, config, monitor);
  }
}
