/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.sdk.ui.preferences;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceConstants;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceInitializer;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudSdkPreferencePage extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {
  public static final String PAGE_ID = SdkUiMessages.CloudSdkPreferencePage_0;
  private static final Logger logger =
      Logger.getLogger(CloudSdkPreferencePage.class.getName());

  private IWorkbench workbench;
  private DirectoryFieldEditor sdkLocation;

  /** Create new page. */
  public CloudSdkPreferencePage() {
    super(GRID);
    setPreferenceStore(PreferenceInitializer.getPreferenceStore());
    setDescription(SdkUiMessages.CloudSdkPreferencePage_1);
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite contents = new Composite(parent, SWT.NONE);
    Link instructions = new Link(contents, SWT.WRAP);
    instructions.setText(
        SdkUiMessages.CloudSdkPreferencePage_2);
    instructions.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        openUrl(event.text);
      }
    });

    super.createContents(contents);
    GridLayoutFactory.swtDefaults().generateLayout(contents);
    return contents;
  }

  protected void openUrl(String urlText) {
    try {
      URL url = new URL(urlText);
      IWorkbenchBrowserSupport browserSupport = workbench.getBrowserSupport();
      browserSupport.createBrowser(null).openURL(url);
    } catch (MalformedURLException mue) {
      logger.log(Level.WARNING, SdkUiMessages.CloudSdkPreferencePage_3, mue);
    } catch (PartInitException pie) {
      logger.log(Level.WARNING, SdkUiMessages.CloudSdkPreferencePage_4, pie);
    }
  }

  /**
   * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
   * manipulate various types of preferences. Each field editor knows how to save and restore
   * itself.
   */
  @Override
  public void createFieldEditors() {
    sdkLocation = new CloudSdkDirectoryFieldEditor(PreferenceConstants.CLOUDSDK_PATH,
        SdkUiMessages.CloudSdkPreferencePage_5, getFieldEditorParent());
    addField(sdkLocation);
  }

  protected boolean validateSdk(File location) {
    try {
      CloudSdk sdk = new CloudSdk.Builder().sdkPath(location.toPath()).build();
      sdk.validate();
    } catch (AppEngineException ex) {
      // accept a seemingly invalid location in case the SDK organization
      // has changed and the CloudSdk#validate() code is out of date
      setMessage(MessageFormat.format(SdkUiMessages.CloudSdkPreferencePage_6, ex.getMessage()),
          WARNING);
    }
    return true;
  }

  @Override
  public void init(IWorkbench workbench) {
    this.workbench = workbench;
  }

  /**
   * Check that the location holds a SDK. Uses {@code VALIDATE_ON_KEY_STROKE} to perform check on
   * per keystroke to avoid wiping out the validation messages.
   */
  class CloudSdkDirectoryFieldEditor extends DirectoryFieldEditor {
    public CloudSdkDirectoryFieldEditor(String name, String labelText, Composite parent) {
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
    protected boolean doCheckState() {
      setMessage(null);
      if (!super.doCheckState()) {
        return false;
      }
      return getStringValue().isEmpty() || validateSdk(new File(getStringValue()));
    }
  }
}
