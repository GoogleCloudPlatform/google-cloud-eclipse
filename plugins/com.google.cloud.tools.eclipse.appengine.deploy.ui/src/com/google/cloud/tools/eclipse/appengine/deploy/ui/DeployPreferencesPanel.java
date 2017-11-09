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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.swt.widgets.Composite;

public abstract class DeployPreferencesPanel extends Composite {

  DeployPreferencesPanel(Composite parent, int style) {
    super(parent, style);
  }

  abstract DataBindingContext getDataBindingContext();

  abstract void resetToDefaults();

  abstract boolean savePreferences();

  @Override
  public void dispose() {
    if (getDataBindingContext() != null) {
      getDataBindingContext().dispose();
    }
    super.dispose();
  }

  protected abstract String getHelpContextId();
}
