
package com.google.cloud.tools.eclipse.sdk.internal;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

  private IPreferenceChangeListener listener = new IPreferenceChangeListener() {
    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
      Object newValue = event.getNewValue();
      if (PreferenceConstants.CLOUDSDK_PATH.equals(event.getKey())
          && (newValue == null || newValue instanceof String)) {
        CloudSdkContextFunction.sdkPathChanged((String) newValue);
      }
    }
  };

  @Override
  public void start(BundleContext context) throws Exception {
    PreferenceConstants.getPreferenceNode().addPreferenceChangeListener(listener);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    PreferenceConstants.getPreferenceNode().removePreferenceChangeListener(listener);
  }
}
