/*
 * Copyright 2017 Google Inc.
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

import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class MavenCoordinatesDialogPageUi {

  private static final String DEFAULT_VERSION = "0.1.0-SNAPSHOT"; //$NON-NLS-1$

  private final DialogPage dialogPage;

  private Button asMavenProjectButton;
  private Group coordinatesGroup;
  private Text groupIdField;
  private Text artifactIdField;
  private Text versionField;
  private Label groupIdLabel;
  private Label artifactIdLabel;
  private Label versionLabel;

  public MavenCoordinatesDialogPageUi(DialogPage dialogPage) {
    this.dialogPage = dialogPage;
  }

  /**
   * @param dynamicEnabling if {@code true}, creates a master check box that enables or disables
   *     the Maven coordinate area; otherwise, always enables the area
   */
  public void createMavenCoordinatesArea(Composite container, boolean dynamicEnabling) {
    if (dynamicEnabling) {
      asMavenProjectButton = new Button(container, SWT.CHECK);
      asMavenProjectButton.setText(Messages.getString("CREATE_AS_MAVEN_PROJECT")); //$NON-NLS-1$
      asMavenProjectButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          updateEnablement();
        }
      });
    }

    coordinatesGroup = new Group(container, SWT.NONE);
    coordinatesGroup.setText(Messages.getString("MAVEN_PROJECT_COORDINATES")); //$NON-NLS-1$

    groupIdLabel = new Label(coordinatesGroup, SWT.LEAD);
    groupIdLabel.setText(Messages.getString("GROUP_ID")); //$NON-NLS-1$
    groupIdField = new Text(coordinatesGroup, SWT.BORDER);
    groupIdField.setToolTipText(Messages.getString("GROUP_ID_TOOLTIP")); //$NON-NLS-1$

    artifactIdLabel = new Label(coordinatesGroup, SWT.LEAD);
    artifactIdLabel.setText(Messages.getString("ARTIFACT_ID")); //$NON-NLS-1$
    artifactIdField = new Text(coordinatesGroup, SWT.BORDER);
    artifactIdField.setToolTipText(Messages.getString("ARTIFACT_ID_TOOLTIP")); //$NON-NLS-1$

    versionLabel = new Label(coordinatesGroup, SWT.LEAD);
    versionLabel.setText(Messages.getString("ARTIFACT_VERSION")); //$NON-NLS-1$
    versionField = new Text(coordinatesGroup, SWT.BORDER);
    versionField.setText(DEFAULT_VERSION);

    updateEnablement();

    GridLayoutFactory.swtDefaults().numColumns(2).generateLayout(coordinatesGroup);
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

  private void updateEnablement() {
    if (asMavenProjectButton != null) {
      boolean checked = asMavenProjectButton.getSelection();
      coordinatesGroup.setEnabled(checked);
      groupIdLabel.setEnabled(checked);
      groupIdField.setEnabled(checked);
      artifactIdLabel.setEnabled(checked);
      artifactIdField.setEnabled(checked);
      versionLabel.setEnabled(checked);
      versionField.setEnabled(checked);
    }
  }

  /**
   * Convenience method that validates a Maven coordinate and sets an information or error message
   * on a {@link DialogPage} if applicable. Does nothing if the UI is disabled.
   *
   * @return {@code false} if there was a validation problem while the UI is enabled and the
   *     relevant message was set; {@code true} otherwise
   */
  public boolean validateMavenSettings() {
    boolean uiDisabled = asMavenProjectButton != null && !asMavenProjectButton.getSelection();
    if (uiDisabled) {
      return true;
    }

    String message = null;
    if (getGroupId().isEmpty()) {
      message = Messages.getString("PROVIDE_GROUP_ID"); //$NON-NLS-1$
      dialogPage.setMessage(message, IMessageProvider.INFORMATION);
    } else if (getArtifactId().isEmpty()) {
      message = Messages.getString("PROVIDE_ARTIFACT_ID"); //$NON-NLS-1$
      dialogPage.setMessage(message, IMessageProvider.INFORMATION);
    } else if (getVersion().isEmpty()) {
      message = Messages.getString("PROVIDE_VERSION"); //$NON-NLS-1$
      dialogPage.setMessage(message, IMessageProvider.INFORMATION);
    } else if (!MavenCoordinatesValidator.validateGroupId(getGroupId())) {
      message = Messages.getString("ILLEGAL_GROUP_ID", groupIdField.getText()); //$NON-NLS-1$
      dialogPage.setErrorMessage(message);
    } else if (!MavenCoordinatesValidator.validateArtifactId(getArtifactId())) {
      message = Messages.getString("ILLEGAL_ARTIFACT_ID", getArtifactId()); //$NON-NLS-1$
      dialogPage.setErrorMessage(message);
    } else if (!MavenCoordinatesValidator.validateVersion(getVersion())) {
      message = Messages.getString("ILLEGAL_VERSION", getVersion()); //$NON-NLS-1$
      dialogPage.setErrorMessage(message);
    }
    return message == null;
  }

}
