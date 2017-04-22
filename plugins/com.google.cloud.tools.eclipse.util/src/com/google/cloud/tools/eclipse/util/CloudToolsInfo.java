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

import com.google.common.annotations.VisibleForTesting;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;

/**
 * Provides generic information about the plug-in, such as a name to be used for usage
 * reporting and the current version, etc.
 */
public class CloudToolsInfo {
  private static final Logger logger = Logger.getLogger(CloudToolsInfo.class.getName());

  /**
   * Our main feature identifier, used for branding.
   */
  @VisibleForTesting
  static final String CLOUD_TOOLS_FOR_ECLIPSE_FEATURE_ID =
      "com.google.cloud.tools.eclipse.suite.e45.feature";

  // Don't change the value; this name is used as an originating "application" of usage metrics.
  public static final String METRICS_NAME = "gcloud-eclipse-tools";

  public static final String EXCEPTION_REPORT_PRODUCT_ID = "CT4E";

  public static final String USER_AGENT = METRICS_NAME + "/" + getToolsVersion();

  /** Return the version of associated Cloud Tools for Eclipse feature, or 0.0.0 if unknown. */
  public static String getToolsVersion() {
    return getToolsVersion(Platform.getBundleGroupProviders());
  }

  @VisibleForTesting
  static String getToolsVersion(IBundleGroupProvider[] bundleGroupProviders) {
    for (IBundleGroupProvider provider : bundleGroupProviders) {
      for (IBundleGroup feature : provider.getBundleGroups()) {
        if (CLOUD_TOOLS_FOR_ECLIPSE_FEATURE_ID.equals(feature.getIdentifier())) {
          return feature.getVersion();
        }
      }
    }
    // May not have been installed with via a feature. Although we could report the bundle version,
    // that may result in a confusing versions.
    logger.fine("Feature not found: " + CLOUD_TOOLS_FOR_ECLIPSE_FEATURE_ID);
    return "0.0.0";
  }
}
