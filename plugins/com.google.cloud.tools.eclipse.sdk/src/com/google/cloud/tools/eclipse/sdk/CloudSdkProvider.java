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

package com.google.cloud.tools.eclipse.sdk;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceConstants;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceInitializer;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.jface.preference.IPreferenceStore;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility to find the Google Cloud SDK either at locations configured by the user or in standard
 * locations on the system.
 */
public class CloudSdkProvider extends ContextFunction {

  private IPreferenceStore preferences = PreferenceInitializer.getPreferenceStore();
  private Path location;
  
  public CloudSdkProvider() {}
  
  @VisibleForTesting
  public CloudSdkProvider(IPreferenceStore preferences) {
    this.preferences = preferences;
  }
  
  // Used when the SDK is being built from the context.
  public CloudSdkProvider(Path location) {
    this.location = location;
  }

  /**
   * Return the {@link CloudSdk} instance from the configured or discovered Cloud SDK.
   * 
   * <p>The used location is the one from IEclipseContext. If there is none, then it is fetched from
   * the preference store. If there is none there, then we let the library find it in the SDK
   * typical locations.
   * 
   * <p>This method ensures that the return SDK is valid, i.e., it contains the most important
   * files.
   * 
   * @return the configured {@link CloudSdk} or {@code null} if no valid SDK could be found
   */
  public CloudSdk getCloudSdk() {
    CloudSdk.Builder sdkBuilder = new CloudSdk.Builder();
    
    String configuredPath = preferences.getString(PreferenceConstants.CLOUDSDK_PATH);
    
    if (location != null) {
      sdkBuilder.sdkPath(location);
    } else if (configuredPath != null && !configuredPath.isEmpty()) {
      sdkBuilder.sdkPath(Paths.get(configuredPath));
    }
    // If no location is set, let library discover the location.
    
    try {
      CloudSdk sdk = sdkBuilder.build();
      sdk.validate();
      return sdk;
    } catch (AppEngineException aee) {
      // If no SDK could be discovered, let the caller prompt the user for a new one.
      return null;
    }
  }
}
