package com.google.cloud.tools.eclipse.appengine.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Accessors for shared icons.
 */
public class AppEngineImages {
  
  public static ImageDescriptor googleCloudPlatform(int size) {
    String imageFilePath = "icons/gcp-" + size + "x" + size + ".png";
    return getIcon(imageFilePath);
  }

  private static ImageDescriptor getIcon(String path) {
    return AbstractUIPlugin.imageDescriptorFromPlugin(
        "com.google.cloud.tools.eclipse.appengine.ui", path);
  }

}
