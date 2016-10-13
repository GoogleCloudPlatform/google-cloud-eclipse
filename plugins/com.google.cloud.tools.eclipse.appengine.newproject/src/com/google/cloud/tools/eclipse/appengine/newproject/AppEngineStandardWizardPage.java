package com.google.cloud.tools.eclipse.appengine.newproject;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import com.google.cloud.tools.eclipse.appengine.libraries.Library;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.ui.util.databinding.BooleanConverter;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.project.ProjectIdValidator;

/**
 * UI to collect all information necessary to create a new App Engine Standard Java Eclipse project.
 */
public class AppEngineStandardWizardPage extends WizardNewProjectCreationPage {

  private Text javaPackageField;
  private Text projectIdField;
  private Group apiGroup;
  private List<Button> libraryButtons = new LinkedList<>();
  private DataBindingContext bindingContext;

  public AppEngineStandardWizardPage() {
    super("basicNewProjectPage"); //$NON-NLS-1$
    // todo instead of hard coding strings, read the wizard.name and wizard.description properties
    // from plugins/com.google.cloud.tools.eclipse.appengine.newproject/plugin.properties
    this.setTitle("App Engine Standard Project");
    this.setDescription("Create a new App Engine Standard Project in the workspace.");

    this.setImageDescriptor(AppEngineImages.googleCloudPlatform(32));
  }

  // todo is there a way to call this for a test?
  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE, parent.getShell());

    Composite container = (Composite) getControl();

    ModifyListener pageValidator = new PageValidator();

    // Java package name
    Label packageNameLabel = new Label(container, SWT.NONE);
    packageNameLabel.setText("Java package:");
    javaPackageField = new Text(container, SWT.BORDER);
    GridData javaPackagePosition = new GridData(GridData.FILL_HORIZONTAL);
    javaPackagePosition.horizontalSpan = 2;
    javaPackageField.setLayoutData(javaPackagePosition);
    javaPackageField.addModifyListener(pageValidator);

    // App Engine Project ID
    Label projectIdLabel = new Label(container, SWT.NONE);
    projectIdLabel.setText("App Engine Project ID: (optional)");
    projectIdField = new Text(container, SWT.BORDER);
    GridData projectIdPosition = new GridData(GridData.FILL_HORIZONTAL);
    projectIdPosition.horizontalSpan = 2;
    projectIdField.setLayoutData(projectIdPosition);
    projectIdField.addModifyListener(pageValidator);

    // Manage APIs
    addManageLibrariesWidgets(container);

    Dialog.applyDialogFont(container);
  }

  private void addManageLibrariesWidgets(Composite container) {
    apiGroup = new Group(container, SWT.NONE);
    apiGroup.setText(Messages.AppEngineStandardWizardPage_librariesGroupLabel);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(apiGroup);

    List<Library> libraries = getLibraries();
    for (Library library : libraries) {
      Button libraryButton = new Button(apiGroup, SWT.CHECK);
      libraryButton.setText(library.getId());
      libraryButton.setData(library);
      libraryButtons.add(libraryButton);
    }

    addDatabindingForDependencies(libraries);

    GridLayoutFactory.fillDefaults().applyTo(apiGroup);
  }

  private void addDatabindingForDependencies(List<Library> libraries) {
    bindingContext = new DataBindingContext();
    for (Button libraryButton : libraryButtons) {
      if (!((Library) libraryButton.getData()).getLibraryDependencies().isEmpty()) {
        addDatabindingForDependencies(libraryButton);
      }
    }
  }

  private void addDatabindingForDependencies(Button libraryButton) {
    for (String libraryId : ((Library) libraryButton.getData()).getLibraryDependencies()) {
      Button dependencyButton = getButtonForLibraryId(libraryId);
      if (dependencyButton != null) {
        ISWTObservableValue libraryButtonSelection = WidgetProperties.selection().observe(libraryButton);
        IObservableValue dependencyButtonSelection =
            PojoProperties.value(Button.class, "selection").observe(getDisplayRealm(), dependencyButton);
        IObservableValue dependencyButtonEnablement =
            PojoProperties.value(Button.class, "enabled").observe(getDisplayRealm(), dependencyButton);

        WritableValue intermediate = new WritableValue(false, Boolean.class);
        bindingContext.bindValue(libraryButtonSelection, intermediate);
        bindingContext.bindValue(dependencyButtonSelection, intermediate);
        bindingContext.bindValue(dependencyButtonEnablement, intermediate,
                                 new UpdateValueStrategy().setConverter(BooleanConverter.negate()),
                                 new UpdateValueStrategy().setConverter(BooleanConverter.negate()));
      }
    }
  }

  private Realm getDisplayRealm() {
    return DisplayRealm.getRealm(getControl().getDisplay());
  }

  private Button getButtonForLibraryId(String libraryId) {
    for (Button button : libraryButtons) {
      if (((Library) button.getData()).getId().equals(libraryId)) {
        return button;
      }
    }
    return null;
  }

  // mock method until the libraries are defined via an extension point
  private List<Library> getLibraries() {
    Library appEngine = new Library("appengine-api");
    appEngine.setName("App Engine API");
    Library endpoints = new Library("appengine-endpoints");
    endpoints.setName("App Engine Endpoints");
    endpoints.setLibraryDependencies(Collections.singletonList("appengine-api"));
    return Arrays.asList(appEngine, endpoints);
  }

  @Override
  public boolean validatePage() {
    if (!super.validatePage()) {
      return false;
    }

    return validateLocalFields();
  }

  private boolean validateLocalFields() {
    String packageName = javaPackageField.getText();
    IStatus packageStatus = JavaPackageValidator.validate(packageName);
    if (!packageStatus.isOK()) {
      setErrorMessage("Illegal package name: " + packageStatus.getMessage());
      return false;
    }

    String projectId = projectIdField.getText();
    if (!projectId.isEmpty()) {
      if (!ProjectIdValidator.validate(projectId)) {
        setErrorMessage("Illegal App Engine Project ID: " + projectId);
        return false;
      }
    }

    File parent = getLocationPath().toFile();
    File projectDirectory = new File(parent, getProjectName());
    if (projectDirectory.exists()) {
      setErrorMessage("Project location already exists: " + projectDirectory);
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

  public String getAppEngineProjectId() {
    return this.projectIdField.getText();
  }

  public String getPackageName() {
    return this.javaPackageField.getText();
  }

  public List<Library> getSelectedLibraries() {
    List<Library> selected = new LinkedList<>();
    for (Button button : libraryButtons) {
      if (button.getSelection()) {
        selected.add((Library) button.getData());
      }
    }
    return selected;
  }

}
