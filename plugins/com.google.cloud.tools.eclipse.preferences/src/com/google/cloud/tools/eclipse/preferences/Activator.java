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

package com.google.cloud.tools.eclipse.preferences;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "com.google.cloud.tools.eclipse.preferences";

  private static final ScopedPreferenceStore preferenceStore =
      new ScopedPreferenceStore(ConfigurationScope.INSTANCE, PLUGIN_ID);

  // The shared instance
  private static Activator plugin;

  public Activator() {
  }

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
  public static Activator getDefault() {
    return plugin;
  }

  /**
   * Returns an image descriptor for the image file at the given
   * plug-in relative path.
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  /**
   * This method is not supposed to be overridden, but we couldn't find any alternative
   * to replace the Instance-scoped {@link AbstractUIPlugin#preferenceStore} in the base
   * class with a Configuration-scoped {@link ScopedPreferenceStore}. As such, this class
   * defines its own {@link preferenceStore} field that is distinct from the field by the
   * same name in the base class.
   */
  @Override
  public IPreferenceStore getPreferenceStore() {
    return preferenceStore;
  }
}
