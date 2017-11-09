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

import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.AppEngineDeployPreferencesPanel;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.internal.AppYamlValidator;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.internal.RelativeFileFieldSetter;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class FlexDeployPreferencesPanel extends AppEngineDeployPreferencesPanel {

  public FlexDeployPreferencesPanel(Composite parent, IProject project,
      IGoogleLoginService loginService, Runnable layoutChangedHandler, boolean requireValues,
      ProjectRepository projectRepository) {
    this(parent, project, loginService, layoutChangedHandler, requireValues, projectRepository,
        new FlexDeployPreferences(project));
  }

  protected FlexDeployPreferencesPanel(Composite parent, IProject project,
      IGoogleLoginService loginService, Runnable layoutChangedHandler, boolean requireValues,
      ProjectRepository projectRepository, DeployPreferences model) {
    super(parent, project, loginService, layoutChangedHandler, requireValues, projectRepository,
        model);
  }

  @Override
  protected void createCenterArea() {
    Text appYamlField = createBrowseFileRow(
        Messages.getString("deploy.preferences.dialog.label.app.yaml"),
        Messages.getString("tooltip.app.yaml"),
        getWorkingDirectory(), new String[] {"*.yaml"});
    setupPossiblyUnvalidatedTextFieldDataBinding(appYamlField, "appYamlPath",
        new AppYamlValidator(getWorkingDirectory(), appYamlField));

    super.createCenterArea();

    Button includeOptionalConfigurationFilesButton = createCheckBox(
        Messages.getString("deploy.config.files"),
        Messages.getString("tooltip.deploy.config.files.flexible"));
    setupCheckBoxDataBinding(
        includeOptionalConfigurationFilesButton, "includeOptionalConfigurationFiles");
  }

  /**
   * Helper method to create a row of a {@link Label}, a {@link Text} for a file path input, and
   * a "browse" {@link Button} to open a {@link FileDialog}.
   *
   * @param fileFieldBasePath a base path that file input fields will treat as a prefix. The path is
   * for 1) relativizing absolute paths in file input fields; and 2) setting the default path for
   * the file chooser dialog.
   */
  protected Text createBrowseFileRow(String labelText, String Tooltip,
      IPath fileFieldBasePath, String[] fileDialogfilterExtensions) {
    Label label = new Label(this, SWT.LEAD);
    label.setText(labelText);
    label.setToolTipText(Tooltip);

    Composite secondColumn = new Composite(this, SWT.NONE);
    Text fileField = new Text(secondColumn, SWT.SINGLE | SWT.BORDER);
    fileField.setToolTipText(Tooltip);

    Button browse = new Button(secondColumn, SWT.PUSH);
    browse.setText(Messages.getString("deploy.preferences.dialog.browse"));
    browse.addSelectionListener(
        new RelativeFileFieldSetter(fileField, fileFieldBasePath, fileDialogfilterExtensions));

    GridDataFactory.fillDefaults().applyTo(secondColumn);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(fileField);
    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(secondColumn);
    return fileField;
  }

  @Override
  protected String getHelpContextId() {
    return "com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployAppEngineFlexProjectContext";
  }

  protected IPath getWorkingDirectory() {
    return project.getLocation();
  }
}
