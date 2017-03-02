package com.google.cloud.tools.eclipse.appengine.libraries.ui;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineLibrariesPageTest {

  private AppEngineLibrariesPage page = new AppEngineLibrariesPage();

  @Test
  public void testConstructor() {
    Assert.assertEquals("App Engine Standard Environment Libraries", page.getTitle());
    Assert.assertNull(page.getMessage());
    Assert.assertNull(page.getErrorMessage());
    Assert.assertEquals(
        "Additional jars commonly used in App Engine Standard Environment applications",
        page.getDescription());
    Assert.assertNotNull(page.getImage());
  }
  
  @Test
  public void testFinish() {
    Assert.assertTrue(page.finish());
  }
  
  
  @Test
  public void testGetSelection() {
    Assert.assertNull(page.getSelection());
  }

}
