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

package com.google.cloud.tools.eclipse.usagetracker;

import com.google.cloud.tools.eclipse.preferences.areas.PreferenceArea;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorDialogErrorHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;

public class AnalyticsOptInArea extends PreferenceArea {

  private Button optInButton;

  /**
   * Create the area contents. Not intended to be called outside of the preference area framework.
   *
   * @noreference
   */
  @Override
  public Control createContents(Composite container) {
    // Opt-in checkbox with a label
    optInButton = new Button(container, SWT.CHECK);
    optInButton.setText(Messages.getString("PREFERENCE_PAGE_OPT_IN_LABEL"));
    optInButton.addSelectionListener(new SelectionListener() {
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
    Link privacyDisclaimer = new Link(container, SWT.NONE);
    privacyDisclaimer.setText(Messages.getString("PREFERENCE_PAGE_PRIVACY_DISCLAIMER"));
    privacyDisclaimer.setFont(optInButton.getFont());
    privacyDisclaimer.addSelectionListener(
        new OpenUriSelectionListener(new ErrorDialogErrorHandler(container.getShell())));

    load();
    return container;
  }

  @Override
  public IStatus getStatus() {
    return Status.OK_STATUS;
  }

  @Override
  public void load() {
    optInButton
        .setSelection(getPreferenceStore().getBoolean(AnalyticsPreferences.ANALYTICS_OPT_IN));
  }

  @Override
  public void loadDefault() {
    optInButton.setSelection(
        getPreferenceStore().getDefaultBoolean(AnalyticsPreferences.ANALYTICS_OPT_IN));
  }

  @Override
  public void performApply() {
    getPreferenceStore().setValue(AnalyticsPreferences.ANALYTICS_OPT_IN,
        optInButton.getSelection());
  }
}
