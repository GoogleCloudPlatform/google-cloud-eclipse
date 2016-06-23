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
import com.google.cloud.tools.appengine.cloudsdk.PathResolver;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceConstants;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceInitializer;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.jface.preference.IPreferenceStore;

import java.io.File;
import java.nio.file.Path;

/**
 * Utililty to find the Google Cloud SDK either at locations configured by the user or in standard
 * locations on the system.
 */
public class CloudSdkProvider extends ContextFunction {

  /**
   * Return the {@link CloudSdk} instance from the configured or discovered Cloud SDK.
   * 
   * @return the configured {@link CloudSdk} or {@code null} if no SDK could be found
   */
  public static CloudSdk getCloudSdk() {
    return createBuilder(null).build();
  }

  /**
   * Return the location of the configured or discovered Cloud SDK.
   * 
   * @return the configured location or {@code null} if the SDK could not be found
   */
  public static File getCloudSdkLocation() {
    return resolveSdkLocation();
  }

  /**
   * Return a {@link CloudSdk.Builder} instance, suitable for creating a {@link CloudSdk} instance.
   * {@code location}, if not null, is used as the location to the Cloud SDK, otherwise the
   * configured or discovered Cloud SDK will be used instead.
   * 
   * @param location if not {@code null}, overrides the default location for the SDK; can be a
   *        {@linkplain String}, {@linkplain File}, or {@linkplain Path}.
   * @return a builder, or {@code null} if the Google Cloud SDK cannot be located
   */
  public static CloudSdk.Builder createBuilder(File location) {
    // perhaps should try to be cleverer in case location references the .../bin/gcloud
    if (location == null || !location.exists()) {
      location = resolveSdkLocation();
    }
    if (location == null || !location.exists()) {
      return null;
    }
    return new CloudSdk.Builder().sdkPath(location);
  }

  /**
   * Attempt to resolve the Google Cloud SDK from the configured location or try to discover its
   * location.
   * 
   * @return the location, or {@code null} if not found
   */
  private static File resolveSdkLocation() {
    IPreferenceStore preferences = PreferenceInitializer.getPreferenceStore();
    String value = preferences.getString(PreferenceConstants.CLOUDSDK_PATH);
    if (value != null && !value.isEmpty()) {
      return new File(value);
    }
    Path discovered = PathResolver.INSTANCE.getCloudSdkPath();
    if (discovered != null) {
      return discovered.toFile();
    }
    return null;
  }

  // should not be instantiated
  private CloudSdkProvider() {}
}
