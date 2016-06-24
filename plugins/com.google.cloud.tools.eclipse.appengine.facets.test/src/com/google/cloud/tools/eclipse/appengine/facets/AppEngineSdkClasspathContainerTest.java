package com.google.cloud.tools.eclipse.appengine.facets;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Test;

public class AppEngineSdkClasspathContainerTest {

  @Test
  public void testGetPath() {
    IPath path = new AppEngineSdkClasspathContainer().getPath();
    assertEquals(1, path.segmentCount());
    assertEquals("AppEngineSDK", path.segment(0));
  }

  @Test
  public void testGetKind() {
    assertEquals(IClasspathEntry.CPE_CONTAINER, 
        new AppEngineSdkClasspathContainer().getKind());
  }

  @Test
  public void testGetDescription() {
    assertEquals("App Engine SDKs", 
        new AppEngineSdkClasspathContainer().getDescription());
  }

  @Test
  public void testGetClasspathEntries() {
    // TODO fill in after 
    // https://github.com/GoogleCloudPlatform/app-tools-lib-for-java/issues/149 is fixed
  }

}
