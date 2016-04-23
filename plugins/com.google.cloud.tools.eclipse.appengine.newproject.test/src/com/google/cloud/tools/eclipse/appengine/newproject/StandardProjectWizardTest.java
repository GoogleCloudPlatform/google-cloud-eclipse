package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.resource.JFaceResources;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class StandardProjectWizardTest {

  private StandardProjectWizard wizard = new StandardProjectWizard();

  @BeforeClass // seems to be needed on Travis to avoid race condition
  public static void loadImageRegistry() {
    JFaceResources.getImageRegistry();
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
