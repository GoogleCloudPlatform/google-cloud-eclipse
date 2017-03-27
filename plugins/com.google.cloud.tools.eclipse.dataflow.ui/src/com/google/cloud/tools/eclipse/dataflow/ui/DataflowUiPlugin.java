/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class DataflowUiPlugin extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.cloud.tools.eclipse.dataflow.ui"; //$NON-NLS-1$

  // The shared instance
  private static DataflowUiPlugin plugin;

  public DataflowUiPlugin() {}

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance.
   */
  public static DataflowUiPlugin getDefault() {
    return plugin;
  }

  public static IDialogSettings getDialogSettingsSection(String sectionName) {
    IDialogSettings settings = plugin.getDialogSettings();
    IDialogSettings section = settings.getSection(sectionName);
    if (section == null) {
      section = settings.addNewSection(sectionName);
    }
    return section;
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in relative path.
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  private static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static void logInfo(String message, Object... fArgs) {
    log(new Status(IStatus.INFO, PLUGIN_ID, String.format(message, fArgs)));
  }

  public static void logWarning(String message, Object... fArgs) {
    log(new Status(IStatus.WARNING, PLUGIN_ID, String.format(message, fArgs)));
  }

  public static void logError(Throwable e, String message, Object... fArgs) {
    String formattedMessage = String.format(message, fArgs);
    log(new Status(IStatus.ERROR, PLUGIN_ID, formattedMessage, e));
  }

  public static Shell getActiveWindowShell() {
    return getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
  }
}
