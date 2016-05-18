package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectIdValidator;
import com.google.cloud.tools.eclipse.appengine.newproject.JavaPackageValidator;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import java.text.MessageFormat;

/**
 * UI to collect all information necessary to create a new Maven-based App Engine Standard Java
 * project.
 */
public class MavenAppEngineStandardWizardPage extends WizardPage implements IWizardPage {

  private String defaultVersion = "0.1.0-SNAPSHOT";

  private Button useDefaults;
  private Text locationField;
  private Button locationBrowseButton;
  private Text groupIdField;
  private Text artifactIdField;
  private Text versionField;
  private Text javaPackageField;
  private Text projectIdField;
  
  public MavenAppEngineStandardWizardPage() {
    super("basicNewProjectPage"); //$NON-NLS-1$
    setTitle("Maven-based App Engine Standard Project");
    setDescription("Create new Maven-based App Engine Standard Project");
    
    // todo get a UI designer to pick a better icon (the little plane?) and 
    // add it to this plugin's icons folder
    ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
        "com.google.cloud.tools.eclipse.appengine.localserver", "icons/gcp-32x32.png"); //$NON-NLS-1$ //$NON-NLS-2$
    this.setImageDescriptor(descriptor);

    setPageComplete(false);
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
    ModifyListener pageValidator = new PageValidator();
    
    /**** Location ****/
    Group locationGroup = new Group(container, SWT.NONE);
    locationGroup.setText("Location");
    GridDataFactory.fillDefaults().span(2, 1).applyTo(locationGroup);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(locationGroup);

    useDefaults = new Button(locationGroup, SWT.CHECK);
    GridDataFactory.defaultsFor(useDefaults).span(3, 1).applyTo(useDefaults);
    useDefaults.setText("Create project in workspace");
    useDefaults.setSelection(true);

    Label locationLabel = new Label(locationGroup, SWT.NONE);
    locationLabel.setText("Location:");
    locationLabel
        .setToolTipText("This location will contain the directory created for the project");

    locationField = new Text(locationGroup, SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false)
        .applyTo(locationField);
    locationField.addModifyListener(pageValidator);
    locationField.setEnabled(false);

