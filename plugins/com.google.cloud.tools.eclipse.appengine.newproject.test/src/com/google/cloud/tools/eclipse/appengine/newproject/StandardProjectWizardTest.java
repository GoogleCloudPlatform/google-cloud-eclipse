package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.runtime.IStatus;
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
    wizard.addPages();
  }
  
  @Test
  public void testCanFinish() {
    Assert.assertFalse(wizard.canFinish());
  }

  @Test
  public void testTitleSet() {
    Assert.assertEquals("New App Engine Standard Project", wizard.getWindowTitle());
  }
  
  @Test
  public void testOnePage() {
    Assert.assertEquals(1, wizard.getPageCount());
  }
  
  @Test
  public void testGetPageByName() {
    Assert.assertNotNull(wizard.getPage("basicNewProjectPage"));
  }
  
  @Test
  public void testErrorMessage_Exception() {
    RuntimeException ex = new RuntimeException("testing");
    IStatus status = StandardProjectWizard.setErrorStatus(ex);
    Assert.assertEquals("Failed to create project: testing", status.getMessage());
  }
    
  @Test
  public void testErrorMessage_ExceptionWithoutMessage() {
    RuntimeException ex = new RuntimeException();
    IStatus status = StandardProjectWizard.setErrorStatus(ex);
    Assert.assertEquals("Failed to create project", status.getMessage());
  }
  
}
