/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.launching.JavaRuntime;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class DataflowCorePlugin extends Plugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.cloud.tools.eclipse.dataflow.core"; //$NON-NLS-1$

  // The shared instance
  private static DataflowCorePlugin plugin;

  public DataflowCorePlugin() {}

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    // Initalizes the compliance options to include the default VM's compliance level.
    JavaRuntime.getDefaultVMInstall();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance.
   */
  public static DataflowCorePlugin getDefault() {
    return plugin;
  }

  private void log(IStatus status) {
    getLog().log(status);
  }

  public IEclipsePreferences getPreferences() {
    return InstanceScope.INSTANCE.getNode(PLUGIN_ID);
  }

  /**
   * Logs the provided message at the OK log level.
   */
  public static void logOk(String message, Object... fArgs) {
    getDefault().log(new Status(Status.OK, PLUGIN_ID, String.format(message, fArgs)));
  }

  /**
   * Logs the provided message at the INFO log level.
   */
  public static void logInfo(String message, Object... fArgs) {
    getDefault().log(new Status(Status.INFO, PLUGIN_ID, String.format(message, fArgs)));
  }

  /**
   * Logs the provided message with the provided cause at the WARNING log level.
   */
  public static void logWarning(Throwable cause, String message, Object... fArgs) {
    getDefault().log(new Status(Status.WARNING, PLUGIN_ID, String.format(message, fArgs), cause));
  }

  /**
   * Logs the provided message at the WARNING log level.
   */
  public static void logWarning(String message, Object... fArgs) {
    getDefault().log(new Status(Status.WARNING, PLUGIN_ID, String.format(message, fArgs)));
  }

  /**
   * Logs the provided message  at the ERROR log level.
   */
  public static void logError(String message, Object... fArgs) {
    getDefault().log(new Status(Status.ERROR, PLUGIN_ID, String.format(message, fArgs)));
  }

  /**
   * Logs the provided message with the provided cause at the ERROR log level.
   */
  public static void logError(Throwable cause, String message, Object... fArgs) {
    getDefault().log(new Status(Status.ERROR, PLUGIN_ID, String.format(message, fArgs), cause));
  }
}
