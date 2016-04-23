package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Test;

public class StandardProjectWizardTest {

  private StandardProjectWizard wizard = new StandardProjectWizard();

  @Test
  public void testCanFinish() {
    Assert.assertTrue(wizard.canFinish());
  }

  @Test
  public void testPerformFinish() {
    Assert.assertTrue(wizard.performFinish());
  }
  
}
