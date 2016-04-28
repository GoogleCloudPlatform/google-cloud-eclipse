package com.google.cloud.tools.eclipse.appengine.newproject;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

public class StandardProjectWizard extends Wizard implements INewWizard {

  private AppEngineStandardWizardPage page;
  private AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
  
  public StandardProjectWizard() {
    this.setWindowTitle("New App Engine Standard Project");
    page = new AppEngineStandardWizardPage();
  }
  
  @Override 
  public void addPages() {
    this.addPage(page);
  }

  @Override
  public boolean performFinish() {
    // todo is this the right time/place to grab these?
    config.setAppEngineProjectId(page.getAppEngineProjectId());
   // config.setEclipseProjectDirectory(page.getLocationPath());
    config.setEclipseProjectName(page.getProjectName());
    config.setPackageName(page.getPackageName());
    
    if (page.useDefaults()) {
      config.setEclipseProjectLocationUri(null);
    } else {
      config.setEclipseProjectLocationUri(page.getLocationURI());
    }
    config.setProject(page.getProjectHandle());
    
    // todo set up
    IProgressMonitor monitor = null;
    IAdaptable uiInfoAdapter = WorkspaceUndoUtil.getUIInfoAdapter(getShell());
    IRunnableWithProgress runnable = EclipseProjectCreator.makeNewProject(config, monitor, uiInfoAdapter);

    IStatus status = Status.OK_STATUS;
    try {
      boolean fork = true;
      boolean cancelable = true;
      getContainer().run(fork, cancelable, runnable);
    } catch (InterruptedException ex) {
      status = Status.CANCEL_STATUS;
    } catch (InvocationTargetException ex) {
      status = new Status(Status.ERROR, ex.getMessage(), 1, "", null);
    }
    
    // todo if fail, call setErrorMessage()
    return status.isOK();
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

}
