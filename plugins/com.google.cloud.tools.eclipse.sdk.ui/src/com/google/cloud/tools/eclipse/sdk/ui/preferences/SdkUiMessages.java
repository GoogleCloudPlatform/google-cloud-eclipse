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

import org.eclipse.osgi.util.NLS;

public class SdkUiMessages extends NLS {
  
  // todo Ick! Mutable public fields. Use the other kind of messages.
  
  private static final String BUNDLE_NAME =
      "com.google.cloud.tools.eclipse.sdk.ui.preferences.messages"; //$NON-NLS-1$
  public static String CloudSdkPreferencePage_0;
  public static String CloudSdkPreferencePage_1;
  public static String CloudSdkPreferencePage_2;
  public static String CloudSdkPreferencePage_3;
  public static String CloudSdkPreferencePage_4;
  public static String CloudSdkPreferencePage_5;
  public static String CloudSdkPreferencePage_6;
  public static String AppEngineJavaComponentsNotInstalled;
  public static String CloudSdkPrompter_0;
  public static String CloudSdkPrompter_1;
  public static String openBrowse;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, SdkUiMessages.class);
  }

  private SdkUiMessages() {}
}
