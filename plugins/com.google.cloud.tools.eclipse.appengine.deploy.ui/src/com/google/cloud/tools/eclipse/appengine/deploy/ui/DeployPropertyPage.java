/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.databinding.preference.PreferencePageSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.cloud.tools.eclipse.util.AdapterUtil;

public class DeployPropertyPage extends PropertyPage {

  private DeployPreferencesPanel content;

  @Override
  protected Control createContents(Composite parent) {
    IProject project = AdapterUtil.adapt(getElement(), IProject.class);
    content = new DeployPreferencesPanel(parent, project, getLayoutChangedHandler());
    PreferencePageSupport.create(this, content.getDataBindingContext());
    return content;
  }

  private Runnable getLayoutChangedHandler() {
    return new Runnable() {

      @Override
      public void run() {
        // resize the page to work around https://bugs.eclipse.org/bugs/show_bug.cgi?id=265237
        Composite parent = content.getParent();
        while (parent != null) {
          if (parent instanceof ScrolledComposite) {
            ScrolledComposite scrolledComposite = (ScrolledComposite) parent;
            scrolledComposite.setMinSize(content.getParent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
            content.layout();
            return;
          }
          parent = parent.getParent();
        }
      }
    };
  }

  @Override
  public boolean performOk() {
    if (isValid()) {
      return content.savePreferences();
    }
    return false;
  }

  @Override
  protected void performDefaults() {
    content.resetToDefaults();
    super.performDefaults();
  }

  @Override
  public void dispose() {
    content.dispose();
    super.dispose();
  }
}
