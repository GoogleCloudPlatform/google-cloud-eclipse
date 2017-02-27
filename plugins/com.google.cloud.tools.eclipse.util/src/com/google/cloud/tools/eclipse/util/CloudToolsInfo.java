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

package com.google.cloud.tools.eclipse.util;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;

/**
 * Provides generic information about the plug-in, such as a name to be used for usage
 * reporting and the current version, etc.
 */
public class CloudToolsInfo {

  /**
   * Our main feature identifier, used for branding.
   */
  private static final String CLOUD_TOOLS_FOR_ECLIPSE_FEATURE_ID =
      "com.google.cloud.tools.eclipse.suite.e45.feature";

  // Don't change the value; this name is used as an originating "application" of usage metrics.
  public static String METRICS_NAME = "gcloud-eclipse-tools";

  public static String USER_AGENT = METRICS_NAME + "/" + getToolsVersion();

  /** Return the version of associated feature. May be slow. */
  public static String getToolsVersion() {
    for(IBundleGroupProvider provider : Platform.getBundleGroupProviders()) {
      for(IBundleGroup feature : provider.getBundleGroups()) {
        if(CLOUD_TOOLS_FOR_ECLIPSE_FEATURE_ID.equals(feature.getIdentifier())) {
          return feature.getVersion();
        }
      }
    }
    // May not have been installed with via a feature. Although we could report the bundle version,
    // that may result in a confusing versions.
    return "UNKNOWN";
  }
}
