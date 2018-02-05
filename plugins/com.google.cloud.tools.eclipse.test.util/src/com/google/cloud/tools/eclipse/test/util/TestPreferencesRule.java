/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.test.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.junit.rules.ExternalResource;
import org.osgi.service.prefs.BackingStoreException;

/** Test utility to create an empty {@link IEclipsePreferences} or {@link IPreferenceStore}. */
public class TestPreferencesRule extends ExternalResource {
  private static final Logger logger = Logger.getLogger(TestPreferencesRule.class.getName());

  private String storeId;
  private IEclipsePreferences preferences;

  @Override
  protected void before() throws Throwable {
    storeId = "prefs" + Double.toString(Math.random()).replace(".", "");
    preferences = InstanceScope.INSTANCE.getNode(storeId);
    super.before();
  }
  
  public IEclipsePreferences getNode() {
    return preferences;
  }
  
  public IPreferenceStore getPreferenceStore() {
    return new ScopedPreferenceStore(InstanceScope.INSTANCE, storeId);
  }

  @Override
  protected void after() {
    try {
      preferences.removeNode();
    } catch (BackingStoreException ex) {
      logger.log(Level.FINE, "Failed clearing preferences: " + storeId, ex);
    }
    super.after();
  }
}
