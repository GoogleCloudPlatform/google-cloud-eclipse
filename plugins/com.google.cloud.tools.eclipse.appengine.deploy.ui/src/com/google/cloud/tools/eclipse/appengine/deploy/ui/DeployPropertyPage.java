package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class DeployPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

  @Override
  protected Control createContents(Composite parent) {
    Label label = new Label(parent, SWT.LEFT);
    label.setText(Messages.getString("preferences.title")); //$NON-NLS-1$
    return label;
  }

}
