/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.databinding.dialog.TitleAreaDialogSupport;
import org.eclipse.jface.databinding.dialog.ValidationMessageProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public abstract class DeployPreferencesDialog extends TitleAreaDialog {

  private Image titleImage;

  private AppEngineDeployPreferencesPanel content;
  private final String title;
  private final IProject project;

  public DeployPreferencesDialog(Shell parentShell, String title, IProject project) {
    super(parentShell);

    this.title = title;
    this.project = project;
  }

  @Override
  protected Control createContents(Composite parent) {
    // if the image is smaller (e.g. 32x32, it will break the layout of the TitleAreaDialog)
    // seems like an Eclipse/JFace bug
    titleImage = AppEngineImages.appEngine(64).createImage();

    Control contents = super.createContents(parent);

    getShell().setText(title);
    if (project == null) {
      setTitle(Messages.getString("deploy.preferences.dialog.subtitle"));
    } else {
      setTitle(
          Messages.getString("deploy.preferences.dialog.subtitle.withProject", project.getName()));
    }
    setTitleImage(titleImage);

    getButton(IDialogConstants.OK_ID).setText(Messages.getString("deploy"));

    // TitleAreaDialogSupport does not validate initially, let's trigger validation this way
    content.getDataBindingContext().updateTargets();

    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);

    Composite container = new Composite(area, SWT.NONE);
    content = createDeployPreferencesPanel(container, project, this::handleLayoutChange,
        new ProjectRepository());
    GridDataFactory.fillDefaults().grab(true, false).applyTo(content);

    // we pull in Dialog's content margins which are zeroed out by TitleAreaDialog
    GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
    GridLayoutFactory.fillDefaults()
        .margins(LayoutConstants.getMargins().x, LayoutConstants.getMargins().y)
        .generateLayout(container);

    TitleAreaDialogSupport.create(this, content.getDataBindingContext())
        .setValidationMessageProvider(new ValidationMessageProvider() {
          @Override
          public int getMessageType(ValidationStatusProvider statusProvider) {
            int type = super.getMessageType(statusProvider);
            setValid(type != IMessageProvider.ERROR && GoogleApiFactory.INSTANCE.getCredential().isPresent());
            return type;
          }
        });
    return area;
  }

  protected abstract AppEngineDeployPreferencesPanel createDeployPreferencesPanel(
      Composite container, IProject project, 
      Runnable layoutChangedHandler, ProjectRepository projectRepository);

  private void handleLayoutChange() {
    Shell shell = getShell();
    shell.setMinimumSize(shell.getSize().x, 0);
    shell.pack();
    shell.setMinimumSize(shell.getSize());
  }

  @Override
  protected void okPressed() {
    content.savePreferences();
    super.okPressed();
  }

  @Override
  public boolean close() {
    if (titleImage != null) {
      titleImage.dispose();
      titleImage = null;
    }
    return super.close();
  }

  @Override
  public boolean isHelpAvailable() {
    return false;
  }

  private void setValid(boolean isValid) {
    Button deployButton = getButton(IDialogConstants.OK_ID);
    if (deployButton != null) {
      deployButton.setEnabled(isValid);
    }
  }

  public Credential getCredential() {
    return content.getSelectedCredential();
  }
}
