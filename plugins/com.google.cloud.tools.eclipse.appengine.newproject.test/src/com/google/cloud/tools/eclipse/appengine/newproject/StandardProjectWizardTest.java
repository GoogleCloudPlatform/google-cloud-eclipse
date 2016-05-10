package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
  public void testErrorMessage_cancel() {
    wizard.showErrorMessageIfNecessary(Status.CANCEL_STATUS);
    Assert.assertEquals("User canceled project creation", 
        wizard.getPage("basicNewProjectPage").getErrorMessage());
  }
  
  @Test
  public void testErrorMessage_OK() {
    wizard.showErrorMessageIfNecessary(Status.OK_STATUS);
    Assert.assertNull(wizard.getPage("basicNewProjectPage").getErrorMessage());
  }
  
  @Test
  public void testErrorMessage_Exception() {
    RuntimeException ex = new RuntimeException("testing");
    IStatus status = new Status(Status.ERROR, "plugin ID", 87, ex.getMessage(), null);
    wizard.showErrorMessageIfNecessary(status);
    Assert.assertEquals("Failed to create project: testing", 
        wizard.getPage("basicNewProjectPage").getErrorMessage());
  }
    
  @Test
  public void testErrorMessage_ExceptionWithoutMessage() {
    RuntimeException ex = new RuntimeException();
    IStatus status = new Status(Status.ERROR, "plugin ID", 87, ex.getMessage(), null);
    wizard.showErrorMessageIfNecessary(status);
    Assert.assertEquals("Failed to create project", 
        wizard.getPage("basicNewProjectPage").getErrorMessage());
  }
  
}
