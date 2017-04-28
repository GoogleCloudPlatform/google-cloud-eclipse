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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class BlankDeployPreferencesPanel extends DeployPreferencesPanel {

  BlankDeployPreferencesPanel(Composite parent) {
    super(parent, SWT.NONE);
    new Label(this, SWT.WRAP).setText(Messages.getString("no.facet.message"));
    GridLayoutFactory.fillDefaults().generateLayout(this);
  }

  @Override
  DataBindingContext getDataBindingContext() {
    return new DataBindingContext();
  }

  @Override
  void resetToDefaults() {
  }

  @Override
  boolean savePreferences() {
    return true;
  }

  @Override
  protected String getHelpContextId() {
    return null;
  }
}
