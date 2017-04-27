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
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
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
    createAppEngineDirectorySection();

    super.createCenterArea();

    Button includeOptionalConfigurationFilesButton = createCheckBox(
        Messages.getString("deploy.config.files"),
        Messages.getString("tooltip.deploy.config.files.flexible"));
    setupCheckBoxDataBinding(
        includeOptionalConfigurationFilesButton, "includeOptionalConfigurationFiles");
  }

  private void createAppEngineDirectorySection() {
    Label label = new Label(this, SWT.LEAD);
    label.setText(Messages.getString("deploy.preferences.dialog.label.appEngineDirectory"));
    label.setToolTipText(Messages.getString("tooltip.appEngineDirectory"));

    Composite secondColumn = new Composite(this, SWT.NONE);
    final Text directoryField = new Text(secondColumn, SWT.SINGLE | SWT.BORDER);
    directoryField.setToolTipText(Messages.getString("tooltip.appEngineDirectory"));

    Button browse = new Button(secondColumn, SWT.PUSH);
    browse.setText(Messages.getString("deploy.preferences.dialog.browser.dir"));
    browse.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        IPath path = new Path(directoryField.getText().trim());
        if (path.isAbsolute()) {
          dialog.setFilterPath(path.toString());
        } else {
          dialog.setFilterPath(project.getLocation() + "/" + path.toString());
        }
        String result = dialog.open();
        if (result != null) {
          IPath maybeProjectRelative = new Path(result).makeRelativeTo(project.getLocation());
          directoryField.setText(maybeProjectRelative.toString());
        }
      }
    });

    GridLayoutFactory.fillDefaults().numColumns(2).applyTo(secondColumn);
    GridData fillGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    secondColumn.setLayoutData(fillGridData);
    directoryField.setLayoutData(fillGridData);

    ISWTObservableValue fieldValue = WidgetProperties.text().observe(directoryField);
    IObservableValue modelValue = PojoProperties.value("appEngineDirectory").observe(model);
    bindingContext.bindValue(fieldValue, modelValue);

    /*
    UpdateValueStrategy modelToField = new UpdateValueStrategy().setConverter(
        new Converter(String.class, String.class) {
          @Override
          public Object convert(Object fromObject) {
            String modelValue = (String) fromObject;
            if (Strings.isNullOrEmpty(modelValue)) {
              return project.getLocation() + "/src/main/appengine";
            } else {
              return modelValue;
            }
          }
        });
    bindingContext.bindValue(fieldValue, modelValue, new UpdateValueStrategy(), modelToField);
    */

/*
    UpdateValueStrategy fieldToModel = new UpdateValueStrategy().setConverter(
        new Converter(String.class, String.class) {
          @Override
          public Object convert(Object fromObject) {
            IPath path = new Path((String) fromObject);
            if (path.isAbsolute()) {
              IPath relativePath = PathUtil.relativizePath(path, project.getLocation());
              System.out.println("user input: " + path);
              System.out.println("project: " + project.getLocation());
              System.out.println("relativized: " + relativePath);
              return relativePath.toString();
            } else {
              return path.toString();
            }
          }
        });
    bindingContext.bindValue(fieldValue, modelValue, fieldToModel, new UpdateValueStrategy());
*/
  }

  @Override
  protected String getHelpContextId() {
    return "com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployAppEngineFlexProjectContext";
  }
}
