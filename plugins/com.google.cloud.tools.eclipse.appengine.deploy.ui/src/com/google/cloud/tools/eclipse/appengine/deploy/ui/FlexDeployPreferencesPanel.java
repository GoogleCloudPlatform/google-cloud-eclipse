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

import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployPreferences;
import com.google.common.annotations.VisibleForTesting;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

public class FlexDeployPreferencesPanel extends DeployPreferencesPanel {
  private static final int LINKED_CHILD_INDENT = 10;

  private static Logger logger = Logger.getLogger(FlexDeployPreferencesPanel.class.getName());

  private FlexDeployPreferences preferences;
  private Button useValuesButton;
  private Label gaeConfigFolderLabel;
  private Text gaeConfigFolderText;
  private Button gaeConfigFolderBrowseButton;
  private Label dockerFileLabel;
  private Text dockerFileText;
  private Button dockerFileBrowseButton;

  public FlexDeployPreferencesPanel(Composite parent, IProject project) {
    this(parent, project, new FlexDeployPreferences(project));
  }

  @VisibleForTesting
  public FlexDeployPreferencesPanel(Composite parent, IProject project,
      FlexDeployPreferences preferences) {
    super(parent, SWT.NONE);
    createConfigurationFilesSection();
    this.preferences = preferences;
    applyPreferences(preferences);
  }

  @Override
  DataBindingContext getDataBindingContext() {
    // provides default validation status
    return new DataBindingContext();
  }

  @Override
  void resetToDefaults() {
    applyPreferences(FlexDeployPreferences.DEFAULT);
  }

  @Override
  boolean savePreferences() {
    preferences.setUseDeploymentPreferences(useValuesButton.getSelection());
    preferences.setAppEngineDirectory(gaeConfigFolderText.getText().trim());
    preferences.setDockerDirectory(dockerFileText.getText().trim());
    try {
      preferences.save();
      return true;
    } catch (BackingStoreException ex) {
      logger.log(Level.SEVERE, "Could not save deploy preferences", ex);
      MessageDialog.openError(getShell(),
                              Messages.getString("deploy.preferences.save.error.title"),
                              Messages.getString("deploy.preferences.save.error.message",
                                                 ex.getLocalizedMessage()));
      return false;
    }
  }

  private void createConfigurationFilesSection() {
    useValuesButton = new Button(this, SWT.CHECK);
    useValuesButton.setText(Messages.getString("use.config.values"));
    useValuesButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
    useValuesButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        updateControls();
      }
    });

    gaeConfigFolderLabel = new Label(this, SWT.LEFT);
    gaeConfigFolderLabel.setText(Messages.getString("config.folder.location"));
    gaeConfigFolderText = new Text(this, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    gaeConfigFolderBrowseButton = new Button(this, SWT.PUSH);
    gaeConfigFolderBrowseButton.setText(Messages.getString("browse.button"));
    gaeConfigFolderBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        browseForConfigFolder();
      }
    });

    dockerFileLabel = new Label(this, SWT.LEFT);
    dockerFileLabel.setText(Messages.getString("docker.file.location"));
    dockerFileText = new Text(this, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    dockerFileBrowseButton = new Button(this, SWT.PUSH);
    dockerFileBrowseButton.setText(Messages.getString("browse.button"));
    dockerFileBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        browseForDockerFile();
      }
    });

    GridDataFactory linkedChildData = GridDataFactory.swtDefaults().indent(LINKED_CHILD_INDENT, 0);
    linkedChildData.applyTo(gaeConfigFolderLabel);
    linkedChildData.applyTo(dockerFileLabel);

    GridLayoutFactory.fillDefaults().numColumns(3).generateLayout(this);
  }

  private void browseForConfigFolder() {
    String last = gaeConfigFolderText.getText().trim();
    DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SINGLE);
    dialog.setText(Messages.getString("config.folder.browse.button.text"));
    dialog.setMessage(Messages.getString("config.folder.browse.button.message"));
    dialog.setFilterPath(last);
    String result = dialog.open();
    if (result == null) {
      return;
    }
    gaeConfigFolderText.setText(result);
  }

  private void browseForDockerFile() {
    String last = dockerFileText.getText().trim();
    DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SINGLE);
    dialog.setText(Messages.getString("docker.file.browse.button.text"));
    dialog.setFilterPath(last);
    String result = dialog.open();
    if (result == null) {
      return;
    }
    dockerFileText.setText(result);
  }

  private void updateControls() {
    boolean enabled = useValuesButton.getSelection();
    gaeConfigFolderLabel.setEnabled(enabled);
    gaeConfigFolderText.setEnabled(enabled);
    gaeConfigFolderBrowseButton.setEnabled(enabled);
    dockerFileLabel.setEnabled(enabled);
    dockerFileText.setEnabled(enabled);
    dockerFileBrowseButton.setEnabled(enabled);
  }

  private void applyPreferences(FlexDeployPreferences preferences) {
    useValuesButton.setSelection(preferences.getUseDeploymentPreferences());
    dockerFileText.setText(preferences.getDockerDirectory());
    updateControls();
  }

  @Override
  String getHelpContextId() {
    return "com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployAppEngineFlexProjectContext"; //$NON-NLS-1$
  }
}
