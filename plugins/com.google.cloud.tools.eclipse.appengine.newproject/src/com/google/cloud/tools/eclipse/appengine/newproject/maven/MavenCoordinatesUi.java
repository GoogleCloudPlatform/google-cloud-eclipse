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
import com.google.cloud.tools.eclipse.util.MavenCoordinatesValidator;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public class MavenCoordinatesUi extends Group implements MavenCoordinatesInput {

  private static final String DEFAULT_VERSION = "0.1.0-SNAPSHOT"; //$NON-NLS-1$

  private final Text groupIdField;
  private final Text artifactIdField;
  private final Text versionField;
  private final Label groupIdLabel;
  private final Label artifactIdLabel;
  private final Label versionLabel;

  public MavenCoordinatesUi(Composite container, int style) {
    super(container, style);

    groupIdLabel = new Label(this, SWT.LEAD);
    groupIdLabel.setText(Messages.getString("GROUP_ID")); //$NON-NLS-1$
    groupIdField = new Text(this, SWT.BORDER);
    groupIdField.setToolTipText(Messages.getString("GROUP_ID_TOOLTIP")); //$NON-NLS-1$

    artifactIdLabel = new Label(this, SWT.LEAD);
    artifactIdLabel.setText(Messages.getString("ARTIFACT_ID")); //$NON-NLS-1$
    artifactIdField = new Text(this, SWT.BORDER);
    artifactIdField.setToolTipText(Messages.getString("ARTIFACT_ID_TOOLTIP")); //$NON-NLS-1$

    versionLabel = new Label(this, SWT.LEAD);
    versionLabel.setText(Messages.getString("ARTIFACT_VERSION")); //$NON-NLS-1$
    versionField = new Text(this, SWT.BORDER);
    versionField.setText(DEFAULT_VERSION);

    GridLayoutFactory.swtDefaults().numColumns(2).generateLayout(this);
  }

  @Override
  protected void checkSubclass () {
    // Allow subclassing by not calling super().
  }

  @Override
  public String getGroupId() {
    return groupIdField.getText().trim();
  }

  @Override
  public String getArtifactId() {
    return artifactIdField.getText().trim();
  }

  @Override
  public String getVersion() {
    return versionField.getText().trim();
  }

  @Override
  public void addChangeListener(Listener listener) {
    groupIdField.addListener(SWT.Modify, listener);
    artifactIdField.addListener(SWT.Modify, listener);
    versionField.addListener(SWT.Modify, listener);
  }

  @Override
  public void addGroupIdModifyListener(ModifyListener listener) {
    groupIdField.addModifyListener(listener);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    groupIdLabel.setEnabled(enabled);
    groupIdField.setEnabled(enabled);
    artifactIdLabel.setEnabled(enabled);
    artifactIdField.setEnabled(enabled);
    versionLabel.setEnabled(enabled);
    versionField.setEnabled(enabled);
  }

  /**
   * @return {@link IStatus#OK} if there was no validation problem; otherwise a status describing a
   *     validation problem (with a non-OK status)
   */
  @Override
  public IStatus validateMavenSettings() {
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

  @Override
  public boolean uiEnabled() {
    return true;
  }
}
