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

package com.google.cloud.tools.eclipse.sdk.ui.preferences;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkOutOfDateException;
import com.google.cloud.tools.eclipse.preferences.areas.PreferenceArea;
import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceConstants;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceConstants.CloudSdkManagement;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;

public class CloudSdkPreferenceArea extends PreferenceArea {
  /** Preference Page ID that hosts this area. */
  public static final String PAGE_ID =
      "com.google.cloud.tools.eclipse.preferences.main"; //$NON-NLS-1$

  private Button managedSdkRadio;
  private Composite managedSdkSubArea;
  private BooleanFieldEditor autoUpdateCheck;
  private Button updateNow;

  private Button customSdkRadio;
  private Composite customSdkSubArea;
  private CloudSdkDirectoryFieldEditor sdkLocation;

  private IStatus status = Status.OK_STATUS;
  private IPropertyChangeListener wrappedPropertyChangeListener = new IPropertyChangeListener() {

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (event.getProperty() == DirectoryFieldEditor.IS_VALID) {
        fireValueChanged(IS_VALID, event.getOldValue(), event.getNewValue());
      } else if (event.getProperty() == DirectoryFieldEditor.VALUE) {
        fireValueChanged(VALUE, event.getOldValue(), event.getNewValue());
      }
    }
  };

  @Override
  public Control createContents(Composite parent) {
    Composite contents = new Composite(parent, SWT.NONE);
    Link instructions = new Link(contents, SWT.WRAP);
    instructions.setText(Messages.getString("CloudSdkRequired")); //$NON-NLS-1$
    instructions.setFont(contents.getFont());
    instructions.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        WorkbenchUtil.openInBrowser(PlatformUI.getWorkbench(), event.text);
      }
    });

    if (CloudSdkManager.managedSdkFeatureEnabled()) {
      managedSdkRadio = new Button(parent, SWT.RADIO);
      managedSdkRadio.setText(Messages.getString("ManagedSdk")); //$NON-NLS-1$
      managedSdkRadio.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateControlEnablement();
        }
      });

      managedSdkSubArea = new Composite(parent, SWT.NONE);
      autoUpdateCheck = new BooleanFieldEditor(
          PreferenceConstants.CLOUD_SDK_AUTO_UPDATE,
          Messages.getString("AutoUpdate"), BooleanFieldEditor.DEFAULT, managedSdkSubArea);
      autoUpdateCheck.setPreferenceStore(getPreferenceStore());

      updateNow = new Button(managedSdkSubArea, SWT.NONE);
      updateNow.setText(Messages.getString("UpdateNow")); //$NON-NLS-1$

      customSdkRadio = new Button(parent, SWT.RADIO);
      customSdkRadio.setText(Messages.getString("CustomSdk")); //$NON-NLS-1$
    }

    customSdkSubArea = new Composite(parent, SWT.NONE);
    sdkLocation = new CloudSdkDirectoryFieldEditor(PreferenceConstants.CLOUD_SDK_PATH,
        Messages.getString("SdkLocation"), customSdkSubArea); //$NON-NLS-1$
    Path defaultLocation = getDefaultSdkLocation();
    if (defaultLocation != null) {
      sdkLocation.setFilterPath(defaultLocation.toFile());
    }
    sdkLocation.setPreferenceStore(getPreferenceStore());
    sdkLocation.setPropertyChangeListener(wrappedPropertyChangeListener);

    if (CloudSdkManager.managedSdkFeatureEnabled()) {
      GridLayoutFactory.fillDefaults().numColumns(2)
          .extendedMargins(IDialogConstants.LEFT_MARGIN, 0, 0, 0)
          .generateLayout(managedSdkSubArea);
      GridLayoutFactory.fillDefaults().numColumns(sdkLocation.getNumberOfControls())
          .extendedMargins(IDialogConstants.LEFT_MARGIN, 0, 0, 0)
          .generateLayout(customSdkSubArea);
    } else {
      GridLayoutFactory.fillDefaults().numColumns(sdkLocation.getNumberOfControls())
          .generateLayout(customSdkSubArea);
    }
    GridLayoutFactory.fillDefaults().generateLayout(contents);

    Dialog.applyDialogFont(contents);
    return contents;
  }

  @VisibleForTesting
  void loadSdkManagement(boolean loadDefault) {
    IPreferenceStore preferenceStore = getPreferenceStore();
    String value;
    if (loadDefault) {
      value = preferenceStore.getDefaultString(PreferenceConstants.CLOUD_SDK_MANAGEMENT);
    } else {
      value = preferenceStore.getString(PreferenceConstants.CLOUD_SDK_MANAGEMENT);
    }

    boolean managed = CloudSdkManagement.MANAGED.name().equals(value);
    managedSdkRadio.setSelection(managed);
    customSdkRadio.setSelection(!managed);
  }

  private void updateControlEnablement() {
    boolean managed = managedSdkRadio.getSelection();
    autoUpdateCheck.setEnabled(managed, managedSdkSubArea);
    updateNow.setEnabled(managed);
    sdkLocation.setEnabled(!managed, customSdkSubArea);
  }

  @Override
  public void load() {
    loadSdkManagement(false /* loadDefault */);
    autoUpdateCheck.load();
    sdkLocation.load();
    updateControlEnablement();
    fireValueChanged(VALUE, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  public void loadDefault() {
    loadSdkManagement(true /* loadDefault */);
    autoUpdateCheck.loadDefault();
    sdkLocation.loadDefault();
    updateControlEnablement();
  }

  @Override
  public IStatus getStatus() {
    return status;
  }

  @Override
  public void performApply() {
    if (managedSdkRadio.getSelection()) {
      getPreferenceStore().putValue(PreferenceConstants.CLOUD_SDK_MANAGEMENT,
          CloudSdkManagement.MANAGED.name());
    } else {
      getPreferenceStore().putValue(PreferenceConstants.CLOUD_SDK_MANAGEMENT,
          CloudSdkManagement.CUSTOM.name());
    }
    autoUpdateCheck.store();
    sdkLocation.store();
  }

  @VisibleForTesting
  public void setSdkLocation(String value) {
    sdkLocation.setStringValue(value);
  }

  private static Path getDefaultSdkLocation() {
    try {
      return new CloudSdk.Builder().build().getSdkPath();
    } catch (AppEngineException ex) {
      return null;
    }
  }

  private boolean validateSdk(Path location) {
    CloudSdk sdk = new CloudSdk.Builder().sdkPath(location).build();
    try {
      sdk.validateCloudSdk();
      sdk.validateAppEngineJavaComponents();
      status = Status.OK_STATUS;
      return true;
    } catch (CloudSdkNotFoundException ex) {
      // accept a seemingly invalid location in case the SDK organization
      // has changed and the CloudSdk#validate() code is out of date
      status = new Status(IStatus.WARNING, getClass().getName(),
          Messages.getString("CloudSdkNotFound", sdk.getSdkPath())); //$NON-NLS-1$
      return false;
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      status = new Status(IStatus.WARNING, getClass().getName(),
          Messages.getString("AppEngineJavaComponentsNotInstalled", ex.getMessage())); //$NON-NLS-1$
      return false;
    } catch (CloudSdkOutOfDateException ex) {
      status = new Status(IStatus.ERROR, getClass().getName(),
          Messages.getString("CloudSdkOutOfDate")); //$NON-NLS-1$
        return false;
    }
  }

  /**
   * A wrapper around DirectoryFieldEditor for validating that the location holds
   * a SDK. Uses {@code VALIDATE_ON_KEY_STROKE} to perform check per keystroke to avoid wiping
   * out the validation messages.
   */
  class CloudSdkDirectoryFieldEditor extends DirectoryFieldEditor {
    CloudSdkDirectoryFieldEditor(String name, String labelText, Composite parent) {
      // unfortunately cannot use super(name, labelText, parent) as must specify the
      // validateStrategy before the createControl()
      init(name, labelText);
      setErrorMessage(JFaceResources.getString("DirectoryFieldEditor.errorMessage")); //$NON-NLS-1$
      setChangeButtonText(JFaceResources.getString("openBrowse")); //$NON-NLS-1$
      setEmptyStringAllowed(true);
      setValidateStrategy(VALIDATE_ON_KEY_STROKE);
      createControl(parent);
    }

    @Override
    public void setFilterPath(File path) {
      super.setFilterPath(path);
      if (path != null) {
        getTextControl().setMessage(path.getAbsolutePath().toString());
      }
    }

    @Override
    protected boolean doCheckState() {
      String directory = getStringValue().trim();
      if (directory.isEmpty()) {
        status = Status.OK_STATUS;
        return true;
      }

      Path location = Paths.get(directory);
      if (!Files.exists(location)) {
        String message = Messages.getString("NoSuchDirectory", location); //$NON-NLS-1$
        status = new Status(IStatus.ERROR, getClass().getName(), message);
        return false;
      } else if (!Files.isDirectory(location)) {
        String message = Messages.getString("FileNotDirectory", location); //$NON-NLS-1$
        status = new Status(IStatus.ERROR, getClass().getName(), message);
        return false;
      }
      return validateSdk(location);
    }
  }
}
