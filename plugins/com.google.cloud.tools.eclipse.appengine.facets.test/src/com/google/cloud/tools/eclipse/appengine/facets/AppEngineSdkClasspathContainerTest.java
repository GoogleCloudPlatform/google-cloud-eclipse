package com.google.cloud.tools.eclipse.appengine.facets;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Test;

public class AppEngineSdkClasspathContainerTest {

  @Test
  public void testGetPath() {
    IPath path = new AppEngineSdkClasspathContainer().getPath();
    assertThat(path.segmentCount(), is(1));
    assertThat(path.segment(0), is("AppEngineSDK"));
  }

  @Test
  public void testGetKind() {
    assertThat(new AppEngineSdkClasspathContainer().getKind(),
               is(IClasspathEntry.CPE_CONTAINER));
  }

  @Test
  public void testGetDescription() {
    assertThat(new AppEngineSdkClasspathContainer().getDescription(),
               is("App Engine SDKs"));
  }

  @Test
  public void testGetClasspathEntries() {
    // TODO fill in after 
    // https://github.com/GoogleCloudPlatform/app-tools-lib-for-java/issues/149 is fixed
  }

}
