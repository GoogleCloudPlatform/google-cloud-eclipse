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
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkOutOfDateException;
import com.google.cloud.tools.eclipse.preferences.areas.PreferenceArea;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudSdkPreferenceArea extends PreferenceArea {
  /** Preference Page ID that hosts this area. */
  public static final String PAGE_ID = "com.google.cloud.tools.eclipse.preferences.main";
  private static final Logger logger = Logger.getLogger(CloudSdkPreferenceArea.class.getName());

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
    instructions.setText(SdkUiMessages.CloudSdkRequired);
    instructions.setFont(contents.getFont());
    instructions.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        openUrl(event.text);
      }
    });

    Composite fieldContents = new Composite(parent, SWT.NONE);
    sdkLocation = new CloudSdkDirectoryFieldEditor(PreferenceConstants.CLOUDSDK_PATH,
        SdkUiMessages.SdkLocation, fieldContents);
    Path defaultLocation = getDefaultSdkLocation();
    if (defaultLocation != null) {
      sdkLocation.setFilterPath(defaultLocation.toFile());
    }
    sdkLocation.setPreferenceStore(getPreferenceStore());
    sdkLocation.setPropertyChangeListener(wrappedPropertyChangeListener);
    GridLayoutFactory.fillDefaults().numColumns(sdkLocation.getNumberOfControls())
        .generateLayout(fieldContents);

    GridLayoutFactory.fillDefaults().generateLayout(contents);
    
    Dialog.applyDialogFont(contents);
    return contents;
  }

  @Override
  public void load() {
    sdkLocation.load();
    fireValueChanged(VALUE, "", "");
  }

  @Override
  public void loadDefault() {
    sdkLocation.loadDefault();
  }

  @Override
  public IStatus getStatus() {
    return status;
  }

  @Override
  public void performApply() {
    sdkLocation.store();
  }

  /**
   * Sets the new value or {@code null} for the empty string.
   */
  public void setStringValue(String value) {
    sdkLocation.setStringValue(value);
  }

  protected void openUrl(String urlText) {
    try {
      if (getWorkbench() != null) {
        URL url = new URL(urlText);
        IWorkbenchBrowserSupport browserSupport = getWorkbench().getBrowserSupport();
        browserSupport.createBrowser(null).openURL(url);
      } else {
        Program.launch(urlText);
      }
    } catch (MalformedURLException mue) {
      logger.log(Level.WARNING, SdkUiMessages.CloudSdkPreferencePage_3, mue);
    } catch (PartInitException pie) {
      logger.log(Level.WARNING, SdkUiMessages.CloudSdkPreferencePage_4, pie);
    }
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
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      status = new Status(IStatus.WARNING, getClass().getName(),
          MessageFormat.format(SdkUiMessages.AppEngineJavaComponentsNotInstalled, ex.getMessage()));
      return false;
    } catch (CloudSdkOutOfDateException ex) {
        status = new Status(IStatus.ERROR,
            "com.google.cloud.tools.eclipse.appengine.deploy.ui", SdkUiMessages.CloudSdkOutOfDate);
        return false;
    } catch (AppEngineException ex) {
      // accept a seemingly invalid location in case the SDK organization
      // has changed and the CloudSdk#validate() code is out of date
      status = new Status(IStatus.WARNING, getClass().getName(),
          MessageFormat.format(SdkUiMessages.CloudSdkNotFound, sdk.getSdkPath()));
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
      // unfortunately cannot use super(name,labelText,parent) as must specify the
      // validateStrategy before the createControl()
      init(name, labelText);
      setErrorMessage(JFaceResources.getString("DirectoryFieldEditor.errorMessage"));//$NON-NLS-1$
      setChangeButtonText(JFaceResources.getString("openBrowse"));//$NON-NLS-1$
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
        return true;
      }
      
      Path location = Paths.get(directory);
      if (!Files.exists(location)) {
        String message = MessageFormat.format(SdkUiMessages.NoSuchDirectory, location);
        status = new Status(IStatus.ERROR, getClass().getName(), message);
        return false;
      } else if (!Files.isDirectory(location)) {
        String message = MessageFormat.format(SdkUiMessages.FileNotDirectory, location);
        status = new Status(IStatus.ERROR, getClass().getName(), message);
        return false;
      }
      return validateSdk(location);
    }
  }
}
