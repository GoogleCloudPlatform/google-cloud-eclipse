package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineStandardWizardPageTest {

  private AppEngineStandardWizardPage page =
      new AppEngineStandardWizardPage();
  
  @Test
  public void testPageInitiallyIncomplete() {
    Assert.assertFalse(page.isPageComplete());
  }
  
  @Test
  public void testGetNextPage() {
    Assert.assertNull(page.getNextPage());
  }
  
  @Test
  public void testGetPreviousPage() {
    Assert.assertNull(page.getPreviousPage());
  }
  
  @Test
  public void testTitle() {
    Assert.assertEquals("App Engine Standard Project", page.getTitle());
  }
  
  @Test
  public void testDescription() {
    Assert.assertEquals("Create a new App Engine Standard Project in the workspace.", 
        page.getDescription());
  }

}
