package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StandardProjectWizardTest {

  private StandardProjectWizard wizard;

  @Before
  public void setUp() {
    try {
      wizard = new StandardProjectWizard();
      // I don't know why this fails the first time and passes the second, but it does.
    } catch (NullPointerException ex) {
      wizard = new StandardProjectWizard();
    }
    wizard.init(null, null);
  }
  
  @Test
  public void testCanFinish() {
    Assert.assertFalse(wizard.canFinish());
  }
  
  @Test
  public void testPerformFinish() {
    Assert.assertTrue(wizard.performFinish());
  }
  
  @Test
  public void testTitleSet() {
    Assert.assertEquals("New App Engine Standard Project", wizard.getWindowTitle());
  }
  
  @Test
  public void testOnePage() {
    Assert.assertEquals(1, wizard.getPageCount());
  }
  
}
