package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

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
    
    // todo set up
    IProgressMonitor monitor = null;
    IStatus status = EclipseProjectCreator.makeNewProject(config, monitor);
    // todo if fail, call  use setErrorMessage()
    return status.isOK();
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

}
