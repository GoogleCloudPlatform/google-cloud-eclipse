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

package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class PageValidator implements ModifyListener, Listener {

  private final AppEngineWizardPage wizardPage;

  public PageValidator(AppEngineWizardPage wizardPage) {
    this.wizardPage = wizardPage;
  }

  @Override
  public void modifyText(ModifyEvent event) {
    wizardPage.setPageComplete(wizardPage.validatePage());
  }

  @Override
  public void handleEvent(Event event) {
    wizardPage.setPageComplete(wizardPage.validatePage());
  }
}
