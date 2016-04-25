package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * UI to collect all information necessary to create a new App Engine Standard Java Eclipse project.
 */
public class AppEngineStandardWizardPage extends WizardPage implements IWizardPage {

  private Text javaPackageField;
  private Text eclipseProjectNameField;
  private Text projectIdField;
  
  private Button workspaceProjectDirectoryButton;
  private Button customProjectDirectoryButton;
  private Text projectDirectoryField;
  private Button projectDirectoryBrowseButton;
  
  AppEngineStandardWizardPage(String pageName) {
    super(pageName);
    setPageComplete(false);
  }

  // todo is there a way to call this for a test?
  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout gridLayout = new GridLayout();
    container.setLayout(gridLayout);
    setControl(container);
    
    // Eclipse project name
    Label projectNameLabel = new Label(container, SWT.NONE);
    projectNameLabel.setText("Eclipse project name:");
    
    eclipseProjectNameField = new Text(container, SWT.BORDER);
    GridData projectNamePosition = new GridData(GridData.FILL_HORIZONTAL);
    projectNamePosition.horizontalSpan = 2;
    eclipseProjectNameField.setLayoutData(projectNamePosition);
    ModifyListener pageValidator = new PageValidator();
    eclipseProjectNameField.addModifyListener(pageValidator);
    
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
    
    addLocationWidgets(container);

    eclipseProjectNameField.forceFocus();
  }

  // UI to choose location of Eclipse project on local file system
  // todo should we pull this out into a separate class?
  private void addLocationWidgets(Composite container) {
    // Eclipse project directory (defaults to subdirectory under workspace)
    Group projectDirectoryGroup = new Group(container, SWT.NULL);
    projectDirectoryGroup.setText("Location");
    GridData groupPosition = new GridData(GridData.FILL_HORIZONTAL);
    groupPosition.horizontalSpan = 2;
    projectDirectoryGroup.setLayoutData(groupPosition);

    GridLayout projectDirectoryLayout = new GridLayout();
    projectDirectoryLayout.numColumns = 3;
    projectDirectoryGroup.setLayout(projectDirectoryLayout);

    workspaceProjectDirectoryButton = new Button(projectDirectoryGroup, SWT.RADIO);
    workspaceProjectDirectoryButton.setText("Create new project in workspace");
    workspaceProjectDirectoryButton.setSelection(true);
    SelectionListener directorySwitcher = new DirectorySwitcher();
    workspaceProjectDirectoryButton.addSelectionListener(directorySwitcher);
    GridData workspaceProjectDirectoryButtonPosition = new GridData();
    workspaceProjectDirectoryButtonPosition.horizontalAlignment = GridData.FILL;
    workspaceProjectDirectoryButtonPosition.grabExcessHorizontalSpace = true;
    workspaceProjectDirectoryButtonPosition.horizontalSpan = 3;
    workspaceProjectDirectoryButton.setLayoutData(workspaceProjectDirectoryButtonPosition);

    customProjectDirectoryButton = new Button(projectDirectoryGroup, SWT.RADIO);
    customProjectDirectoryButton.setSelection(false); // not by default
    customProjectDirectoryButton.setText("Create new project in:");
    customProjectDirectoryButton.addSelectionListener(directorySwitcher);
    GridData customProjectDirectoryButtonPosition = new GridData();
    customProjectDirectoryButtonPosition.horizontalAlignment = GridData.FILL;
    customProjectDirectoryButtonPosition.grabExcessHorizontalSpace = true;
    customProjectDirectoryButtonPosition.horizontalSpan = 3;
    customProjectDirectoryButton.setLayoutData(customProjectDirectoryButtonPosition);

    Label projectDirectoryLabel = new Label(projectDirectoryGroup, SWT.NONE);
    projectDirectoryLabel.setText("Directory:");

    projectDirectoryField = new Text(projectDirectoryGroup, SWT.BORDER);
    projectDirectoryField.setEnabled(false);
    GridData projectDirectoryFieldPosition = new GridData();
    projectDirectoryFieldPosition.horizontalAlignment = GridData.FILL;
    projectDirectoryFieldPosition.grabExcessHorizontalSpace = true;
    projectDirectoryField.setLayoutData(projectDirectoryFieldPosition);

    projectDirectoryBrowseButton = new Button(projectDirectoryGroup, SWT.NONE);
    projectDirectoryBrowseButton.setEnabled(false);
    projectDirectoryBrowseButton.setText("Browse...");
    projectDirectoryBrowseButton.addSelectionListener(new ProjectDirectoryPicker());
  }
  
  private final class ProjectDirectoryPicker extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent event) {
      DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
      dialog.setMessage("Choose a directory for the project contents:");
      String userChoice = dialog.open();
      if (userChoice != null) {
        projectDirectoryField.setText(userChoice);
      }
    }
  }

  private final class PageValidator implements ModifyListener {
    @Override
    public void modifyText(ModifyEvent event) {
      // todo more checks
      // todo add error messages
      boolean complete = JavaPackageValidator.validate(javaPackageField.getText()) 
          && eclipseProjectNameField.getText().trim().length() > 0;
      setPageComplete(complete);
    }
  }  
  
  private final class DirectorySwitcher extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent event) {
      projectDirectoryBrowseButton.setEnabled(event.widget == customProjectDirectoryButton);
      projectDirectoryField.setEnabled(event.widget == customProjectDirectoryButton);
    }
  }

  AppEngineStandardProjectConfig getAppEngineStandardProjectConfig() {
    AppEngineStandardProjectConfig projectConfig = new AppEngineStandardProjectConfig();
    projectConfig.setEclipseProjectName(eclipseProjectNameField.getText());
    projectConfig.setPackageName(javaPackageField.getText());
    projectConfig.setAppEngineProjectId(this.projectIdField.getText());
    // todo: directory
    return projectConfig;
  }
  
}
