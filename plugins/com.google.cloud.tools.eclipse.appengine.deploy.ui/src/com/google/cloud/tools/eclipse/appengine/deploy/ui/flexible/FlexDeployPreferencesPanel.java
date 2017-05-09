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

import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.AppEngineDeployPreferencesPanel;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.internal.AppYamlValidator;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.internal.RelativeFileFieldSetter;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class FlexDeployPreferencesPanel extends AppEngineDeployPreferencesPanel {

  public FlexDeployPreferencesPanel(Composite parent, IProject project,
      IGoogleLoginService loginService, Runnable layoutChangedHandler, boolean requireValues,
      ProjectRepository projectRepository) {
    super(parent, project, loginService, layoutChangedHandler, requireValues, projectRepository,
        new FlexDeployPreferences(project));
  }

  @Override
  protected void createCenterArea() {
    createAppYamlSection();

    super.createCenterArea();

    Button includeOptionalConfigurationFilesButton = createCheckBox(
        Messages.getString("deploy.config.files"),
        Messages.getString("tooltip.deploy.config.files.flexible"));
    setupCheckBoxDataBinding(
        includeOptionalConfigurationFilesButton, "includeOptionalConfigurationFiles");
  }

  private void createAppYamlSection() {
    // Part 1. create UI widgets
    Label label = new Label(this, SWT.LEAD);
    label.setText(Messages.getString("deploy.preferences.dialog.label.app.yaml"));
    label.setToolTipText(Messages.getString("tooltip.app.yaml"));

    Composite secondColumn = new Composite(this, SWT.NONE);
    Text appYamlField = new Text(secondColumn, SWT.SINGLE | SWT.BORDER);
    appYamlField.setToolTipText(Messages.getString("tooltip.app.yaml"));

    Button browse = new Button(secondColumn, SWT.PUSH);
    browse.setText(Messages.getString("deploy.preferences.dialog.browse"));
    browse.addSelectionListener(new RelativeFileFieldSetter(appYamlField, project.getLocation()));

    GridLayoutFactory.fillDefaults().numColumns(2).applyTo(secondColumn);
    GridData fillGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    secondColumn.setLayoutData(fillGridData);
    appYamlField.setLayoutData(fillGridData);

    // Part 2. set up data binding
    ISWTObservableValue fieldValue = WidgetProperties.text(SWT.Modify).observe(appYamlField);
    IObservableValue modelValue = PojoProperties.value("appYamlPath").observe(model);

    bindingContext.bindValue(fieldValue, modelValue);
    if (requireValues) {
      bindingContext.addValidationStatusProvider(
          new AppYamlValidator(project.getLocation(), fieldValue));
    }
  }

  @Override
  protected String getHelpContextId() {
    return "com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployAppEngineFlexProjectContext";
  }
}
