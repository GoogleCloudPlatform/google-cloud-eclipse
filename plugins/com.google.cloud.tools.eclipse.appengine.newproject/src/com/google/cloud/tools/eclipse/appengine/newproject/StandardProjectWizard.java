package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class StandardProjectWizard extends Wizard implements INewWizard {

  private AppEngineStandardWizardPage page;
  private AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.setWindowTitle("New App Engine Standard Project");
    page = new AppEngineStandardWizardPage("first page");
    this.addPage(page);
  }

  @Override
  public boolean performFinish() {
    // todo is this the right time/place to grab these?
    config.setAppEngineProjectId(page.getAppEngineProjectId());
   // config.setEclipseProjectDirectory(page.getLocationPath());
    config.setEclipseProjectName(page.getProjectName());
    config.setPackageName(page.getPackageName());
    
    EclipseProjectCreator.makeNewProject(config);
    // todo what are we supposed to do if project creation fails?
    return true;
  }

}
