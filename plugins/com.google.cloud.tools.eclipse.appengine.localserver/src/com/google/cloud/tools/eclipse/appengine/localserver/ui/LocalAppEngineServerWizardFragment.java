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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.sdk.ui.preferences.CloudSdkPreferenceArea;
import com.google.common.annotations.VisibleForTesting;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

public class LocalAppEngineServerWizardFragment extends WizardFragment {

  private String cloudSdkPath = null;
  private boolean dialogFinished = false;
  private boolean enableFinishButton;

  public LocalAppEngineServerWizardFragment() {
    cloudSdkPath = getCloudSdkLocation();
  }

  @VisibleForTesting
  LocalAppEngineServerWizardFragment(String cloudSdkPath) {
    this.cloudSdkPath = cloudSdkPath;
  }

  @Override
  public boolean hasComposite() {
    return true;
  }

  @Override
  public boolean isComplete() {
    return enableFinishButton || cloudSdkPath != null;
  }

  @Override
  public void enter() {
    // TODO why do we check here AND in the constructor?
    cloudSdkPath = getCloudSdkLocation();
    dialogFinished = false;
    enableFinishButton = true;
  }

  @Override
  public void exit() {
    enableFinishButton = false;
  }

  @Override
  public Composite createComposite(Composite parent, IWizardHandle wizard) {
    wizard.setTitle(Messages.CREATE_APP_ENGINE_RUNTIME_WIZARD_TITLE);
    wizard.setDescription(Messages.CREATE_APP_ENGINE_RUNTIME_WIZARD_DESCRIPTION);

    Composite cloudSdkComposite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    cloudSdkComposite.setLayout(layout);

    Label label = new Label(cloudSdkComposite, SWT.NONE);
    
    if (cloudSdkPath != null) {
      String cloudSdkFound = NLS.bind(Messages.RUNTIME_WIZARD_CLOUD_SDK_FOUND, cloudSdkPath);
      label.setText(cloudSdkFound);
    } else {
      label.setText(Messages.RUNTIME_WIZARD_CLOUD_SDK_NOT_FOUND);
      Button cloudSdkButton = new Button(cloudSdkComposite, SWT.CHECK);
      cloudSdkButton.setText(Messages.OPEN_CLOUD_SDK_PREFERENCE_BUTTON);
      parent.addDisposeListener(new OpenPreferencePage(cloudSdkButton));
    }
    
    return cloudSdkComposite;
  }

  @Override
  public void performFinish(IProgressMonitor monitor) throws CoreException {
    dialogFinished = true;
  }

  private String getCloudSdkLocation() {
    try {
      CloudSdk cloudSdk = new CloudSdk.Builder().build();
      return cloudSdk.getSdkPath().toString();
    } catch (AppEngineException ex) {
      return null;
    }
  }

  private final class OpenPreferencePage implements DisposeListener {
    private final Button cloudSdkButton;

    private OpenPreferencePage(Button cloudSdkButton) {
      this.cloudSdkButton = cloudSdkButton;
    }

    @Override
    public void widgetDisposed(DisposeEvent event) {
      if (dialogFinished && cloudSdkButton.getSelection()) {
        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null,
            CloudSdkPreferenceArea.PAGE_ID, null, null);
        dialog.open();
      }
    }
  }

}
