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

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.ui.LibrarySelectorGroup;
import com.google.cloud.tools.eclipse.appengine.newproject.maven.MavenCoordinatesWizardUi;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.util.JavaPackageValidator;
import com.google.cloud.tools.eclipse.util.MavenCoordinatesValidator;
import com.google.cloud.tools.project.ServiceNameValidator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

/**
 * UI to collect all information necessary to create a new App Engine Java Eclipse project.
 */
public abstract class AppEngineWizardPage extends WizardNewProjectCreationPage {

  private LibrarySelectorGroup appEngineLibrariesSelectorGroup;
  private Text javaPackageField;
  private Text serviceNameField;
  private MavenCoordinatesWizardUi mavenCoordinatesUi;

  /** True if we should auto-generate the javaPackageField from the provided groupId */
  @VisibleForTesting
  boolean autoGeneratePackageName = true;

  /** True if we're programmatically setting javaPackageField with an auto-generated value */
  private boolean javaPackageProgrammaticUpdate = false;

  public AppEngineWizardPage() {
    super("basicNewProjectPage"); //$NON-NLS-1$
    setImageDescriptor(AppEngineImages.appEngine(64));
  }

  public abstract void setHelp(Composite container);

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);

    Composite container = (Composite) getControl();
    setHelp(container);

    createCustomFields(container);

    mavenCoordinatesUi = new MavenCoordinatesWizardUi(container, SWT.NONE);
    mavenCoordinatesUi.addChangeListener(event -> revalidate());
    mavenCoordinatesUi.addGroupIdModifyListener(new AutoPackageNameSetterOnGroupIdChange());

    // Manage APIs
    appEngineLibrariesSelectorGroup = new LibrarySelectorGroup(container,
        getSupportedLibrariesGroup(),
        Messages.getString("app.engine.libraries.group")); //$NON-NLS-1$

    revalidate();
    // Show enter project name on opening
    setErrorMessage(null);
    setMessage(Messages.getString("enter.project.name")); //$NON-NLS-1$

    GridLayoutFactory.swtDefaults().generateLayout(container);
    Dialog.applyDialogFont(container);
  }

  protected abstract String getSupportedLibrariesGroup();

  private void createCustomFields(Composite container) {
    Composite composite = new Composite(container, SWT.NONE);
    createRuntimeField(composite);
    createPackageField(composite);
    createServiceField(composite);

    GridLayoutFactory.swtDefaults().numColumns(2).generateLayout(composite);
  }

  protected void revalidate() {
    setPageComplete(validatePage());
  }

  /**
   * Creates a Runtime section. Composite is laid out with 2 columns.
   */
  protected void createRuntimeField(@SuppressWarnings("unused") Composite composite) {
    // default: do nothing; used by subclasses
  }

  public String getRuntimeId() {
    return null;
  }

  // Java package name; Composite is laid out with 2 columns.
  private void createPackageField(Composite parent) {
    Label packageNameLabel = new Label(parent, SWT.LEAD);
    packageNameLabel.setText(Messages.getString("java.package")); //$NON-NLS-1$
    javaPackageField = new Text(parent, SWT.BORDER);
    javaPackageField.addModifyListener(event -> revalidate());
    javaPackageField.addVerifyListener(event -> {
      // if the user ever changes the package name field, then we never auto-generate again.
      if (!javaPackageProgrammaticUpdate) {
        autoGeneratePackageName = false;
      }
    });
  }

  // App Engine service name
  private void createServiceField(Composite parent) {
    Label serviceNameLabel = new Label(parent, SWT.LEAD);
    serviceNameLabel.setText(Messages.getString("app.engine.service")); //$NON-NLS-1$
    serviceNameField = new Text(parent, SWT.BORDER);
    serviceNameField.setMessage("default"); //$NON-NLS-1$
    serviceNameField.addModifyListener(event -> revalidate());
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

    return mavenCoordinatesUi.setValidationMessage(this);
  }

  public String getPackageName() {
    return javaPackageField.getText();
  }

  public boolean asMavenProject() {
    return mavenCoordinatesUi.uiEnabled();
  }

  public String getMavenGroupId() {
    return mavenCoordinatesUi.getGroupId();
  }

  public String getMavenArtifactId() {
    return mavenCoordinatesUi.getArtifactId();
  }

  public String getMavenVersion() {
    return mavenCoordinatesUi.getVersion();
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

  private void updatePackageField(String newSuggestion) {
    if (autoGeneratePackageName) {
      javaPackageProgrammaticUpdate = true;
      javaPackageField.setText(newSuggestion);
      javaPackageProgrammaticUpdate = false;
    }
  }

  /**
   * Auto-fills {@link #javaPackageField} from the Group ID if the user has not explicitly chosen a
   * package name.
   */
  private final class AutoPackageNameSetterOnGroupIdChange implements ModifyListener {
    @Override
    public void modifyText(ModifyEvent event) {
      String groupId = mavenCoordinatesUi.getGroupId();

      if (MavenCoordinatesValidator.validateGroupId(groupId)) {
        String newSuggestion = suggestPackageName(groupId);
        updatePackageField(newSuggestion);
      } else if (groupId.isEmpty()) {
        updatePackageField(""); //$NON-NLS-1$
      }
    }
  }
  
  /**
   * Helper function returning a suggested package name based on {@code groupId}.
   * It does basic string filtering/manipulation, which does not completely eliminate
   * naming issues. However, users will be alerted of any errors in naming by
   * {@link #validatePage}.
   */
  @VisibleForTesting
  static String suggestPackageName(String groupId) {
    if (JavaPackageValidator.validate(groupId).isOK()) {
      return groupId;
    }

    // 1) Remove leading and trailing dots.
    // 2) Keep only word characters ([a-zA-Z_0-9]) and dots (escaping inside [] not necessary).
    // 3) Replace consecutive dots with a single dot.
    return CharMatcher.is('.').trimFrom(groupId)
        .replaceAll("[^\\w.]", "").replaceAll("\\.+",  "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  }
}
