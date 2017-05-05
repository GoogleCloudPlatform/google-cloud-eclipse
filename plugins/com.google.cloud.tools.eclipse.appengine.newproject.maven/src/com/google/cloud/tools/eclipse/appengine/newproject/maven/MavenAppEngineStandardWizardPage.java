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

package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.newproject.JavaPackageValidator;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.appengine.ui.LibrarySelectorGroup;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.io.FilePermissions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Paths;
import java.util.Collection;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * UI to collect all information necessary to create a new Maven-based App Engine Standard
 * environment Java project.
 */
public class MavenAppEngineStandardWizardPage extends WizardPage {

  private String defaultVersion = "0.1.0-SNAPSHOT"; //$NON-NLS-1$

  @VisibleForTesting Button useDefaults;
  @VisibleForTesting Text locationField;
  private Button locationBrowseButton;
  @VisibleForTesting Text groupIdField;
  private Text artifactIdField;
  private Text versionField;
  @VisibleForTesting Text javaPackageField;
  private LibrarySelectorGroup appEngineLibrariesSelectorGroup;

  private boolean canFlipPage;

  /** True if we should auto-generate the javaPackageField from the provided groupId */
  @VisibleForTesting
  boolean autoGeneratePackageName = true;

  /** True if we're programmatically setting javaPackageField with an auto-generated value */
  private boolean javaPackageProgrammaticUpdate = false;

  private final IPath workspaceLocation;

  public MavenAppEngineStandardWizardPage() {
    this(ResourcesPlugin.getWorkspace().getRoot().getLocation());
  }

  @VisibleForTesting
  public MavenAppEngineStandardWizardPage(IPath workspaceLocation) {
    super("basicNewProjectPage"); //$NON-NLS-1$
    setTitle(Messages.getString("WIZARD_TITLE")); //$NON-NLS-1$
    setDescription(Messages.getString("WIZARD_DESCRIPTION")); //$NON-NLS-1$
    setImageDescriptor(AppEngineImages.appEngine(64));

    canFlipPage = false;
    this.workspaceLocation = workspaceLocation;
  }

  @Override
  public void createControl(Composite parent) {
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_MAVEN, parent.getShell());

