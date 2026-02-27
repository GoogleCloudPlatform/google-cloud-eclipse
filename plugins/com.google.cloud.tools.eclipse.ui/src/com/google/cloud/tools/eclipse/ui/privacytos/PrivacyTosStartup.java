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

import com.google.cloud.tools.eclipse.ui.util.MessageDialogWithToggleAndLink;
import com.google.cloud.tools.eclipse.ui.util.Messages;
import com.google.common.annotations.VisibleForTesting;
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
  private static final String PRIVACY_TOS_TITLE = Messages.getString("privacytos.message.title");
  private static final String PRIVACY_TOS_MESSAGE = Messages.getString("privacytos.message");
  public static final String PREFERENCE_PERSISTENCE_KEY = "com.google.cloud.tools.eclipse.ui.privacytos.displayprivacytos";
  public static final String PREFERENCE_NODE_KEY = "com.google.cloud.tools.eclipse.ui.privacytos";
  private static final String[] STATEMENT_LINKS = {
      Messages.getString("privacytos.links.privacy"),
      Messages.getString("privacytos.links.tos"),
  };
  private final IWorkbench workbench;
  
  public PrivacyTosStartup() {
    this(PlatformUI.getWorkbench());
  }
  @VisibleForTesting
  PrivacyTosStartup(IWorkbench workbench) {
    this.workbench = workbench;
  }
  
  private boolean isDisabled() {
    String isDisabledStr = System.getProperty("disablePrivacyTos");
    return isDisabledStr == null ? false : Boolean.valueOf(isDisabledStr);
  }
  
  @Override
  public void earlyStartup() {
    if (isDisabled()) {
      return;
    }
    workbench.getDisplay().asyncExec(new Runnable() {
      @Override
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
                Messages.getString("toggle.message.showagain"),
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
