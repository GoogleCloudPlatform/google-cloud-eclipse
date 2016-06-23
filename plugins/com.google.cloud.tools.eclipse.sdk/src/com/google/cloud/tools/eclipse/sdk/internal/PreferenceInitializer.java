
package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.appengine.cloudsdk.PathResolver;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import java.nio.file.Path;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
  static final String BUNDLEID = "com.google.cloud.tools.eclipse.sdk";
  
  public static IPreferenceStore getPreferenceStore() {
    return new ScopedPreferenceStore(InstanceScope.INSTANCE, BUNDLEID);
  }

  @Override
  public void initializeDefaultPreferences() {
    Path path = PathResolver.INSTANCE.getCloudSdkPath();
    DefaultScope.INSTANCE.getNode(BUNDLEID).put(PreferenceConstants.CLOUDSDK_PATH,
        path == null ? "" : path.toString());
  }

  public static IEclipsePreferences getPreferenceNode() {
    return InstanceScope.INSTANCE.getNode(BUNDLEID);
  }
}
