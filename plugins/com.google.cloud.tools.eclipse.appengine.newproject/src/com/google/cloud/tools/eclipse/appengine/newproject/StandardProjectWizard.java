package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class StandardProjectWizard extends Wizard implements INewWizard {

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.setWindowTitle("New App Engine Standard Project");
    // see BasicNewResourceProjectWizard which is what we need except it isn't meant to be subclassed
    IWizardPage page = new StubWizardPage("first page");
    this.addPage(page);
  }
  
  @Override
  public boolean canFinish() {
    return true;
  }

  @Override
  public boolean performFinish() {
    return true;
  }

}
