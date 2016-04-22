package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class StubWizardPage extends WizardPage implements IWizardPage {

  protected StubWizardPage(String pageName) {
    super(pageName);
  }

  @Override
  public void createControl(final Composite parent) {
    final Composite composite = new Composite(parent, SWT.NULL);
    setControl(composite);
  }

}
