package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import java.lang.reflect.InvocationTargetException;

public class StandardProjectWizard extends Wizard implements INewWizard {

  private AppEngineStandardWizardPage page;
  private AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
  
  public StandardProjectWizard() {
    this.setWindowTitle("New App Engine Standard Project");
    page = new AppEngineStandardWizardPage();
    setNeedsProgressMonitor(true);
  }
  
  @Override 
  public void addPages() {
    this.addPage(page);
  }

  @Override
  public boolean performFinish() {
    // todo is this the right time/place to grab these?
    config.setAppEngineProjectId(page.getAppEngineProjectId());
    config.setPackageName(page.getPackageName());
    
    if (page.useDefaults()) {
      config.setEclipseProjectLocationUri(null);
    } else {
      config.setEclipseProjectLocationUri(page.getLocationURI());
    }
    config.setProject(page.getProjectHandle());
    
    // todo set up
    final IAdaptable uiInfoAdapter = WorkspaceUndoUtil.getUIInfoAdapter(getShell());
    IRunnableWithProgress runnable = new CreateAppEngineStandardWtpProject(config, uiInfoAdapter);

    IStatus status = Status.OK_STATUS;
    try {
      boolean fork = true;
      boolean cancelable = true;
      getContainer().run(fork, cancelable, runnable);
    } catch (InterruptedException ex) {
      status = Status.CANCEL_STATUS;
    } catch (InvocationTargetException ex) {
      int errorCode = 1;
      status = new Status(Status.ERROR, "todo plugin ID", errorCode, ex.getMessage(), null);
    }
    
    showErrorMessageIfNecessary(status);
    
    return status.isOK();
  }

  
  // visible for testing
  void showErrorMessageIfNecessary(IStatus status) {
    // if fail, display error message in wizard
    if (status == Status.CANCEL_STATUS) {
      page.setErrorMessage("User canceled project creation");
    } else if (!status.isOK()) {
      String message = "Failed to create project";
      if (status.getMessage() != null && !status.getMessage().isEmpty()) {
        message += ": " + status.getMessage();
      }
      page.setErrorMessage(message);
    }
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

}
