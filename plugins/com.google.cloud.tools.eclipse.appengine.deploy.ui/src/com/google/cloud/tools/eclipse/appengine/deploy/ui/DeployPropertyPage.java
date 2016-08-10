package com.google.cloud.tools.eclipse.appengine.deploy.ui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.cloud.tools.eclipse.ui.util.FontUtil;

public class DeployPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

  private static final int INDENT_CHECKBOX_ENABLED_WIDGET = 10;
  private Button promptForProjectIdButton;
  private Label projectIdLabel;
  private Text projectId;
  private Button browseProjectButton;
  
  private Button overrideDefaultVersionButton;
  private Label versionLabel;
  private Text version;
  
  private Button autoPromoteButton;
  
  private Button overrideDefaultBucketButton;
  private Label bucketLabel;
  private Text bucket;
  private Button browseBucketButton;
  

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    createProjectIdSection(container);

    createProjectVersionSection(container);

    createPromoteSection(container);

    ExpandBar expandBar = createCollapsibleAdvancedSection(container);

    Composite defaultBucketComp = createBucketSection(expandBar);

    addBucketSectionToAdvancedSettings(expandBar, defaultBucketComp);
    
    return container;
  }

  private void createProjectIdSection(Composite parent) {
    Composite projectIdComp = new Composite(parent, SWT.NONE);
    projectIdComp.setLayout(new GridLayout(3, false));
    projectIdComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    promptForProjectIdButton = new Button(projectIdComp, SWT.CHECK);
    promptForProjectIdButton.setText(Messages.getString("deploy.prompt.projectid"));
    promptForProjectIdButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
    promptForProjectIdButton.addSelectionListener(new SelectionListener() {
      
      @Override
      public void widgetSelected(SelectionEvent e) {
        enableProjectIdWidgets();
      }
      
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        enableProjectIdWidgets();
      }
    });
    
    projectIdLabel = new Label(projectIdComp, SWT.LEFT);
    GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    layoutData.horizontalIndent = INDENT_CHECKBOX_ENABLED_WIDGET;
    projectIdLabel.setLayoutData(layoutData);
    projectIdLabel.setText(Messages.getString("project.id"));
  
    projectId = new Text(projectIdComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    projectId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    browseProjectButton = new Button(projectIdComp, SWT.PUSH);
    browseProjectButton.setText("...");
    browseProjectButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
  
    selectProjectIdButton();
  }

  private void createProjectVersionSection(Composite parent) {
    Composite versionComp = new Composite(parent, SWT.NONE);
    versionComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    versionComp.setLayout(new GridLayout(2, false));
  
    overrideDefaultVersionButton = new Button(versionComp, SWT.CHECK);
    overrideDefaultVersionButton.setText(Messages.getString("use.custom.versioning"));
    overrideDefaultVersionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
  
    versionLabel = new Label(versionComp, SWT.NONE);
    versionLabel.setText(Messages.getString("project.version"));
    GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    layoutData.horizontalIndent = INDENT_CHECKBOX_ENABLED_WIDGET;
    versionLabel.setLayoutData(layoutData);
  
    version = new Text(versionComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    version.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    overrideDefaultVersionButton.addSelectionListener(new SelectionListener() {
      
      @Override
      public void widgetSelected(SelectionEvent e) {
        enableVersionWidgets();
      }
      
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        enableVersionWidgets();
      }
    });
  
    selectVersionButton();
  }

  private void createPromoteSection(Composite parent) {
    Composite promoteComp = new Composite(parent, SWT.NONE);
    promoteComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    promoteComp.setLayout(new GridLayout(1, false));
    autoPromoteButton = new Button(promoteComp, SWT.CHECK);
    autoPromoteButton.setText(Messages.getString("auto.promote"));
    autoPromoteButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    
    Link manualPromoteLabel = new Link(promoteComp, SWT.NONE);
    manualPromoteLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    manualPromoteLabel.setText(Messages.getString("deploy.manual.link", "https://console.developers.google.com/appengine"));
  }

  private ExpandBar createCollapsibleAdvancedSection(Composite parent) {
    ExpandBar expandBar = new ExpandBar(parent, SWT.V_SCROLL);
    expandBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    FontUtil.convertFontToBold(expandBar);
    return expandBar;
  }

  private Composite createBucketSection(Composite parent) {
    Composite defaultBucketComp = new Composite(parent, SWT.NONE);
    defaultBucketComp.setLayout(new GridLayout(1, true));
    
    overrideDefaultBucketButton = new Button(defaultBucketComp, SWT.CHECK);
    overrideDefaultBucketButton.setText(Messages.getString("use.custom.bucket"));
    overrideDefaultBucketButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    overrideDefaultBucketButton.addSelectionListener(new SelectionListener() {
      
      @Override
      public void widgetSelected(SelectionEvent e) {
        enableCustomBucketWidgets();
      }
      
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        enableCustomBucketWidgets();
      }
    });
  
    Composite customBucketComp = new Composite(defaultBucketComp, SWT.NONE);
    customBucketComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    customBucketComp.setLayout(new GridLayout(3, false));
    
    bucketLabel = new Label(customBucketComp, SWT.RADIO);
    bucketLabel.setText(Messages.getString("bucket.name"));
    bucketLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
  
    bucket = new Text(customBucketComp, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    bucket.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    browseBucketButton = new Button(customBucketComp, SWT.PUSH);
    browseBucketButton.setText("...");
    browseBucketButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
  
    selectBucketButton();
    
    return defaultBucketComp;
  }

  private void addBucketSectionToAdvancedSettings(ExpandBar parent, Composite child) {
    ExpandItem expandItem = new ExpandItem(parent, SWT.NONE, 0);
    expandItem.setText(Messages.getString("settings.advanced"));
    expandItem.setHeight(child.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
    expandItem.setControl(child);
  }

  private void selectProjectIdButton() {
    promptForProjectIdButton.setSelection(true);
    enableProjectIdWidgets();
  }

  private void enableProjectIdWidgets() {
    projectIdLabel.setEnabled(!promptForProjectIdButton.getSelection());
    projectId.setEnabled(!promptForProjectIdButton.getSelection());
    browseProjectButton.setEnabled(!promptForProjectIdButton.getSelection());
  }

  private void selectVersionButton() {
    overrideDefaultVersionButton.setSelection(false);
    enableVersionWidgets();
  }

  private void enableVersionWidgets() {
    version.setEnabled(overrideDefaultVersionButton.getSelection());
    versionLabel.setEnabled(overrideDefaultVersionButton.getSelection());
  }

  private void selectBucketButton() {
    overrideDefaultBucketButton.setSelection(false);
    enableCustomBucketWidgets();
  }

  private void enableCustomBucketWidgets() {
    bucket.setEnabled(overrideDefaultBucketButton.getSelection());
    bucketLabel.setEnabled(overrideDefaultBucketButton.getSelection());
    browseBucketButton.setEnabled(overrideDefaultBucketButton.getSelection());
  }

}
