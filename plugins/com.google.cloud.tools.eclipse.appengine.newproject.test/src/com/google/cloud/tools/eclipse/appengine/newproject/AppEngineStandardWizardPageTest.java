package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineStandardWizardPageTest {

  private AppEngineStandardWizardPage page = new AppEngineStandardWizardPage();
  
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

}
