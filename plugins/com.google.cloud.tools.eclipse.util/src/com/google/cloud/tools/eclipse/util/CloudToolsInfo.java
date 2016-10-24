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

import org.osgi.framework.FrameworkUtil;

/**
 * Provides generic information about the plug-in, such as a name to be used for usage
 * reporting and the current version, etc.
 */
public class CloudToolsInfo {

  // Don't change the value; this name is used as an originating "application" of usage metrics.
  public static String METRICS_NAME = "gcloud-eclipse-tools";

  public static String USER_AGENT = METRICS_NAME + "/" + getToolsVersion();

  public static String getToolsVersion() {
    return FrameworkUtil.getBundle(new CloudToolsInfo().getClass()).getVersion().toString();
  }
}
