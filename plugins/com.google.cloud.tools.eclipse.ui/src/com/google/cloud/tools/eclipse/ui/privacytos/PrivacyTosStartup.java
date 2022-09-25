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

import com.google.cloud.tools.eclipse.util.MessageDialogWithToggleAndLink;
import com.google.common.collect.ImmutableList;
import java.util.logging.Logger;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Displays Privacy and TOS statement at startup
 */
public class PrivacyTosStartup implements IStartup {

  private static final Logger logger = Logger.getLogger(PrivacyTosStartup.class.getName());
  private static final String PRIVACY_TOS_TITLE = "Google Cloud Tools for Eclipse Privacy and TOS";
  private static final String PRIVACY_TOS_MESSAGE = "Please read our Privacy Statement and TOS before proceeding with the tool.";
  private static final String PREFERENCE_PERSISTENCE_KEY = "com.google.cloud.tools.eclipse.ui.privacytos.displayprivacytos";
  private static final String PREFERENCE_NODE_KEY = "com.google.cloud.tools.eclipse.ui.privacytos";
  private static final String[] STATEMENT_LINKS = {
      "<a href=\"https://policies.google.com/privacy\"> Privacy Policy </a>",
      "<a href=\"https://policies.google.com/terms\"> Terms of Service </a>"
  };
  @Override
  public void earlyStartup() {
    // TODO Determine persistent way of "do not show again"
    IWorkbench workbench = PlatformUI.getWorkbench();
    workbench.getDisplay().asyncExec(new Runnable() {
      public void run() {
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window != null) {

          IEclipsePreferences preferenceStore =
              InstanceScope.INSTANCE.getNode(PREFERENCE_NODE_KEY);
          Preferences preferences = preferenceStore.node(PREFERENCE_NODE_KEY);
          String showAgain = preferences.get(PREFERENCE_PERSISTENCE_KEY, MessageDialogWithToggle.PROMPT);
          if (showAgain.equals(MessageDialogWithToggle.PROMPT)) {
            MessageDialogWithToggle dialog = MessageDialogWithToggleAndLink.openOkCancelConfirmLinks(
                null,
                PRIVACY_TOS_TITLE, 
                PRIVACY_TOS_MESSAGE, 
                "Do not show again", 
                false, 
                ImmutableList.copyOf(STATEMENT_LINKS)
            );  
            boolean toggleResponse = dialog.getToggleState();
            String newPreference = toggleResponse ? MessageDialogWithToggle.NEVER : MessageDialogWithToggle.PROMPT;

            preferences.put(PREFERENCE_PERSISTENCE_KEY, newPreference);
            try {
              // forces the application to save the preferences
              preferenceStore.flush();
            } catch (BackingStoreException e) {
              logger.severe("Invalid preference specification: " + e);
            }
          }         
        }
      }
    });
  }

}
