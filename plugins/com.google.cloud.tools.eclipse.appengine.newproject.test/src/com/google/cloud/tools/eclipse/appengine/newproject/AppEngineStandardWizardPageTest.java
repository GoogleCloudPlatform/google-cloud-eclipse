package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineStandardWizardPageTest {

  private AppEngineStandardWizardPage page = new AppEngineStandardWizardPage("foo");
  
  @Test
  public void testPageInitiallyIncomplete() {
    Assert.assertFalse(page.isPageComplete());
  }
  
}
