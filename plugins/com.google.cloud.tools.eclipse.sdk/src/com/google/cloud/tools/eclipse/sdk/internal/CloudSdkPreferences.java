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

package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Class for Cloud SDK preferences: defining constants, accessing their preference store and node,
 * initializing default values, etc.
 */
public final class CloudSdkPreferences extends AbstractPreferenceInitializer {

  private static final Logger logger = Logger.getLogger(CloudSdkPreferences.class.getName());

  // host bundle for the preference
  static final String BUNDLEID = "com.google.cloud.tools.eclipse.sdk";

  public static enum CloudSdkManagementOption {
    AUTOMATIC, MANUAL
  }

  /**
   * Preference name for how the Google Cloud SDK is managed. Actual values are the names (strings)
   * of the {@code enum CloudSdkManagementOption} constants.
   *
   * @see CloudSdkManagementOption#name()
   */
  public static final String CLOUD_SDK_MANAGEMENT = "cloudSdkManagement";

  /**
   * Preference name for the path to the Google Cloud SDK.
   */
  public static final String CLOUD_SDK_PATH = "cloudSdkPath";

  static IPreferenceStore getPreferenceStore() {
    return new ScopedPreferenceStore(InstanceScope.INSTANCE, BUNDLEID);
  }

  static IEclipsePreferences getPreferenceNode() {
    return InstanceScope.INSTANCE.getNode(BUNDLEID);
  }

  public static boolean isAutoManaging() {
    return isAutoManaging(getPreferenceStore());
  }

  @VisibleForTesting
  static boolean isAutoManaging(IPreferenceStore preferences) {
    return preferences.contains(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)
        && preferences.getString(CLOUD_SDK_MANAGEMENT).equals(
            CloudSdkManagementOption.AUTOMATIC.name());
  }

  @Override
  public void initializeDefaultPreferences() {
    initializeDefaultPreferences(getPreferenceStore());
  }

  @VisibleForTesting
  static void initializeDefaultPreferences(IPreferenceStore preferences) {
    if (!preferences.contains(CLOUD_SDK_MANAGEMENT)) {
      // If the CLOUD_SDK_MANAGEMENT preference has not been set, then determine the
      // appropriate setting. Note that CloudSdkPreferenceResolver only checks for
      // the Managed Cloud SDK when CLOUD_SDK_MANAGEMENT has been explicitly set
      // (i.e., this code has been run).
      configureManagementPreferences(preferences, isCloudSdkAvailable());
    }
    preferences.setDefault(CLOUD_SDK_MANAGEMENT, CloudSdkManagementOption.AUTOMATIC.name());
  }

  /** Configure the managed SDK settings given current settings. */
  @VisibleForTesting
  static void configureManagementPreferences(
      IPreferenceStore preferences, boolean cloudSdkAvailable) {
    // has the user previously set the Cloud SDK path? has it been found in a well-known location?
    if (!Strings.isNullOrEmpty(preferences.getString(CLOUD_SDK_PATH)) || cloudSdkAvailable) {
      preferences.setValue(CLOUD_SDK_MANAGEMENT, CloudSdkManagementOption.MANUAL.name());
    } else {
      preferences.setValue(CLOUD_SDK_MANAGEMENT, CloudSdkManagementOption.AUTOMATIC.name());
    }
    flushPreferences(getPreferenceNode());
  }

  @VisibleForTesting
  static void flushPreferences(IEclipsePreferences preferenceNode) {
    try {
      preferenceNode.flush();
    } catch (BackingStoreException ex) {
      logger.log(Level.WARNING, "could not save preferences", ex);
    }
  }

  /** Return {@code true} if the Cloud SDK is available. */
  private static boolean isCloudSdkAvailable() {
    try {
      new CloudSdk.Builder().build();
      return true;
    } catch (CloudSdkNotFoundException ex) {
      return false;
    }
  }
}
