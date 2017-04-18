package com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployPreferencesDialog;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;

public class FlexDeployPreferencesDialog extends DeployPreferencesDialog {

  public FlexDeployPreferencesDialog(Shell parentShell, String title, IProject project,
      IGoogleLoginService loginService, IGoogleApiFactory googleApiFactory) {
    super(parentShell, title, project, loginService, googleApiFactory);
  }

}
