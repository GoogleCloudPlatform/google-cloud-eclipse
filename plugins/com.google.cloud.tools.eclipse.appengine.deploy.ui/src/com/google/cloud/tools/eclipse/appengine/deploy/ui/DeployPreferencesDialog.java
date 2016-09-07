package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.google.common.base.Preconditions;

public class DeployPreferencesDialog extends Dialog {

  private static final Image ERROR_ICON = 
      PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK).createImage();

  private DeployPreferencesPanel content;
  private IProject project;

  private CLabel validationMessage;

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
  protected Control createDialogArea(final Composite parent) {
    validationMessage = new CLabel(parent, SWT.LEFT);
    validationMessage.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    Composite dialogArea = (Composite) super.createDialogArea(parent);
    ScrolledComposite scrolledComposite = new ScrolledComposite(dialogArea, SWT.V_SCROLL | SWT.H_SCROLL);
    scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    scrolledComposite.setShowFocusedControl(true);
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);
    scrolledComposite.addControlListener(new ControlListener() {
      @Override
      public void controlResized(ControlEvent e) {
        Point computeSize = DeployPreferencesDialog.this.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        DeployPreferencesDialog.this.getShell().setSize(computeSize);
      }

      @Override
      public void controlMoved(ControlEvent e) {
      }
    });

    content = new DeployPreferencesPanel(scrolledComposite, project);
    content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    scrolledComposite.setContent(content);

    DataBindingContext dataBindingContext = content.getDataBindingContext();
    final AggregateValidationStatus validationStatus =
        new AggregateValidationStatus(dataBindingContext, AggregateValidationStatus.MAX_SEVERITY);
    validationStatus.addValueChangeListener(new IValueChangeListener() {
      @Override
      public void handleValueChange(ValueChangeEvent event) {
        IStatus status = (IStatus) validationStatus.getValue();
        if (status.isOK()) {
          updateValidationStatus("");
        } else {
          updateValidationStatus(status.getMessage());
        }
      }
    });
    return content;
  }
  
  
  private void updateValidationStatus(String errorMessage) {
    Preconditions.checkNotNull(errorMessage, "errorMessage is null");
    
    if (!validationMessage.isDisposed()) {
      getButton(IDialogConstants.OK_ID).setEnabled(errorMessage.isEmpty());
      validationMessage.setText(errorMessage);
      validationMessage.setImage(errorMessage.isEmpty() ? null : ERROR_ICON);
      validationMessage.setVisible(!errorMessage.isEmpty());
      ((GridData)validationMessage.getLayoutData()).exclude = errorMessage.isEmpty();
      validationMessage.getParent().layout();
    }
  }
}
