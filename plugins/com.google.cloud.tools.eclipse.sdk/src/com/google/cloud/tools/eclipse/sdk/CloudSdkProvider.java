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

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceConstants;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceInitializer;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.jface.preference.IPreferenceStore;

import java.nio.file.Paths;

/**
 * Utility to find the Google Cloud SDK either at locations configured by the user or in standard
 * locations on the system.
 */
public class CloudSdkProvider extends ContextFunction {

  private final IPreferenceStore preferences = PreferenceInitializer.getPreferenceStore();

  /**
   * Return the {@link CloudSdk} instance from the configured or discovered Cloud SDK.
   * 
   * @return the configured {@link CloudSdk} or {@code null} if no SDK could be found
   */
  public CloudSdk getCloudSdk() {
    CloudSdk.Builder sdkBuilder = new CloudSdk.Builder();
    
    String configuredPath = preferences.getString(PreferenceConstants.CLOUDSDK_PATH);
    
    // Let's use the configured path, if there is one. Otherwise, the lib will try to discover the
    // path.
    if (configuredPath != null && !configuredPath.isEmpty()) {
    	// TODO(joaomartins): What happens when the provided path is invalid? Tools lib isn't
    	// calling validate().
    	sdkBuilder.sdkPath(Paths.get(configuredPath));
    }
    
    return sdkBuilder.build();
  }
}
