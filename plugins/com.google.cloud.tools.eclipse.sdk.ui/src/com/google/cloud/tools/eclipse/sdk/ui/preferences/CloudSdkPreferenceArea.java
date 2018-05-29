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
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkVersionFileException;
import com.google.cloud.tools.appengine.cloudsdk.InvalidJavaSdkException;
import com.google.cloud.tools.eclipse.preferences.areas.PreferenceArea;
import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences.CloudSdkManagementOption;
import com.google.cloud.tools.eclipse.sdk.ui.Messages;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;

public class CloudSdkPreferenceArea extends PreferenceArea {
  /** Preference Page ID that hosts this area. */
  public static final String PAGE_ID =
      "com.google.cloud.tools.eclipse.preferences.main"; //$NON-NLS-1$

  private Button chooseSdk;
  private Button updateSdk;
  private Composite chooseSdkArea;
  private CloudSdkDirectoryFieldEditor sdkLocation;
  private Label sdkVersionLabel;
  private IStatus status = Status.OK_STATUS;
  private IPropertyChangeListener wrappedPropertyChangeListener = new IPropertyChangeListener() {

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (event.getProperty() == DirectoryFieldEditor.IS_VALID) {
        fireValueChanged(IS_VALID, event.getOldValue(), event.getNewValue());
      } else if (event.getProperty() == DirectoryFieldEditor.VALUE) {
        fireValueChanged(VALUE, event.getOldValue(), event.getNewValue());
      }
      updateSelectedVersion();
    }
  };

  private final CloudSdkManager cloudSdkManager;

  public CloudSdkPreferenceArea() {
    this(CloudSdkManager.getInstance());
  }

  @VisibleForTesting
  CloudSdkPreferenceArea(CloudSdkManager cloudSdkManager) {
    this.cloudSdkManager = cloudSdkManager;
  }

  @Override
  public Control createContents(Composite parent) {
    Composite contents = new Composite(parent, SWT.NONE);
    Link instructions = new Link(contents, SWT.WRAP);
    instructions.setText(Messages.getString("CloudSdkRequiredWithManagedSdk")); // $NON-NLS-1$
    instructions.setFont(contents.getFont());
    instructions.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        WorkbenchUtil.openInBrowser(PlatformUI.getWorkbench(), event.text);
      }
    });

    Composite versionArea = new Composite(parent, SWT.NONE);
    sdkVersionLabel = new Label(versionArea, SWT.LEAD);
    sdkVersionLabel.setFont(contents.getFont());
    sdkVersionLabel.setText(Messages.getString("SdkVersion", "Unset")); //$NON-NLS-1$ //$NON-NLS-2$
    updateSdk = new Button(versionArea, SWT.PUSH);
    updateSdk.setText(Messages.getString("UpdateSdk"));
    updateSdk.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent event) {
            updateManagedSdk();
          }
        });
    GridDataFactory.defaultsFor(sdkVersionLabel).grab(true, false).applyTo(sdkVersionLabel);
    GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(versionArea);

    chooseSdk = new Button(parent, SWT.CHECK);
    chooseSdk.setText(Messages.getString("UseLocalSdk")); //$NON-NLS-1$
    chooseSdk.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        if (!chooseSdk.getSelection()) {
          status = Status.OK_STATUS;
        } else {
          sdkLocation.doCheckState();
        }
        updateSelectedVersion();
        fireValueChanged(VALUE, "", ""); //$NON-NLS-1$ //$NON-NLS-2$

        updateControlEnablement();
      }
    });

    chooseSdkArea = new Composite(parent, SWT.NONE);

    sdkLocation = new CloudSdkDirectoryFieldEditor(CloudSdkPreferences.CLOUD_SDK_PATH,
        Messages.getString("SdkLocation"), chooseSdkArea); //$NON-NLS-1$
    Path defaultLocation = getDefaultSdkLocation();
    if (defaultLocation != null) {
      sdkLocation.setFilterPath(defaultLocation.toFile());
    }
    sdkLocation.setPreferenceStore(getPreferenceStore());
    sdkLocation.setPropertyChangeListener(wrappedPropertyChangeListener);

    GridLayoutFactory.fillDefaults().numColumns(sdkLocation.getNumberOfControls())
        .extendedMargins(IDialogConstants.LEFT_MARGIN, 0, 0, 0)
        .generateLayout(chooseSdkArea);
    GridLayoutFactory.fillDefaults().generateLayout(contents);

    Dialog.applyDialogFont(contents);
    return contents;
  }

  private void updateManagedSdk() {
    cloudSdkManager.updateManagedSdkAsync();
    // would be nice if we could updateSelectedVersion() when finished
  }

  private void updateSelectedVersion() {
    String version = Messages.getString("UnknownVersion"); //$NON-NLS-1$
    String location = null;
    if (chooseSdk.getSelection()) {
      location = sdkLocation.getStringValue();
      if (Strings.isNullOrEmpty(location)) { 
        try {
          // look in default locations; see
          // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2897
          CloudSdk sdk = new CloudSdk.Builder().build();
          location = sdk.getPath().toString();
          version = sdk.getVersion().toString();
          // ends up calling this method again
          sdkLocation.setStringValue(location);
        } catch (CloudSdkNotFoundException | CloudSdkVersionFileException ex) {
          // surprising but here a CloudSdkVersionFileNotFoundException also means
          // no SDK is found where expected, probably because it was moved or deleted
          // behind Eclipse's back
          version = Messages.getString("NoSdkFound");
        }
      } else {
        Path path = Paths.get(location);
        version = getSdkVersion(path);
      }
    } else {
      try {
        Path home = ManagedCloudSdk.newManagedSdk().getSdkHome();
        version = getSdkVersion(home);
        location = home.toString();
      } catch (UnsupportedOsException ex) {
        // shouldn't happen but if it does we'll just leave
        // version set to Unknown
      }
    }
    sdkVersionLabel.setText(Messages.getString("SdkVersion", version)); //$NON-NLS-1$
    sdkVersionLabel.setToolTipText(location); // null is ok
  }

  private static String getSdkVersion(Path path) {
    if (!Files.exists(path) || !Files.isDirectory(path)) {
      return Messages.getString("NoSdkFound"); //$NON-NLS-1$
    }

    try {
      return new CloudSdk.Builder().sdkPath(path).build().getVersion().toString();
    } catch (CloudSdkVersionFileException | CloudSdkNotFoundException ex) {
      return Messages.getString("NoSdkFound"); //$NON-NLS-1$
    }
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
    chooseSdk.setSelection(manual);
  }

  private void updateControlEnablement() {
    boolean manual = chooseSdk.getSelection();
    sdkLocation.setEnabled(manual, chooseSdkArea);
    updateSdk.setEnabled(!manual);
  }

  @Override
  public void load() {
    loadSdkManagement(false /* loadDefault */);
    updateControlEnablement();
    sdkLocation.load();
    updateSelectedVersion();
    fireValueChanged(VALUE, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  public void loadDefault() {
    loadSdkManagement(true /* loadDefault */);
    updateControlEnablement();
    updateSelectedVersion();
  }

  @Override
  public IStatus getStatus() {
    return status;
  }

  @Override
  public void performApply() {
    if (chooseSdk.getSelection()) {
      getPreferenceStore().putValue(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT,
          CloudSdkManagementOption.MANUAL.name());
    } else {
      getPreferenceStore().putValue(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT,
          CloudSdkManagementOption.AUTOMATIC.name());
      cloudSdkManager.installManagedSdkAsync();
    }
    sdkLocation.store();
  }

  private static Path getDefaultSdkLocation() {
    try {
      return new CloudSdk.Builder().build().getPath();
    } catch (AppEngineException ex) {
      return null;
    }
  }

  private boolean validateSdk(Path location) {
    try {
      CloudSdk sdk = new CloudSdk.Builder().sdkPath(location).build();
      sdk.validateCloudSdk();
      CloudSdkManager.validateJdk(sdk);  // TODO: call sdk.validateJdk() once it becomes public.
      sdk.validateAppEngineJavaComponents();
      status = Status.OK_STATUS;
      return true;
    } catch (CloudSdkNotFoundException | InvalidJavaSdkException ex) {
      // accept a seemingly invalid location in case the SDK organization
      // has changed and the CloudSdk#validate() code is out of date
      status = new Status(IStatus.WARNING, getClass().getName(),
          Messages.getString("CloudSdkNotFound", location)); //$NON-NLS-1$
      return false;
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      status = new Status(IStatus.WARNING, getClass().getName(),
          Messages.getString("AppEngineJavaComponentsNotInstalled", ex.getMessage())); //$NON-NLS-1$
      return false;
    } catch (CloudSdkOutOfDateException | CloudSdkVersionFileException ex) {
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
    protected boolean doCheckState() {
      if (!chooseSdk.getSelection()) {
        // return early if we're not using a local SDK
        return true;
      }
      
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
      boolean valid = validateSdk(location);
      return valid;
    }
  }
}
