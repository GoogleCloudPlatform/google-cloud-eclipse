package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class DeployPreferencesDialog extends Dialog {

  private DeployPreferencesPanel content;
  private IProject project;

  public DeployPreferencesDialog(Shell parentShell, IProject project) {
    super(parentShell);
    this.project = project;
  }

  
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Deployment preferences");
  }


  @Override
  protected Control createDialogArea(Composite parent) {
    Control dialogArea = super.createDialogArea(parent);
    content = new DeployPreferencesPanel((Composite) dialogArea, project);
    DataBindingContext dataBindingContext = content.getDataBindingContext();
    final AggregateValidationStatus validationStatus =
        new AggregateValidationStatus(dataBindingContext, AggregateValidationStatus.MAX_SEVERITY);
    validationStatus.addValueChangeListener(new IValueChangeListener() {
      @Override
      public void handleValueChange(ValueChangeEvent event) {
        IStatus status = (IStatus) validationStatus.getValue();
        if (status.isOK()) {
          getButton(IDialogConstants.OK_ID).setEnabled(true);
        } else {
          getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
      }
    });
    return content;
  }
}
