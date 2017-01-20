/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.dialog.DialogPageSupport;
import org.eclipse.jface.preference.PreferencePage;

/**
 * Re-implementation of {@link org.eclipse.jface.databinding.preference.PreferencePageSupport}
 * to support showing a validation ERROR message as a different severity type message (e.g., INFO)
 * while disabling the 'Apply' or 'OK' buttons.
 *
 * Related issue: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1120
 */
public class CustomPreferencePageSupport extends DialogPageSupport {

  CustomPreferencePageSupport(
      PreferencePage preferencePage, DataBindingContext dataBindingContext) {
    super(preferencePage, dataBindingContext);
  }

  // Based on PreferencePageSupport.handleStatusChanged().
  @Override
  protected void handleStatusChanged() {
    super.handleStatusChanged();
    boolean valid = computeValid(currentStatusStale, currentStatus);
    ((PreferencePage) getDialogPage()).setValid(valid);
  }

  // Based on PreferencePageSupport.handleStatusChanged().
  @VisibleForTesting
  static boolean computeValid(boolean statusStale, IStatus status) {
    if (statusStale) {
      return false;
    } else if (status == null) {
      return true;
    }

    return !status.matches(IStatus.ERROR | IStatus.CANCEL)
        // Additionally check plug-in-specific error code.
        && status.getCode() != IStatus.ERROR && status.getCode() != IStatus.CANCEL;
  }
}

