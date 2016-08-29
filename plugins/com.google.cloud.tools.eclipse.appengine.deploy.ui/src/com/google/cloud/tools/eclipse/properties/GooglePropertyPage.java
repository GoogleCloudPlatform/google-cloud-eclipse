package com.google.cloud.tools.eclipse.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;

// To be moved to a more general bundle
// https://github.com/GoogleCloudPlatform/gcloud-eclipse-tools/issues/507
public class GooglePropertyPage extends PropertyPage {

  @Override
  protected Control createContents(Composite parent) {
    Label label = new Label(parent, SWT.LEFT);
    label.setText(Messages.getString("google.preferences")); //$NON-NLS-1$
    return label;
  }

}
