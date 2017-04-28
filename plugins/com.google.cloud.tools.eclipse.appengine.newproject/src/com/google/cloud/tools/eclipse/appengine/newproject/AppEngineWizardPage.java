/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject;

import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.appengine.ui.LibrarySelectorGroup;
import com.google.cloud.tools.project.ServiceNameValidator;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

/**
 * UI to collect all information necessary to create a new App Engine Java Eclipse project.
 */
public abstract class AppEngineWizardPage extends WizardNewProjectCreationPage {

  private Text javaPackageField;
  private LibrarySelectorGroup appEngineLibrariesSelectorGroup;
  private Text serviceNameField;
  private final boolean showLibrariesSelectorGroup;

  public AppEngineWizardPage(boolean showLibrariesSelectorGroup) {
    super("basicNewProjectPage"); //$NON-NLS-1$
    setImageDescriptor(AppEngineImages.appEngine(64));
    this.showLibrariesSelectorGroup = showLibrariesSelectorGroup;
  }

  public abstract void sendAnalyticsPing(Shell parentShell);

  public abstract void setHelp(Composite container);

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    sendAnalyticsPing(parent.getShell());

    Composite container = (Composite) getControl();
    setHelp(container);

    ModifyListener pageValidator = new PageValidator();
    createCustomFields(container, pageValidator);

    // Manage APIs
    // todo we don't need this if; can do with subclasses
    if (showLibrariesSelectorGroup) {
      appEngineLibrariesSelectorGroup =
          new LibrarySelectorGroup(container, CloudLibraries.APP_ENGINE_GROUP);
    }

    setPageComplete(validatePage());
    // Show enter project name on opening
    setErrorMessage(null);
    setMessage(Messages.getString("enter.project.name"));

    Dialog.applyDialogFont(container);
  }

  private void createCustomFields(Composite container, ModifyListener pageValidator) {
    Composite composite = new Composite(container, SWT.NONE);
    GridDataFactory.fillDefaults().applyTo(composite);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    composite.setLayout(layout);

    createPackageField(composite, pageValidator);
    createServiceField(composite, pageValidator);
  }

  // Java package name
  private void createPackageField(Composite parent, ModifyListener pageValidator) {
    Label packageNameLabel = new Label(parent, SWT.LEAD);
    packageNameLabel.setText(Messages.getString("java.package")); //$NON-NLS-1$
    javaPackageField = new Text(parent, SWT.BORDER);

    javaPackageField.addModifyListener(pageValidator);

    GridDataFactory.fillDefaults().grab(true, false).applyTo(javaPackageField);
  }

  // App Engine service name
  private void createServiceField(Composite parent, ModifyListener pageValidator) {

    Label serviceNameLabel = new Label(parent, SWT.LEAD);
    serviceNameLabel.setText(Messages.getString("app.engine.service"));
    serviceNameField = new Text(parent, SWT.BORDER);
    serviceNameField.setMessage("default"); //$NON-NLS-1$
    serviceNameField.addModifyListener(pageValidator);

    GridDataFactory.fillDefaults().grab(true, false).applyTo(serviceNameField);
  }

  @Override
  public boolean validatePage() {
    setErrorMessage(null);
    setMessage(null);

    if (!super.validatePage()) {
      return false;
    }

    return validateLocalFields();
  }

  private boolean validateLocalFields() {
    String packageName = javaPackageField.getText();
    IStatus packageStatus = JavaPackageValidator.validate(packageName);
    if (!packageStatus.isOK()) {
      String message = Messages.getString("illegal.package.name",  //$NON-NLS-1$
          packageName, packageStatus.getMessage());
      setErrorMessage(message);
      return false;
    }

    String serviceName = serviceNameField.getText();
    boolean serviceNameValid =
        serviceName.isEmpty() || ServiceNameValidator.validate(serviceName);
    if (!serviceNameValid) {
      String message = Messages.getString("illegal.service.name", serviceName); //$NON-NLS-1$
      setErrorMessage(message);
      return false;
    }

    File parent = getLocationPath().toFile();
    File projectDirectory = new File(parent, getProjectName());
    if (projectDirectory.exists()) {
      setErrorMessage(
          Messages.getString("project.location.exists", projectDirectory)); //$NON-NLS-1$
      return false;
    }

    return true;
  }

  private final class PageValidator implements ModifyListener {
    @Override
    public void modifyText(ModifyEvent event) {
      setPageComplete(validatePage());
    }
  }

  public String getPackageName() {
    return javaPackageField.getText();
  }

  public Collection<Library> getSelectedLibraries() {
    if (appEngineLibrariesSelectorGroup == null) {
      return new HashSet<>();
    } else {
      return appEngineLibrariesSelectorGroup.getSelectedLibraries();
    }
  }

  @Override
  public void dispose() {
    if (appEngineLibrariesSelectorGroup != null) {
      appEngineLibrariesSelectorGroup.dispose();
    }
    super.dispose();
  }

  public String getServiceName() {
    return serviceNameField.getText();
  }
}
