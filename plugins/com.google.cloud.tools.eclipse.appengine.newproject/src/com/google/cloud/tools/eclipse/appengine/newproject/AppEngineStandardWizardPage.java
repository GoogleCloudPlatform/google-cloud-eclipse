package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * UI to collect all information necessary to create a new App Engine Standard Java Eclipse project.
 */
public class AppEngineStandardWizardPage extends WizardPage implements IWizardPage {

  private Text javaPackageField;
  private Text eclipseProjectNameField;
  
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
    
    eclipseProjectNameField.forceFocus();
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
