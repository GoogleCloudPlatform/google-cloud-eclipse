/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.page;

import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowDependencyManager;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowProjectCreator;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowProjectCreator.Template;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowProjectValidationStatus;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.cloud.tools.eclipse.dataflow.ui.util.ButtonFactory;
import com.google.common.base.Strings;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import java.io.File;
import java.net.URI;
import java.util.Map;

/**
 * The landing page for the New Cloud Dataflow Project wizard.
 */
public class NewDataflowProjectWizardLandingPage extends WizardPage  {
  private static final String EXAMPLE_GROUP_ID = "com.company.product";

  private DataflowDependencyManager dependencyManager;
  private final DataflowProjectCreator targetCreator;

  private Text groupIdInput;
  private Text artifactIdInput;
  private Text packageInput;

  private Button useDefaultLocation;
  private Text locationInput;
  private Button locationBrowse;

  private Combo templateDropdown;
  private Combo templateVersionDropdown;
  private Combo projectNameTemplate;

  public NewDataflowProjectWizardLandingPage(DataflowProjectCreator targetCreator) {
    super("newDataflowProjectWizardLandingPage");
    this.dependencyManager = DataflowDependencyManager.create();
    this.targetCreator = targetCreator;
    setTitle(Messages.getString("CREATE_DATAFLOW_PROJECT"));
    setDescription(Messages.getString("WIZARD_DESCRIPTION"));
    setImageDescriptor(getDataflowIcon());
    setPageComplete(false);
  }

  private static ImageDescriptor getDataflowIcon() {
    String imageFilePath = "icons/Dataflow_64.png";
    return AbstractUIPlugin.imageDescriptorFromPlugin(
        "com.google.cloud.tools.eclipse.dataflow.ui", imageFilePath);
  }
  
  private static void addLabel(Composite formComposite, String labelText) {
    Label label = new Label(formComposite, SWT.NULL);
    label.setText(labelText);
  }

  private static Text addLabeledText(Composite formComposite, String labelText) {
    addLabel(formComposite, labelText);

    Text widget = new Text(formComposite, SWT.SINGLE | SWT.BORDER);
    widget.setLayoutData(gridSpan(GridData.FILL_HORIZONTAL, 2));
    return widget;
  }

  private static Button addCheckbox(Composite formComposite, String labelText, boolean initialValue) {
    Button checkbox = new Button(formComposite, SWT.CHECK);
    checkbox.setText(labelText);
    checkbox.setLayoutData(gridSpan(SWT.NULL, 3));

    checkbox.setSelection(initialValue);
    return checkbox;
  }

  private static Combo addCombo(Composite formComposite, String labelText, boolean readOnly) {
    addLabel(formComposite, labelText);

    Combo combo = new Combo(formComposite,
        SWT.DROP_DOWN | (readOnly ? SWT.READ_ONLY : SWT.NULL));
    combo.setLayoutData(gridSpan(GridData.FILL_HORIZONTAL, 2));
    return combo;
  }

  private static GridData gridSpan(int style, int span) {
    GridData gridData = new GridData(style);
    gridData.horizontalSpan = span;
    return gridData;
  }

