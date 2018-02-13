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
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences.CloudSdkManagementOption;
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

  private Button useLocalSdk;
  private Composite localSdkArea;
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
    instructions.setText(CloudSdkManager.isManagedSdkFeatureEnabled()
        ? Messages.getString("CloudSdkRequiredWithManagedSdk") //$NON-NLS-1$
        : Messages.getString("CloudSdkRequired")); //$NON-NLS-1$
    instructions.setFont(contents.getFont());
    instructions.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        WorkbenchUtil.openInBrowser(PlatformUI.getWorkbench(), event.text);
      }
    });

    if (CloudSdkManager.isManagedSdkFeatureEnabled()) {
      useLocalSdk = new Button(parent, SWT.CHECK);
      useLocalSdk.setText(Messages.getString("UseLocalSdk")); //$NON-NLS-1$
      useLocalSdk.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if (!useLocalSdk.getSelection()) {
            status = Status.OK_STATUS;
          } else {
            sdkLocation.doCheckState();
          }
          fireValueChanged(VALUE, "", "");

          updateControlEnablement();
        }
      });
    }

    localSdkArea = new Composite(parent, SWT.NONE);
    sdkLocation = new CloudSdkDirectoryFieldEditor(CloudSdkPreferences.CLOUD_SDK_PATH,
        Messages.getString("SdkLocation"), localSdkArea); //$NON-NLS-1$
    Path defaultLocation = getDefaultSdkLocation();
    if (defaultLocation != null) {
      sdkLocation.setFilterPath(defaultLocation.toFile());
    }
    sdkLocation.setPreferenceStore(getPreferenceStore());
    sdkLocation.setPropertyChangeListener(wrappedPropertyChangeListener);

    if (CloudSdkManager.isManagedSdkFeatureEnabled()) {
      GridLayoutFactory.fillDefaults().numColumns(sdkLocation.getNumberOfControls())
          .extendedMargins(IDialogConstants.LEFT_MARGIN, 0, 0, 0)
          .generateLayout(localSdkArea);
    } else {
      GridLayoutFactory.fillDefaults().numColumns(sdkLocation.getNumberOfControls())
          .generateLayout(localSdkArea);
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
      value = preferenceStore.getDefaultString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT);
    } else {
      value = preferenceStore.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT);
    }

    boolean manual = CloudSdkManagementOption.MANUAL.name().equals(value);
    useLocalSdk.setSelection(manual);
  }

  private void updateControlEnablement() {
    boolean manual = useLocalSdk.getSelection();
    sdkLocation.setEnabled(manual, localSdkArea);
  }

  @Override
  public void load() {
    if (CloudSdkManager.isManagedSdkFeatureEnabled()) {
      loadSdkManagement(false /* loadDefault */);
      updateControlEnablement();
    }
    sdkLocation.load();
    fireValueChanged(VALUE, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  public void loadDefault() {
    if (CloudSdkManager.isManagedSdkFeatureEnabled()) {
      loadSdkManagement(true /* loadDefault */);
      updateControlEnablement();
    }
  }

  @Override
  public IStatus getStatus() {
    return status;
  }

  @Override
  public void performApply() {
    if (CloudSdkManager.isManagedSdkFeatureEnabled()) {
      if (useLocalSdk.getSelection()) {
        getPreferenceStore().putValue(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT,
            CloudSdkManagementOption.MANUAL.name());
      } else {
        getPreferenceStore().putValue(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT,
            CloudSdkManagementOption.AUTOMATIC.name());
      }
    }
    sdkLocation.store();
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
