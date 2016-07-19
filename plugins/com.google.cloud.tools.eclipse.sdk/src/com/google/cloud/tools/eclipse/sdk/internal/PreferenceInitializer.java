
package com.google.cloud.tools.eclipse.sdk.internal;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.CloudSdkProvider;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

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
    CloudSdk sdk = null;
    try {
      sdk = new CloudSdkProvider().getCloudSdk();
    } catch (AppEngineException aee) {
      // No SDK could be found.
    }
    
    DefaultScope.INSTANCE.getNode(BUNDLEID).put(PreferenceConstants.CLOUDSDK_PATH,
        sdk == null ? "" : sdk.getSdkPath().toAbsolutePath().toString());
  }

  public static IEclipsePreferences getPreferenceNode() {
    return InstanceScope.INSTANCE.getNode(BUNDLEID);
  }
}
