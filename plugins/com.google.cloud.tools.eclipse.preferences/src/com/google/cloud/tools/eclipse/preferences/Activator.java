package com.google.cloud.tools.eclipse.preferences;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "com.google.cloud.tools.eclipse.preferences";

  // The shared instance
  private static Activator plugin;

  public Activator() {
  }

  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

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
}
