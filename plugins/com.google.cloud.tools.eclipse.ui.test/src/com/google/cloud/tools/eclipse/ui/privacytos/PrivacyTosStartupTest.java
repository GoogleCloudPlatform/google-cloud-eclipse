/*
 * Copyright 2022 Google LLC
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

package com.google.cloud.tools.eclipse.ui.privacytos;



import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.prefs.Preferences;


@RunWith(MockitoJUnitRunner.class)
public class PrivacyTosStartupTest {
  private PrivacyTosStartup ptStartup;
  
  public static void configureShowAgain(boolean showAgain) {
    IEclipsePreferences preferenceStore =
        InstanceScope.INSTANCE.getNode(PrivacyTosStartup.PREFERENCE_NODE_KEY);
    Preferences preferences = preferenceStore.node(PrivacyTosStartup.PREFERENCE_NODE_KEY);
    preferences.put(PrivacyTosStartup.PREFERENCE_PERSISTENCE_KEY, showAgain ? MessageDialogWithToggle.PROMPT : MessageDialogWithToggle.NEVER);
  }
  
  @BeforeClass
  public static void setup() {
    configureShowAgain(false);
  }
  
  @Test(expected = Test.None.class /* no exception expected */)
  public void testRun() {
    configureShowAgain(true);
    ptStartup = new PrivacyTosStartup();
    ptStartup.earlyStartup();   
  }
}
