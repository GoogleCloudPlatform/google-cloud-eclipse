package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class StandardProjectWizard extends Wizard implements INewWizard {

  private IWizardPage page;

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.setWindowTitle("New App Engine Standard Project");
    page = new AppEngineStandardWizardPage("first page");
    this.addPage(page);
  }
  
  @Override
  public boolean canFinish() {
    return page.isPageComplete();
  }

  @Override
  public boolean performFinish() {
    return true;
  }

}
