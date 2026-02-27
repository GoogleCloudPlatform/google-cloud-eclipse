/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.ui;

import java.util.EnumSet;

/**
 * Description of the App Engine runtime environments.
 */
public enum AppEngineRuntime {
  STANDARD_JAVA_8(
      Messages.getString("appengine.runtimes.java8"), "java8"), //$NON-NLS-1$ //$NON-NLS-2$
  STANDARD_JAVA_8_SERVLET_25(
      Messages.getString("appengine.runtimes.java8.servlet25"), //$NON-NLS-1$
      "java8"), //$NON-NLS-1$
  STANDARD_JAVA_11_JETTY(
      Messages.getString("appengine.runtimes.java11.jetty"), //$NON-NLS-1$
      "java11"); //$NON-NLS-1$

  public static final EnumSet<AppEngineRuntime> STANDARD_RUNTIMES =
      EnumSet.of(STANDARD_JAVA_8, STANDARD_JAVA_8_SERVLET_25);

  private final String label;
  private final String runtimeId;

  private AppEngineRuntime(String label, String runtimeId) {
    this.label = label;
    this.runtimeId = runtimeId;
  }

  public String getLabel() {
    return label;
  }

  public String getRuntimeId() {
    return runtimeId;
  }
}