package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class MavenProjectWizardSupport {

  private static final String DEFAULT_VERSION = "0.1.0-SNAPSHOT"; //$NON-NLS-1$

  private final WizardPage wizardPage;

  private Button asMavenProject;
  private Group mavenCoordinateGroup;
  private Text groupIdField;
  private Text artifactIdField;
  private Text versionField;
  private Label groupIdLabel;
  private Label artifactIdLabel;
  private Label versionLabel;

  public MavenProjectWizardSupport(WizardPage wizardPage) {
    this.wizardPage = wizardPage;
  }

  /**
   * @param dynamicEnabling if {@code true}, creates a master check box that enables or disables
   *     the Maven coordinate area; otherwise, statically shows the area
   */
  public void createMavenCoordinateArea(Composite container, boolean dynamicEnabling) {
    if (dynamicEnabling) {
      asMavenProject = new Button(container, SWT.CHECK);
      asMavenProject.setText(Messages.getString("CREATE_AS_MAVEN_PROJECT")); //$NON-NLS-1$
      asMavenProject.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          updateMavenSectionEnabled();
        }
      });
    }

    mavenCoordinateGroup = new Group(container, SWT.NONE);
    mavenCoordinateGroup.setText(Messages.getString("MAVEN_PROJECT_COORDINATE")); //$NON-NLS-1$

    groupIdLabel = new Label(mavenCoordinateGroup, SWT.LEAD);
    groupIdLabel.setText(Messages.getString("GROUP_ID")); //$NON-NLS-1$
    groupIdField = new Text(mavenCoordinateGroup, SWT.BORDER);
    groupIdField.setToolTipText(Messages.getString("GROUP_ID_TOOLTIP")); //$NON-NLS-1$

    artifactIdLabel = new Label(mavenCoordinateGroup, SWT.LEAD);
    artifactIdLabel.setText(Messages.getString("ARTIFACT_ID")); //$NON-NLS-1$
    artifactIdField = new Text(mavenCoordinateGroup, SWT.BORDER);
    artifactIdField.setToolTipText(Messages.getString("ARTIFACT_ID_TOOLTIP")); //$NON-NLS-1$

    versionLabel = new Label(mavenCoordinateGroup, SWT.LEAD);
    versionLabel.setText(Messages.getString("ARTIFACT_VERSION")); //$NON-NLS-1$
    versionField = new Text(mavenCoordinateGroup, SWT.BORDER);
    versionField.setText(DEFAULT_VERSION);

    updateMavenSectionEnabled();

    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(mavenCoordinateGroup);
    GridDataFactory.swtDefaults().span(2, 1).applyTo(mavenCoordinateGroup);
    GridDataFactory horizontalFill = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER);
    horizontalFill.applyTo(groupIdField);
    horizontalFill.applyTo(artifactIdField);
    horizontalFill.applyTo(versionField);
  }

  public String getGroupId() {
    return groupIdField.getText().trim();
  }

  public String getArtifactId() {
    return artifactIdField.getText().trim();
  }

  public String getVersion() {
    return versionField.getText().trim();
  }

  public void addModifyListener(ModifyListener listener) {
    groupIdField.addModifyListener(listener);
    artifactIdField.addModifyListener(listener);
    versionField.addModifyListener(listener);
  }

  public void addGroupIdModifyListener(ModifyListener listener) {
    groupIdField.addModifyListener(listener);
  }

  private void updateMavenSectionEnabled() {
    if (asMavenProject != null) {
      boolean checked = asMavenProject.getSelection();
      mavenCoordinateGroup.setEnabled(checked);
      groupIdLabel.setEnabled(checked);
      groupIdField.setEnabled(checked);
      artifactIdLabel.setEnabled(checked);
      artifactIdField.setEnabled(checked);
      versionLabel.setEnabled(checked);
      versionField.setEnabled(checked);
    }
  }

  /**
   * Convenience method that validates a Maven coordinate and sets information or error messages on
   * a {@link WizardPage} accordingly.
   *
   * @return {@code true} if there was a validation problem; {@code false} otherwise
   */
  public boolean validateMavenSettings() {
    String errorMessage = null;
    String infoMessage = null;

    if (getGroupId().isEmpty()) {
      infoMessage = Messages.getString("PROVIDE_GROUP_ID"); //$NON-NLS-1$
    } else if (getArtifactId().isEmpty()) {
      infoMessage = Messages.getString("PROVIDE_ARTIFACT_ID"); //$NON-NLS-1$
    } else if (getVersion().isEmpty()) {
      infoMessage = Messages.getString("PROVIDE_VERSION"); //$NON-NLS-1$
    } else if (!MavenCoordinatesValidator.validateGroupId(getGroupId())) {
      errorMessage = Messages.getString("ILLEGAL_GROUP_ID", groupIdField.getText()); //$NON-NLS-1$
    } else if (!MavenCoordinatesValidator.validateArtifactId(getArtifactId())) {
      errorMessage = Messages.getString("ILLEGAL_ARTIFACT_ID", getArtifactId());
    } else if (!MavenCoordinatesValidator.validateVersion(getVersion())) {
      errorMessage = Messages.getString("ILLEGAL_VERSION", getVersion()); //$NON-NLS-1$
    } else if (ResourcesPlugin.getWorkspace().getRoot().getProject(getArtifactId()).exists()) {
      errorMessage = Messages.getString("PROJECT_ALREADY_EXISTS", getArtifactId()); //$NON-NLS-1$
    }

    if (errorMessage != null) {
      wizardPage.setErrorMessage(errorMessage);
    } else if (infoMessage != null) {
      wizardPage.setMessage(infoMessage, IMessageProvider.INFORMATION);
    }

    return errorMessage == null && infoMessage == null;
  }

}
