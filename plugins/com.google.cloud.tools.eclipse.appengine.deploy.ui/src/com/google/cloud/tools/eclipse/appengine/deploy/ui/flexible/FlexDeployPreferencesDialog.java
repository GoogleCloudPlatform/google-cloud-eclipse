package com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployPreferencesDialog;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorDialogErrorHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

public class FlexDeployPreferencesDialog extends DeployPreferencesDialog {

  public FlexDeployPreferencesDialog(Shell parentShell, String title, IProject project,
      IGoogleLoginService loginService, IGoogleApiFactory googleApiFactory) {
    super(parentShell, title, project, loginService, googleApiFactory);
  }

  @Override
  protected Control createDialogArea(final Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);

    Composite container = new Composite(dialogArea, SWT.NONE);
    Link flexPricing = new Link(container, SWT.NONE);
    flexPricing.addSelectionListener(
        new OpenUriSelectionListener(new ErrorDialogErrorHandler(getShell())));
    flexPricing.setText(Messages.getString("deploy.preferences.dialog.flex.pricing")); //$NON-NLS-1$

    Point margins = LayoutConstants.getMargins();
    GridLayoutFactory.fillDefaults()
        .extendedMargins(margins.x, margins.x, 0 /* no upper margin */, margins.y)
        .generateLayout(container);

    return dialogArea;
  }
}