    Composite container = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
    setControl(container);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
        "com.google.cloud.tools.eclipse.appengine.newproject.maven.NewMavenProjectContext"); //$NON-NLS-1$

    ModifyListener pageValidator = new PageValidator();
    createLocationArea(container, pageValidator);
    createMavenCoordinatesArea(container, pageValidator);
    createAppEngineProjectDetailsArea(container, pageValidator);
    appEngineLibrariesSelectorGroup =
        new LibrarySelectorGroup(container, CloudLibraries.APP_ENGINE_GROUP);
    appEngineLibrariesSelectorGroup.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        validatePage();
      }
    });

    Dialog.applyDialogFont(container);
  }

  /** Create UI for specifying the generated location area */
  private void createLocationArea(Composite container, ModifyListener pageValidator) {

    Group locationGroup = new Group(container, SWT.NONE);
    locationGroup.setText(Messages.getString("LOCATION_GROUP_TEXT")); //$NON-NLS-1$
    GridDataFactory.fillDefaults().span(2, 1).applyTo(locationGroup);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(locationGroup);

    useDefaults = new Button(locationGroup, SWT.CHECK | SWT.LEAD);
    GridDataFactory.defaultsFor(useDefaults).span(3, 1).applyTo(useDefaults);
    useDefaults.setText(Messages.getString("CREATE_PROJECT_IN_WORKSPACE")); //$NON-NLS-1$
    useDefaults.setSelection(true);

    Label locationLabel = new Label(locationGroup, SWT.LEAD);
    locationLabel.setText(Messages.getString("LOCATION_LABEL")); //$NON-NLS-1$
    locationLabel.setToolTipText(Messages.getString("LOCATION_TOOL_TIP")); //$NON-NLS-1$

    locationField = new Text(locationGroup, SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false)
        .applyTo(locationField);
    locationField.setText(workspaceLocation.toOSString());
    locationField.setData("" /* initially empty for manually entered location */); //$NON-NLS-1$
    locationField.addModifyListener(pageValidator);
    locationField.setEnabled(false);

    locationBrowseButton = new Button(locationGroup, SWT.PUSH);
    locationBrowseButton.setText(Messages.getString("BROWSE_BUTTON_LABEL")); //$NON-NLS-1$
    locationBrowseButton.setEnabled(false);
    locationBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        openLocationDialog();
      }
    });
    useDefaults.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        if (useDefaults.getSelection()) {
          locationField.setData(locationField.getText());  // Save to restore it later.
          locationField.setText(workspaceLocation.toOSString());
        } else {
          String previousValue = (String) locationField.getData();
          locationField.setText(previousValue);
        }

        locationField.setEnabled(!useDefaults.getSelection());
        locationBrowseButton.setEnabled(!useDefaults.getSelection());
        checkFlipToNext();
      }
    });
  }

  /** Create UI for specifying desired Maven Coordinates */
  private void createMavenCoordinatesArea(Composite container, ModifyListener pageValidator) {
    Group mavenCoordinatesGroup = new Group(container, SWT.NONE);
    mavenCoordinatesGroup.setText(Messages.getString("MAVEN_PROJECT_COORDINATES")); //$NON-NLS-1$
    GridDataFactory.defaultsFor(mavenCoordinatesGroup).span(2, 1).applyTo(mavenCoordinatesGroup);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(mavenCoordinatesGroup);

    Label groupIdLabel = new Label(mavenCoordinatesGroup, SWT.LEAD);
    groupIdLabel.setText(Messages.getString("GROUP_ID")); //$NON-NLS-1$
    groupIdField = new Text(mavenCoordinatesGroup, SWT.BORDER);
    groupIdField.setToolTipText(Messages.getString("GROUP_ID_TOOLTIP")); //$NON-NLS-1$

    GridDataFactory.defaultsFor(groupIdField).align(SWT.FILL, SWT.CENTER).applyTo(groupIdField);
    groupIdField.addModifyListener(pageValidator);
    groupIdField.addModifyListener(new AutoPackageNameSetterOnGroupIdChange());

    Label artifactIdLabel = new Label(mavenCoordinatesGroup, SWT.LEAD);
    artifactIdLabel.setText(Messages.getString("ARTIFACT_ID")); //$NON-NLS-1$
    artifactIdField = new Text(mavenCoordinatesGroup, SWT.BORDER);
    artifactIdField.setToolTipText(Messages.getString("ARTIFACT_ID_TOOLTIP")); //$NON-NLS-1$

    GridDataFactory.defaultsFor(artifactIdField).align(SWT.FILL, SWT.CENTER)
        .applyTo(artifactIdField);
    artifactIdField.addModifyListener(pageValidator);

    Label versionLabel = new Label(mavenCoordinatesGroup, SWT.LEAD);
    versionLabel.setText(Messages.getString("ARTIFACT_VERSION")); //$NON-NLS-1$
    versionField = new Text(mavenCoordinatesGroup, SWT.BORDER);
    versionField.setText(defaultVersion);
    GridDataFactory.defaultsFor(versionField).align(SWT.FILL, SWT.CENTER).applyTo(versionField);
    versionField.addModifyListener(pageValidator);
  }

  /** Create UI for specifying App Engine project details */
  private void createAppEngineProjectDetailsArea(Composite container,
      ModifyListener pageValidator) {
    Composite composite = new Composite(container, SWT.NONE);
    // assumed that container has a two-column GridLayout
    GridDataFactory.fillDefaults().span(2, 1).grab(true,  false).applyTo(composite);

    // Java package name
    Label packageNameLabel = new Label(composite, SWT.LEAD);
    packageNameLabel.setText(Messages.getString("JAVA_PACKAGE_LABEL")); //$NON-NLS-1$
    javaPackageField = new Text(composite, SWT.BORDER);
    javaPackageField.addModifyListener(pageValidator);
    javaPackageField.addVerifyListener(new VerifyListener() {

      @Override
      public void verifyText(VerifyEvent event) {
        // if the user ever changes the package name field, then we never auto-generate again
        if (!javaPackageProgrammaticUpdate) {
          autoGeneratePackageName = false;
        }
      }
    });

    GridDataFactory.fillDefaults().grab(true, false).applyTo(javaPackageField);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(composite);
  }

  private void openLocationDialog() {
    DirectoryDialog dialog = new DirectoryDialog(getShell());
    dialog.setText(Messages.getString("GENERATED_PROJECT_LOCATION")); //$NON-NLS-1$
    String location = dialog.open();
    if (location != null) {
      locationField.setText(location);
      checkFlipToNext();
    }
  }

  @Override
  public boolean canFlipToNextPage() {
    return canFlipPage;
  }

  private void checkFlipToNext() {
    canFlipPage = validatePage();
    getContainer().updateButtons();
  }

  /**
   * Validate and report on the contents of this page
   *
   * @return true if valid, false if there is a problem
   */
  private boolean validatePage() {
    setMessage(null);
    setErrorMessage(null);

    // order here should match order of the UI fields
    if (!useDefaults()) {
      if (!validateLocation(locationField.getText().trim(), this)) {
        return false;
      }
    }

    if (!validateMavenSettings()) {
      return false;
    }
    if (!validateGeneratedProjectLocation()) {
      return false;
    }
    if (!validateAppEngineProjectDetails()) {
      return false;
    }

    return true;
  }

  @VisibleForTesting
  static boolean validateLocation(String location, WizardPage page) {
    if (location.isEmpty()) {
      page.setMessage(Messages.getString("PROVIDE_LOCATION"), INFORMATION); //$NON-NLS-1$
      return false;
    } else {
      try {
        java.nio.file.Path path = Paths.get(location);
        FilePermissions.verifyDirectoryCreatable(path);
        return true;
      } catch (NotDirectoryException ex) {
          String message = Messages.getString("FILE_LOCATION", location); //$NON-NLS-1$
          page.setMessage(message, ERROR);
          return false;
      } catch (IOException ex) {
        String message = Messages.getString(
            "INVALID_PATH", location, ex.getLocalizedMessage()); //$NON-NLS-1$
        page.setMessage(message, ERROR);
        return false;
      }
    }
  }

  public Collection<Library> getSelectedLibraries() {
    return appEngineLibrariesSelectorGroup.getSelectedLibraries();
  }

  @Override
  public void dispose() {
    appEngineLibrariesSelectorGroup.dispose();
    super.dispose();
  }

  /**
   * Check that we won't overwrite an existing location. Expects a valid Maven artifact ID.
   */
  private boolean validateGeneratedProjectLocation() {
    String artifactId = getArtifactId();
    IPath path = getLocationPath().append(artifactId);
    if (path.toFile().exists()) {
      String errorMessage = Messages.getString("LOCATION_ALREADY_EXISTS", path); //$NON-NLS-1$
      setErrorMessage(errorMessage);
      return false;
    }
    return true;
  }

  private boolean validateMavenSettings() {
    String groupId = getGroupId();
    if (groupId.isEmpty()) {
      setMessage(Messages.getString("PROVIDE_GROUP_ID"), INFORMATION); //$NON-NLS-1$
      return false;
    } else if (!MavenCoordinatesValidator.validateGroupId(groupId)) {
      setErrorMessage(Messages.getString("ILLEGAL_GROUP_ID", groupId)); //$NON-NLS-1$
      return false;
    }
    String artifactId = getArtifactId();
    if (artifactId.isEmpty()) {
      setMessage(Messages.getString("PROVIDE_ARTIFACT_ID"), INFORMATION); //$NON-NLS-1$
      return false;
    } else if (!MavenCoordinatesValidator.validateArtifactId(artifactId)) {
      setErrorMessage(Messages.getString("ILLEGAL_ARTIFACT_ID", artifactId)); //$NON-NLS-1$
      return false;
    } else if (ResourcesPlugin.getWorkspace().getRoot().getProject(artifactId).exists()) {
      setErrorMessage(Messages.getString("PROJECT_ALREADY_EXISTS", artifactId)); //$NON-NLS-1$
      return false;
    }
    String version = getVersion();
    if (version.isEmpty()) {
      setMessage(Messages.getString("PROVIDE_VERSION"), INFORMATION); //$NON-NLS-1$
      return false;
    } else if (!MavenCoordinatesValidator.validateVersion(version)) {
      setErrorMessage(Messages.getString("ILLEGAL_VERSION") + version); //$NON-NLS-1$
      return false;
    }
    return true;
  }

  private boolean validateAppEngineProjectDetails() {
    String packageName = getPackageName();
    IStatus status = JavaPackageValidator.validate(packageName);
    if (!status.isOK()) {
      String details = status.getMessage() == null ? packageName : status.getMessage();
      String message = Messages.getString("ILLEGAL_PACKAGE_NAME", details); //$NON-NLS-1$
      setErrorMessage(message);
      return false;
    }

    return true;
  }

  /** Return the Maven group for the project */
  public String getGroupId() {
    return groupIdField.getText().trim();
  }

  /** Return the Maven artifact for the project */
  public String getArtifactId() {
    return artifactIdField.getText().trim();
  }

  /** Return the Maven version for the project */
  public String getVersion() {
    return versionField.getText().trim();
  }

  /**
   * If true, projects are generated into the workspace, otherwise placed into a specified location.
   */
  public boolean useDefaults() {
    return useDefaults.getSelection();
  }

  /** Return the package name for any example code */
  public String getPackageName() {
    return this.javaPackageField.getText();
  }

  /** Return the location where the project should be generated into */
  public IPath getLocationPath() {
    if (useDefaults()) {
      return workspaceLocation;
    }
    return new Path(locationField.getText());
  }

  private final class PageValidator implements ModifyListener {
    @Override
    public void modifyText(ModifyEvent event) {
      checkFlipToNext();
    }
  }

  /**
   * Auto-fills {@link #javaPackageField} from the Group ID if the user has not explicitly chosen a
   * package name.
   */
  private final class AutoPackageNameSetterOnGroupIdChange implements ModifyListener {

    @Override
    public void modifyText(ModifyEvent event) {
      // getGroupId() trims whitespace, so we do the same to sync with the dialog validation error.
      String groupId = groupIdField.getText().trim();

      if (MavenCoordinatesValidator.validateGroupId(groupId)) {
        String newSuggestion = suggestPackageName(groupId);
        updatePackageField(newSuggestion);
      } else if (groupId.isEmpty()) {
        updatePackageField(""); //$NON-NLS-1$
      }
    }
  }

  private void updatePackageField(String newSuggestion) {
    if (autoGeneratePackageName) {
      javaPackageProgrammaticUpdate = true;
      javaPackageField.setText(newSuggestion);
      javaPackageProgrammaticUpdate = false;
    }
  }

  /**
   * Helper function returning a suggested package name based on groupId.
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