    locationBrowseButton = new Button(locationGroup, SWT.PUSH);
    locationBrowseButton.setText("Browse");
    locationBrowseButton.setEnabled(false);
    locationBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        openLocationDialog();
      }
    });
    useDefaults.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        locationField.setEnabled(!useDefaults.getSelection());
        locationBrowseButton.setEnabled(!useDefaults.getSelection());
        checkPageComplete();
      }
    });
    

    /**** Maven Coordinates ****/
    Group mavenCoordinatesGroup = new Group(container, SWT.NONE);
    mavenCoordinatesGroup.setText("Maven project coordinates");
    GridDataFactory.defaultsFor(mavenCoordinatesGroup).span(2, 1).applyTo(mavenCoordinatesGroup);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(mavenCoordinatesGroup);

    Label groupIdLabel = new Label(mavenCoordinatesGroup, SWT.NONE);
    groupIdLabel.setText("Group id:"); //$NON-NLS-1$
    groupIdField = new Text(mavenCoordinatesGroup, SWT.BORDER);
    GridDataFactory.defaultsFor(groupIdField).align(SWT.FILL, SWT.CENTER)
        .applyTo(groupIdField);
    groupIdField.addModifyListener(pageValidator);

    Label artifactIdLabel = new Label(mavenCoordinatesGroup, SWT.NONE);
    artifactIdLabel.setText("Artifact id:"); //$NON-NLS-1$
    artifactIdField = new Text(mavenCoordinatesGroup, SWT.BORDER);
    GridDataFactory.defaultsFor(artifactIdField).align(SWT.FILL, SWT.CENTER)
        .applyTo(artifactIdField);
    artifactIdField.addModifyListener(pageValidator);

    Label versionLabel = new Label(mavenCoordinatesGroup, SWT.NONE);
    versionLabel.setText("Version:"); //$NON-NLS-1$
    versionField = new Text(mavenCoordinatesGroup, SWT.BORDER);
    versionField.setText(defaultVersion);
    GridDataFactory.defaultsFor(versionField).align(SWT.FILL, SWT.CENTER)
    .applyTo(versionField);
    versionField.addModifyListener(pageValidator);

    /**** App Engine project details ****/

    // Java package name
    Label packageNameLabel = new Label(container, SWT.NONE);
    packageNameLabel.setText("Java package:"); //$NON-NLS-1$
    javaPackageField = new Text(container, SWT.BORDER);
    GridData javaPackagePosition = new GridData(GridData.FILL_HORIZONTAL);
    javaPackagePosition.horizontalSpan = 2;
    javaPackageField.setLayoutData(javaPackagePosition);
    javaPackageField.addModifyListener(pageValidator);
    
    // App Engine Project ID
    Label projectIdLabel = new Label(container, SWT.NONE);
    projectIdLabel.setText("App Engine Project ID: (optional)"); //$NON-NLS-1$
    projectIdField = new Text(container, SWT.BORDER);
    GridData projectIdPosition = new GridData(GridData.FILL_HORIZONTAL);
    projectIdPosition.horizontalSpan = 2;
    projectIdField.setLayoutData(projectIdPosition);
    projectIdField.addModifyListener(pageValidator);
    
    setControl(container);
  }

  protected void openLocationDialog() {
    DirectoryDialog dialog = new DirectoryDialog(getShell());
    dialog.setText("Please select the location to contain generated project");
    String loc = dialog.open();
    if (loc != null) {
      locationField.setText(loc);
      checkPageComplete();
    }
  }

  protected void checkPageComplete() {
    setPageComplete(validatePage());
  }

  /**
   * Validate and report on the contents of this page
   * 
   * @return true if valid, false if there is a problem
   */
  public boolean validatePage() {
    setMessage(null);
    setErrorMessage(null);
    
    String location = locationField.getText().trim();
    if (!useDefaults() && location.isEmpty()) {
      setMessage("Please provide a location", INFORMATION);
      return false;
    }

    if (!validateMavenSettings()) {
      return false;
    }

    String artifactId = getArtifactId();
    if (!artifactId.isEmpty()) {
      IPath path = useDefaults() ? getWorkspace().getRoot().getLocation() : new Path(location);
      path = path.append(artifactId);
      // don't overwrite a location
      if (path.toFile().exists()) {
        setErrorMessage(MessageFormat.format("Location already exists: {0}", path));
        return false;
      }
    }

    String packageName = getPackageName();
    if (!JavaPackageValidator.validate(packageName)) {
      setErrorMessage("Illegal Java package name: " + packageName); //$NON-NLS-1$
      return false;
    }

    String projectId = getAppEngineProjectId();
    if (!AppEngineProjectIdValidator.validate(projectId)) {
      setErrorMessage("Illegal App Engine Project ID: " + projectId); //$NON-NLS-1$
      return false;
    }

    return true;
  }

  private boolean validateMavenSettings() {
    String groupId = getGroupId();
    if (groupId.isEmpty()) {
      setMessage("Please provide Maven group id", INFORMATION);
      return false;
    } else if (!MavenCoordinatesValidator.validateGroupId(groupId)) {
      setErrorMessage("Illegal group id: " + groupId);
      return false;
    }
    String artifactId = getArtifactId();
    if (artifactId.isEmpty()) {
      setMessage("Please provide Maven artifact id", INFORMATION);
      return false;
    } else if (!MavenCoordinatesValidator.validateArtifactId(artifactId)) {
      setErrorMessage("Illegal artifact id: " + artifactId);
      return false;
    }
    String version = getVersion();
    if (version.isEmpty()) {
      setMessage("Please provide Maven artifact version", INFORMATION);
      return false;
    } else if (!MavenCoordinatesValidator.validateVersion(version)) {
      setErrorMessage("Illegal version: " + version);
      return false;
    }
    return true;
  }

  private IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
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
   * If true, projects are to be generated into the workspace, otherwise placed into a specified
   * location.
   */
  public boolean useDefaults() {
    return useDefaults.getSelection();
  }

  /** Return the App Engine project id (if any) */
  public String getAppEngineProjectId() {
    return this.projectIdField.getText();
  }

  /** Return the package name for any example code */
  public String getPackageName() {
    return this.javaPackageField.getText();
  }

  /** Return the location where the project should be generated into */
  public IPath getLocationPath() {
    if (useDefaults()) {
      return ResourcesPlugin.getWorkspace().getRoot().getLocation();
    }
    return new Path(locationField.getText());
  }

  private final class PageValidator implements ModifyListener {
    @Override
    public void modifyText(ModifyEvent event) {
      checkPageComplete();
    }
  }
}
