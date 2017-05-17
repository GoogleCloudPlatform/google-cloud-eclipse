/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.AppEngineDeployPreferencesPanel;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployPreferencesDialog;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.ui.util.FontUtil;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorDialogErrorHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

class FlexDeployPreferencesDialog extends DeployPreferencesDialog {

  FlexDeployPreferencesDialog(Shell parentShell, String title, IProject project,
      IGoogleLoginService loginService, IGoogleApiFactory googleApiFactory) {
    super(parentShell, title, project, loginService, googleApiFactory);
  }

  @Override
  protected Control createDialogArea(final Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);

    Composite container = new Composite(dialogArea, SWT.NONE);
    Link flexPricing = new Link(container, SWT.WRAP);
    flexPricing.setText(Messages.getString("deploy.preferences.dialog.flex.pricing")); //$NON-NLS-1$
    flexPricing.addSelectionListener(
        new OpenUriSelectionListener(new ErrorDialogErrorHandler(getShell())));
    FontUtil.convertFontToItalic(flexPricing);

    GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
    Point margins = LayoutConstants.getMargins();
    GridLayoutFactory.fillDefaults()
        .extendedMargins(margins.x, margins.x, 0 /* no upper margin */, margins.y)
        .generateLayout(container);

    return dialogArea;
  }

  @Override
  protected AppEngineDeployPreferencesPanel createDeployPreferencesPanel(Composite container,
      IProject project, IGoogleLoginService loginService, Runnable layoutChangedHandler,
      boolean requireValues, ProjectRepository projectRepository) {
    return new FlexDeployPreferencesPanel(container, project, loginService, layoutChangedHandler,
        requireValues, projectRepository);
  }
}
