/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.sdk;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.ui.internal.WorkbenchPlugin;

public class CloudSdkManager {

  private static final String OPTION_MANAGED_CLOUD_SDK =
      "com.google.cloud.tools.eclipse.sdk/managed.cloud.sdk";

  public final static boolean MANAGED_SDK;

  static {
    WorkbenchPlugin activator = WorkbenchPlugin.getDefault();
    if (activator == null) {
      MANAGED_SDK = false;
    } else {
      DebugOptions debugOptions = activator.getDebugOptions();
      if (debugOptions == null) {
        MANAGED_SDK = false;
      } else {
        MANAGED_SDK = debugOptions.getBooleanOption(OPTION_MANAGED_CLOUD_SDK, false);
      }
    }
  }
}
