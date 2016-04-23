package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
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

  // todo we need a model class that collects all the info for creating a project
  
  AppEngineStandardWizardPage(String pageName) {
    super(pageName);
    setPageComplete(false);
  }

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
    
    // todo enable browsing for Project IDs
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
    GridData gd3 = new GridData(GridData.FILL_HORIZONTAL);
    gd3.horizontalSpan = 2;
    projectDirectoryGroup.setLayoutData(gd3);

    GridLayout projectDirectoryLayout = new GridLayout();
    projectDirectoryLayout.numColumns = 3;
    projectDirectoryGroup.setLayout(projectDirectoryLayout);

    workspaceProjectDirectoryButton = new Button(projectDirectoryGroup, SWT.RADIO);
    workspaceProjectDirectoryButton.setText("Create new project in workspace");
    workspaceProjectDirectoryButton.setSelection(true);
    GridData gd4 = new GridData();
    gd4.horizontalAlignment = GridData.FILL;
    gd4.grabExcessHorizontalSpace = true;
    gd4.horizontalSpan = 3;
    workspaceProjectDirectoryButton.setLayoutData(gd4);

    customProjectDirectoryButton = new Button(projectDirectoryGroup, SWT.RADIO);
    customProjectDirectoryButton.setSelection(false); // not by default
    customProjectDirectoryButton.setText("Create new project in:");
    GridData gd5 = new GridData();
    gd5.horizontalAlignment = GridData.FILL;
    gd5.grabExcessHorizontalSpace = true;
    gd5.horizontalSpan = 3;
    customProjectDirectoryButton.setLayoutData(gd5);

    Label projectDirectoryLabel = new Label(projectDirectoryGroup, SWT.NONE);
    projectDirectoryLabel.setText("Directory:");

    // todo enable these next two depending on state of radio buttons
    projectDirectoryField = new Text(projectDirectoryGroup, SWT.BORDER);
    projectDirectoryField.setEnabled(false);
    GridData gd6 = new GridData();
    gd6.horizontalAlignment = GridData.FILL;
    gd6.grabExcessHorizontalSpace = true;
    projectDirectoryField.setLayoutData(gd6);

    projectDirectoryBrowseButton = new Button(projectDirectoryGroup, SWT.NONE);
    projectDirectoryBrowseButton.setEnabled(false);
    projectDirectoryBrowseButton.setText("Browse...");
  }
  
  private final class PageValidator implements ModifyListener {
    @Override
    public void modifyText(ModifyEvent event) {
      // todo more checks
      // todo add error messages
      boolean complete = javaPackageField.getText().trim().length() > 0 
          && eclipseProjectNameField.getText().trim().length() > 0;
      setPageComplete(complete);
    }
  }  
  
}
