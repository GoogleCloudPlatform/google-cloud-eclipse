package com.google.cloud.tools.eclipse.appengine.localserver.deploy;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkProjectPropertyTesterTest {

  @Mock private IProject mockProject;

  @Test
  public void testTest_nonProjectArgument() {
    assertFalse(new CloudSdkProjectPropertyTester().test(new Object(), "", new Object[0], ""));
  }
  
  @Test
  public void testTest_projectHasNoCloudSdkFacet() {
    assertFalse(new CloudSdkProjectPropertyTester().test(mockProject, "", new Object[0], ""));
  }
  
  @Test
  public void testTest_throwsCoreException() throws CoreException {
    when(mockProject.isAccessible()).thenReturn(true);
    when(mockProject.isNatureEnabled(anyString())).thenThrow(CoreException.class);
    assertFalse(new CloudSdkProjectPropertyTester().test(mockProject, "", new Object[0], ""));
  }
}