  @Override
  public void createControl(Composite parent) {
    Composite formComposite = new Composite(parent, SWT.NULL);
    formComposite.setLayout(new GridLayout(3, false));
    setControl(formComposite);

    groupIdInput = addLabeledText(formComposite, "&Group ID:");
    groupIdInput.setMessage(EXAMPLE_GROUP_ID);
    groupIdInput.setToolTipText(Messages.getString("GROUP_ID_TOOLTIP"));
    artifactIdInput = addLabeledText(formComposite, "&Artifact ID:");
    artifactIdInput.setToolTipText(Messages.getString("ARTIFACT_ID_TOOLTIP"));

    templateDropdown = addCombo(formComposite, "Project &Template:", true);
    for (Template template : Template.values()) {
      templateDropdown.add(template.getLabel());
    }
    templateVersionDropdown = addCombo(formComposite, "Project Dataflow &Version", false);

    templateDropdown.select(0);
    updateAvailableVersions();

    packageInput = addLabeledText(formComposite, "&Package:");
    packageInput.setToolTipText(Messages.getString("UNSET_PACKAGE_TOOLTIP"));
    packageInput.setMessage(EXAMPLE_GROUP_ID);

    // Add a labeled text and button for the default location.
    Group locationGroup = new Group(formComposite, SWT.NULL);
    locationGroup.setLayoutData(gridSpan(GridData.FILL_HORIZONTAL, 3));
    locationGroup.setLayout(new GridLayout(3, false));

    useDefaultLocation = addCheckbox(locationGroup, "Use default &Workspace location", true);

    addLabel(locationGroup, "&Location:");

    String defaultLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
    locationInput = new Text(locationGroup, SWT.SINGLE | SWT.BORDER);
    locationInput.setText(defaultLocation);
    locationInput.setEnabled(false);
    locationInput.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    locationInput.setToolTipText(Messages.getString("LOCATION_TOOLTIP"));

    locationBrowse = ButtonFactory.newPushButton(locationGroup, "&Browse");
    locationBrowse.setEnabled(false);

    projectNameTemplate = addCombo(formComposite, "Name &template:", false);
    projectNameTemplate.setToolTipText(
        "Optional Eclipse project name template such as [groupId]-[artifactId].");
    projectNameTemplate.add("[artifactId]");
    projectNameTemplate.add("[groupId]-[artifactId]");
    projectNameTemplate.setLayoutData(gridSpan(GridData.FILL_HORIZONTAL, 1));

    // Register all the listeners
    addListeners(defaultLocation);

    formComposite.layout();
    parent.layout();
  }

  private void validateAndSetError() {
    if (targetCreator.isValid()) {
      setErrorMessage(null);
      setPageComplete(true);
      return;
    } else {
      for (DataflowProjectValidationStatus status : targetCreator.validate()) {
        if (!status.isValid()) {
          setErrorMessage(status.getMessage());
          break;
        }
      }
    }
    setPageComplete(false);
  }

  private void addListeners(String defaultProjectLocation) {
    // When default location is selected, overwrite the input with the defaultProjectLocation
    useDefaultLocation.addSelectionListener(
        showDefaultOrCustomValueListener(defaultProjectLocation));

    // When the Browse button is pressed, open a directory selection dialogue
    locationBrowse.addSelectionListener(folderSelectionListener(getShell()));

    // Updating the group ID updates the the default package name
    ModifyListener propagateGroupIdToPackageListener = propagateGroupIdToPackageListener();
    groupIdInput.addModifyListener(propagateGroupIdToPackageListener);
    packageInput.addFocusListener(
        changeGroupIdPropogationListener(propagateGroupIdToPackageListener));

    // When the project inputs are modified, validate them and update the error message
    locationInput.addModifyListener(validateAndSetProjectLocationListener());
    groupIdInput.addModifyListener(validateAndSetMavenGroupIdListener());
    artifactIdInput.addModifyListener(validateAndSetMavenArtifactIdListener());
    packageInput.addModifyListener(validateAndSetPackageListener());
    projectNameTemplate.addModifyListener(setProjectNameTemplate());

    templateDropdown.addSelectionListener(templateListener());
    templateVersionDropdown.addModifyListener(customTemplateVersionListener());
    templateVersionDropdown.addSelectionListener(templateVersionListener());
  }

