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
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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

public class MavenCoordinatesUi {

  private static final String DEFAULT_VERSION = "0.1.0-SNAPSHOT"; //$NON-NLS-1$

  private Button asMavenProjectButton;
  private Group coordinatesGroup;
  private Text groupIdField;
  private Text artifactIdField;
  private Text versionField;
  private Label groupIdLabel;
  private Label artifactIdLabel;
  private Label versionLabel;

  /**
   * @param dynamicEnabling if {@code true}, creates a master check box that enables or disables
   *     the Maven coordinate area; otherwise, always enables the area
   */
  public MavenCoordinatesUi(Composite container, boolean dynamicEnabling) {
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

    if (dynamicEnabling) {
      updateEnablement();
    }

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
    boolean checked = asMavenProjectButton.getSelection();
    coordinatesGroup.setEnabled(checked);
    groupIdLabel.setEnabled(checked);
    groupIdField.setEnabled(checked);
    artifactIdLabel.setEnabled(checked);
    artifactIdField.setEnabled(checked);
    versionLabel.setEnabled(checked);
    versionField.setEnabled(checked);
  }

  /**
   * @return {@link IStatus#OK} if there was no validation problem or the UI is disabled; otherwise
   *     a status describing a validation problem (with a non-OK status)
   */
  public IStatus validateMavenSettings() {
    if (asMavenProjectButton != null && !asMavenProjectButton.getSelection()) {
      return Status.OK_STATUS;
    }

    if (getGroupId().isEmpty()) {
      return StatusUtil.info(this, Messages.getString("PROVIDE_GROUP_ID")); //$NON-NLS-1$
    } else if (getArtifactId().isEmpty()) {
      return StatusUtil.info(this, Messages.getString("PROVIDE_ARTIFACT_ID")); //$NON-NLS-1$
    } else if (getVersion().isEmpty()) {
      return StatusUtil.info(this, Messages.getString("PROVIDE_VERSION")); //$NON-NLS-1$
    } else if (!MavenCoordinatesValidator.validateGroupId(getGroupId())) {
      return StatusUtil.error(this,
          Messages.getString("ILLEGAL_GROUP_ID", groupIdField.getText())); //$NON-NLS-1$
    } else if (!MavenCoordinatesValidator.validateArtifactId(getArtifactId())) {
      return StatusUtil.error(this,
          Messages.getString("ILLEGAL_ARTIFACT_ID", getArtifactId())); //$NON-NLS-1$
    } else if (!MavenCoordinatesValidator.validateVersion(getVersion())) {
      return StatusUtil.error(this,
          Messages.getString("ILLEGAL_VERSION", getVersion())); //$NON-NLS-1$
    }
    return Status.OK_STATUS;
  }

  /**
   * Convenience method to set a validation message on {@link DialogPage} from the result of calling
   * {@link #validateMavenSettings()}.
   *
   * @return {@code true} if no validation message was set; {@code false} otherwise
   *
   * @see #validateMavenSettings()
   */
  public boolean setValidationMessage(DialogPage page) {
    IStatus status = validateMavenSettings();
    if (status.isOK()) {
      return true;
    }

    if (IStatus.ERROR == status.getSeverity()) {
      page.setErrorMessage(status.getMessage());
    } else if (IStatus.WARNING == status.getSeverity()) {
      page.setMessage(status.getMessage(), IMessageProvider.WARNING);
    } else if (IStatus.INFO == status.getSeverity()) {
      page.setMessage(status.getMessage(), IMessageProvider.INFORMATION);
    }
    return false;
  }
}
