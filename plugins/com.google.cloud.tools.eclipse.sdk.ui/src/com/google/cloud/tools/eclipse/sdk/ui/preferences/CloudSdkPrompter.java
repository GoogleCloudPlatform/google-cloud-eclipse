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
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.io.File;
import java.nio.file.Path;

/**
 * A special Google Cloud SDK provider that will open the Cloud SDK
 * preference page if no location is found. Must be called from the
 * SWT UI Thread.
 */
public class CloudSdkPrompter {

  /**
   * Return the Cloud SDK. If it cannot be found, prompt the user to specify its location. Like
   * {@linkplain CloudSdk.Builder#build()} the caller is responsible for validating the SDK location
   * (if desired).
   * <p>
   * <b>Must be called from the SWT UI Thread.</b>
   * </p>
   * 
   * @param shell the parent shell for any dialogs; may be {@code null}
   * @return the Cloud SDK, or {@code null} if unspecified
   */
  public static CloudSdk getCloudSdk(Shell shell) {
    return getCloudSdk(new SameShellProvider(shell));
  }

  /**
   * Return the Cloud SDK. If it cannot be found, prompt the user to specify its location. Like
   * {@linkplain CloudSdk.Builder#build()} the caller is responsible for validating the SDK location
   * (if desired).
   * 
   * <p>
   * <b>Must be called from the SWT UI Thread.</b>
   * </p>
   * 
   * @param shellProvider an object to provide the parent shell for any dialogs; may be {@code null}
   * @return the Cloud SDK, or {@code null} if unspecified
   */
  public static CloudSdk getCloudSdk(IShellProvider shellProvider) {
    try {
      return new CloudSdk.Builder().build();
    } catch (AppEngineException ex) {
      /* fall through */
    }
    // assumption here is that the CloudSdkPreferenceResolver is in place
    if (promptForSdk(shellProvider)) {
      try {
        // preference was changed so try again
        return new CloudSdk.Builder().build();
      } catch (AppEngineException ex) {
        /* fall through */
      }
    }
    return null;
  }

  /**
   * Return the Cloud SDK location. If it cannot be found, prompt the user to specify its location.
   * Like {@linkplain CloudSdk.Builder#build()} the caller is responsible for validating the SDK
   * location (if desired).
   * <p>
   * <b>Must be called from the SWT UI Thread.</b>
   * </p>
   * 
   * @param shell the parent shell for any dialogs; may be {@code null}
   * @return the Cloud SDK location, or {@code null} if unspecified
   */
  public static File getCloudSdkLocation(Shell shell) {
    return getCloudSdkLocation(new SameShellProvider(shell));
  }

  /**
   * Return the Cloud SDK location. If it cannot be found, prompt the user to specify its location.
   * Like {@linkplain CloudSdk.Builder#build()} the caller is responsible for validating the SDK
   * location (if desired).
   * <p>
   * <b>Must be called from the SWT UI Thread.</b>
   * </p>
   * 
   * @param shellProvider an object to provide the parent shell for any dialogs; may be {@code null}
   * @return the Cloud SDK location, or {@code null} if unspecified
   */
  public static File getCloudSdkLocation(IShellProvider shellProvider) {
    CloudSdk sdk = getCloudSdk(shellProvider);
    if (sdk == null) {
      return null;
    }
    Path location = sdk.getSdkPath();
    if (location != null) {
      return location.toFile();
    }
    return null;
  }

  /**
   * Prompt the user to install and configure the Google Cloud SDK.
   * 
   * @param shellProvider an object that knows how to obtain a shell; may be {@code null}
   * @return true if the user appears to have configured the SDK, or false if the SDK is unavailable
   */
  static boolean promptForSdk(IShellProvider shellProvider) {
    if (!MessageDialog.openQuestion(null, SdkUiMessages.CloudSdkPrompter_0,
        SdkUiMessages.CloudSdkPrompter_1)) {
      return false;
    }
    Shell shell = shellProvider == null ? null : shellProvider.getShell();
    final PreferenceDialog dialog =
        PreferencesUtil.createPreferenceDialogOn(shell, CloudSdkPreferenceArea.PAGE_ID, null, null);
    return dialog.open() == PreferenceDialog.OK;
  }

  // should not be instantiated
  private CloudSdkPrompter() {}
}
