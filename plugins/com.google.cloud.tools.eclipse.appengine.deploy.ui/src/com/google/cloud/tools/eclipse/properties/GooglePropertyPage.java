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

package com.google.cloud.tools.eclipse.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;

// To be moved to a more general bundle
// https://github.com/GoogleCloudPlatform/gcloud-eclipse-tools/issues/507
public class GooglePropertyPage extends PropertyPage {

  @Override
  protected Control createContents(Composite parent) {
    Label label = new Label(parent, SWT.LEFT);
    label.setText(Messages.getString("google.preferences")); //$NON-NLS-1$
    return label;
  }

}
