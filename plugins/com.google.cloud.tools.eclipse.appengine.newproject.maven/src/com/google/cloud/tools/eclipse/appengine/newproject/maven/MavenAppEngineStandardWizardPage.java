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

  private Button useDefaults;
  private Text locationField;
  private Button locationBrowseButton;
  private Text javaPackageField;
  private LibrarySelectorGroup appEngineLibrariesSelectorGroup;
  private MavenCoordinatesUi mavenCoordinatesUi;

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

    GridLayoutFactory.swtDefaults().generateLayout(container);
    Dialog.applyDialogFont(container);
  }

  /** Create UI for specifying the generated location area */
  private void createLocationArea(Composite container, ModifyListener pageValidator) {

    Group locationGroup = new Group(container, SWT.NONE);
    locationGroup.setText(Messages.getString("LOCATION_GROUP_TEXT")); //$NON-NLS-1$

    useDefaults = new Button(locationGroup, SWT.CHECK | SWT.LEAD);
    useDefaults.setText(Messages.getString("CREATE_PROJECT_IN_WORKSPACE")); //$NON-NLS-1$
    useDefaults.setSelection(true);

    Label locationLabel = new Label(locationGroup, SWT.LEAD);
    locationLabel.setText(Messages.getString("LOCATION_LABEL")); //$NON-NLS-1$
    locationLabel.setToolTipText(Messages.getString("LOCATION_TOOL_TIP")); //$NON-NLS-1$

    locationField = new Text(locationGroup, SWT.BORDER);
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

    GridDataFactory.defaultsFor(useDefaults).span(3, 1).applyTo(useDefaults);
    GridLayoutFactory.swtDefaults().numColumns(3).generateLayout(locationGroup);
  }

  /** Create UI for specifying desired Maven Coordinates */
  private void createMavenCoordinatesArea(Composite container, ModifyListener pageValidator) {
    mavenCoordinatesUi = new MavenCoordinatesUi(container, false /* no dynamic enabling */);
    mavenCoordinatesUi.addModifyListener(pageValidator);
    mavenCoordinatesUi.addGroupIdModifyListener(new AutoPackageNameSetterOnGroupIdChange());
  }

  /** Create UI for specifying App Engine project details */
  private void createAppEngineProjectDetailsArea(Composite container,
      ModifyListener pageValidator) {
    Composite composite = new Composite(container, SWT.NONE);

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

    GridLayoutFactory.swtDefaults().numColumns(2).generateLayout(composite);
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
    if (!mavenCoordinatesUi.setValidationMessage(this)) {
      return false;
    } else if (ResourcesPlugin.getWorkspace().getRoot().getProject(getArtifactId()).exists()) {
      setErrorMessage(Messages.getString("PROJECT_ALREADY_EXISTS", getArtifactId())); //$NON-NLS-1$
      return false;
    }
    return true;
  }

  private boolean validateAppEngineProjectDetails() {
    String packageName = getPackageName();
    if (packageName.isEmpty()) {
      String message = Messages.getString("EMPTY_PACKAGE_NAME"); //$NON-NLS-1$
      setErrorMessage(message);
      return false;
    }

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
    return mavenCoordinatesUi.getGroupId();
  }

  /** Return the Maven artifact for the project */
  public String getArtifactId() {
    return mavenCoordinatesUi.getArtifactId();
  }

  /** Return the Maven version for the project */
  public String getVersion() {
    return mavenCoordinatesUi.getVersion();
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
      String groupId = getGroupId();

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