  private ModifyListener setProjectNameTemplate() {
    return new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        targetCreator.setProjectNameTemplate(projectNameTemplate.getText());
      }
    };
  }

  private ModifyListener propagateGroupIdToPackageListener() {
    return new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        packageInput.setText(groupIdInput.getText());
      }
    };
  }

  private FocusListener changeGroupIdPropogationListener(final ModifyListener propogationListener) {
    return new FocusListener() {
      @Override
      public void focusLost(FocusEvent event) {
        if (Strings.isNullOrEmpty(packageInput.getText())
            || packageInput.getText().equals(groupIdInput.getText())) {
          packageInput.setText(groupIdInput.getText());
          groupIdInput.addModifyListener(propogationListener);
        }
      }

      @Override
      public void focusGained(FocusEvent event) {
        groupIdInput.removeModifyListener(propogationListener);
      }
    };
  }

  private SelectionListener templateListener() {
    return new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        targetCreator.setTemplate(Template.values()[templateDropdown.getSelectionIndex()]);
        updateAvailableVersions();
      }
    };
  }

  /**
   * Update the dropdown of available versions to versions that are available for the currently
   * selected Template. If the available versions for the currently selected template does not
   * include the currently selected version, select the latest stable version; if none is available,
   * select the latest version.
   */
  private void updateAvailableVersions() {
    String selected = templateVersionDropdown.getText();
    templateVersionDropdown.removeAll();

    Template template = Template.values()[templateDropdown.getSelectionIndex()];
    Map<ArtifactVersion, MajorVersion> availableVersions =
        dependencyManager.getLatestVersions(template.getSdkVersions());

    // If there is a previously selected version that is available, select it. Otherwise, if there
    // is a stable version available, select the most recent. Otherwise, select the latest version.
    int latestStableVersionIndex = availableVersions.size() - 1;
    boolean selectedVersionExists = false;
    for (Map.Entry<ArtifactVersion, MajorVersion> version : availableVersions.entrySet()) {
      templateVersionDropdown.add(version.getKey().toString());
      int index = templateVersionDropdown.getItemCount() - 1;
      if (version.getKey().toString().equals(selected)) {
        templateVersionDropdown.select(index);
        selectedVersionExists = true;
      } else if (version.getValue().hasStableApi()) {
        latestStableVersionIndex = index;
      }
    }
    if (!selectedVersionExists) {
      templateVersionDropdown.select(latestStableVersionIndex);
    }
    updateArchetypeVersion();
  }

  private ModifyListener customTemplateVersionListener() {
    return new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent modifyEvent) {
        updateArchetypeVersion();
      }
    };
  }

  private SelectionListener templateVersionListener() {
    return new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        updateArchetypeVersion();
      }
    };
  }

  private void updateArchetypeVersion() {
    targetCreator.setArchetypeVersion(templateVersionDropdown.getText());
    validateAndSetError();
  }

  private SelectionListener showDefaultOrCustomValueListener(final String defaultValue) {
    return new SelectionAdapter() {
      private String customValue = "";

      @Override
      public void widgetSelected(SelectionEvent event) {
        boolean customLocation = !useDefaultLocation.getSelection();

        // Update the targetCreator
        targetCreator.setCustomLocation(customLocation);

        // Enable/disable the location inputs as appropriate
        locationBrowse.setEnabled(customLocation);
        locationInput.setEnabled(customLocation);

        // Capture the current customValue if we're disabling custom values.
        if (!customLocation) {
          customValue = locationInput.getText();
        }

        // Update the locationInput box
        locationInput.setText(customLocation ? customValue : defaultValue);
      }
    };
  }

  private ModifyListener validateAndSetMavenArtifactIdListener() {
    return new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        targetCreator.setMavenArtifactId(artifactIdInput.getText());
        validateAndSetError();
      }
    };
  }
  private ModifyListener validateAndSetMavenGroupIdListener() {
    return new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        targetCreator.setMavenGroupId(groupIdInput.getText());
        validateAndSetError();
      }
    };
  }

  private ModifyListener validateAndSetProjectLocationListener() {
    return new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        String locationInputString = locationInput.getText();
        if (Strings.isNullOrEmpty(locationInputString)) {
          targetCreator.setProjectLocation(null);
          validateAndSetError();
        } else {
          File file = new File(locationInputString);
          URI location = file.toURI();
          targetCreator.setProjectLocation(location);
          validateAndSetError();
        }
      }
    };
  }

  private ModifyListener validateAndSetPackageListener() {
    return new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent event) {
        String packageInputString = packageInput.getText();
        targetCreator.setPackage(packageInputString);
        validateAndSetError();
      }
    };
  }

  /**
   * A listener that opens a folder selection dialog when the button is pressed and sets the text
   * when the dialog was closed if a selection was made.
   */
  private SelectionListener folderSelectionListener(final Shell shell) {
    return new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        DirectoryDialog dialog = new DirectoryDialog(shell);
        dialog.setMessage(Messages.getString("SELECT_PROJECT_LOCATION"));
        String result = dialog.open();
        if (!Strings.isNullOrEmpty(result)) {
          locationInput.setText(result);
        }
      }
    };
  }

  public boolean isDefaultLocation() {
    return useDefaultLocation.getSelection();
  }

  public URI getProjectLocation() {
    if (isDefaultLocation()) {
      return null;
    }
    return URI.create(locationInput.getText());
  }

  public String getGroupId() {
    return groupIdInput.getText();
  }

  public String getArtifactId() {
    return artifactIdInput.getText();
  }
}
