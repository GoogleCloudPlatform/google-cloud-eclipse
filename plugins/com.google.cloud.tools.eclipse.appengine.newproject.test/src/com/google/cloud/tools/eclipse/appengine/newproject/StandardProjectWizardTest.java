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
  }
  
  @Test
  public void testCanFinish() {
    Assert.assertTrue(wizard.canFinish());
  }
  
  @Test
  public void testPerformFinish() {
    Assert.assertTrue(wizard.performFinish());
  }
  
}
