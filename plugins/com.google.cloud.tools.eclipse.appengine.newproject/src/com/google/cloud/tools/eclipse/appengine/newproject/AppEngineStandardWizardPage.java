package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * UI to collect all information necessary to create a new App Engine Standard Java Eclipse project.
 */
class AppEngineStandardWizardPage extends WizardNewProjectCreationPage implements IWizardPage {

  private Text javaPackageField;
  private Text projectIdField;
  
  AppEngineStandardWizardPage() {
    super("basicNewProjectPage");
  }

  // todo is there a way to call this for a test?
  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
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
    
    // todo what to focus on with forceFocus
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
    if (!JavaPackageValidator.validate(packageName)) {
      setErrorMessage("Illegal Java package name: " + packageName);
      return false;
    }
    
    String projectId = projectIdField.getText();
    if (!AppEngineProjectIdValidator.validate(projectId)) {
      setErrorMessage("Illegal App Engine Project ID: " + projectId);
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

  String getAppEngineProjectId() {
    return this.projectIdField.getText();
  }

  String getPackageName() {
    return this.javaPackageField.getText();
  }
  
}
