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

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import java.io.File;

public class CloudSdkPreferencePage extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {
  private IWorkbench workbench;
  private DirectoryFieldEditor sdkLocation;

  public CloudSdkPreferencePage() {
    super(GRID);
    // setPreferenceStore(activator.getDefault().getPreferenceStore());
    setPreferenceStore(PreferenceInitializer.getPreferenceStore());
    setDescription("Google Cloud SDK Preferences");
  }

  /**
   * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
   * manipulate various types of preferences. Each field editor knows how to save and restore
   * itself.
   */
  public void createFieldEditors() {
    // need to override checkState() instead of doCheckState() as checkState()
    // wipes out any warning messages
    sdkLocation = new DirectoryFieldEditor(PreferenceConstants.CLOUDSDK_PATH, "&SDK location:",
        getFieldEditorParent()) {
      @Override
      protected boolean checkState() {
        if (!super.checkState()) {
          return false;
        }
        setMessage(null);
        return getStringValue().isEmpty() || validateSdk(new File(getStringValue()));
      }
    };
    sdkLocation.setEmptyStringAllowed(true);
    addField(sdkLocation);
  }

  protected boolean validateSdk(File location) {
    try {
      new CloudSdk.Builder().sdkPath(location).build().validate();
    } catch (AppEngineException e) {
      // accept a seemingly invalid location in case the SDK organization
      // has changed and this code is out of date
      System.out.println("No SDK found: " + e);
      setMessage("No SDK found!", WARNING);
    }
    return true;
  }

  public void init(IWorkbench workbench) {
    this.workbench = workbench;
  }
}
