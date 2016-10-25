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

import com.google.cloud.tools.eclipse.preferences.areas.PreferenceArea;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnalyticsOptInArea extends PreferenceArea {

  private static final Logger logger = Logger.getLogger(AnalyticsOptInArea.class.getName());

  private Button optInStatusEditor;

  /**
   * Create the area contents. Not intended to be called outside of the preference area framework.
   * 
   * @noreference
   */
  @Override
  public Control createContents(Composite container) {
    // Opt-in checkbox with a label
    optInStatusEditor = new Button(container, SWT.CHECK);
    optInStatusEditor.setText(Messages.ANALYTICS_OPT_IN_TEXT);
    optInStatusEditor.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        boolean value = ((Button) event.widget).getSelection();
        fireValueChanged(VALUE, !value, value);
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent event) {
        boolean value = ((Button) event.widget).getSelection();
        fireValueChanged(VALUE, !value, value);
      }
    });

    // The privacy policy disclaimer with a clickable link
    Link privacyPolicyDisclaimer = new Link(container, SWT.NONE);
    privacyPolicyDisclaimer.setText(Messages.ANALYTICS_DISCLAIMER);
    privacyPolicyDisclaimer.setFont(optInStatusEditor.getFont());
    privacyPolicyDisclaimer.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        // Open a privacy policy web page when the link is clicked.
        try {
          URL url = new URL(Messages.GOOGLE_PRIVACY_POLICY_URL);
          IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
          browserSupport.createBrowser(null).openURL(url);
        } catch (MalformedURLException mue) {
          logger.log(Level.WARNING, "URL malformed", mue);
        } catch (PartInitException pie) {
          logger.log(Level.WARNING, "Cannot launch a browser", pie);
        }
      }
    });
    
    load();
    return container;
  }

  @Override
  public IStatus getStatus() {
    return Status.OK_STATUS;
  }

  @Override
  public void load() {
    optInStatusEditor
        .setSelection(getPreferenceStore().getBoolean(AnalyticsPreferences.ANALYTICS_OPT_IN));
  }

  @Override
  public void loadDefault() {
    optInStatusEditor.setSelection(
        getPreferenceStore().getDefaultBoolean(AnalyticsPreferences.ANALYTICS_OPT_IN));
  }

  @Override
  public void performApply() {
    getPreferenceStore().setValue(AnalyticsPreferences.ANALYTICS_OPT_IN,
        optInStatusEditor.getSelection());
  }
}
