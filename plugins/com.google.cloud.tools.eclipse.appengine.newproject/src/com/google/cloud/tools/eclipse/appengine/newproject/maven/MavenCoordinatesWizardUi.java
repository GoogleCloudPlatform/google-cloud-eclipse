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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

public class MavenCoordinatesWizardUi extends Composite {

  private final Button asMavenProjectButton;

  private final MavenCoordinatesUi mavenCoordinatesUi;

  public MavenCoordinatesWizardUi(Composite container, int style) {
    super(container, style);

    asMavenProjectButton = new Button(this, SWT.CHECK);
    asMavenProjectButton.setText(Messages.getString("CREATE_AS_MAVEN_PROJECT")); //$NON-NLS-1$
    asMavenProjectButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        updateEnablement();
      }
    });

    mavenCoordinatesUi = new MavenCoordinatesUi(this, SWT.NONE);
    mavenCoordinatesUi.setText(Messages.getString("MAVEN_PROJECT_COORDINATES")); //$NON-NLS-1$

    updateEnablement();

    GridDataFactory.defaultsFor(mavenCoordinatesUi).indent(20, 0).applyTo(mavenCoordinatesUi);
    GridLayoutFactory.swtDefaults().generateLayout(this);
  }

  public boolean uiEnabled() {
    return asMavenProjectButton.getSelection();
  }

  public String getGroupId() {
    return mavenCoordinatesUi.getGroupId();
  }

  public String getArtifactId() {
    return mavenCoordinatesUi.getArtifactId();
  }

  public String getVersion() {
    return mavenCoordinatesUi.getVersion();
  }

  public void addChangeListener(Listener listener) {
    mavenCoordinatesUi.addChangeListener(listener);
    if (asMavenProjectButton != null) {
      asMavenProjectButton.addListener(SWT.Selection, listener);
    }
  }

  public void addGroupIdModifyListener(ModifyListener listener) {
    mavenCoordinatesUi.addGroupIdModifyListener(listener);
  }

  private void updateEnablement() {
    boolean checked = asMavenProjectButton.getSelection();
    mavenCoordinatesUi.setEnabled(checked);
  }

  /**
   * @return {@link IStatus#OK} if there was no validation problem or the UI is disabled; otherwise
   *     a status describing a validation problem (with a non-OK status)
   */
  public IStatus validateMavenSettings() {
    if (!uiEnabled()) {
      return Status.OK_STATUS;
    } else {
      return mavenCoordinatesUi.validateMavenSettings();
    }
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
