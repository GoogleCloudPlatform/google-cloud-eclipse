/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;

/**
 * AppEngineComponentPage is a page that displays a message that Gcloud App Engine Java component is missing
 * with instructions on how to install it. This page disables the 'Finish' button.
 */
public class AppEngineComponentPage extends WizardPage {
  private boolean forNativeProjectWizard;

  public AppEngineComponentPage(boolean forNativeProjectWizard) {
    super("appEngineComponentPage");
    setTitle("App Engine Component is missing");
    setDescription("The Cloud SDK App Engine Java component is not installed"); 
    this.forNativeProjectWizard = forNativeProjectWizard;
  }

  @Override
  public void createControl(Composite parent) {
    if (forNativeProjectWizard) {
      AnalyticsPingManager.getInstance().sendPing(
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD,
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE,
          parent.getShell());
    } else {
      AnalyticsPingManager.getInstance().sendPing(
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD,
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_MAVEN,
          parent.getShell());
    }

    Composite container = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);

    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = parent.getSize().x;

    String message = Messages.AppEngineJavaComponentMissing;
    StyledText styledText = new StyledText(container, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
    styledText.setLayoutData(gridData);
    styledText.setText(message);
    styledText.setBackground(container.getBackground());
    styledText.setCaret(null /* hide caret */);

    int startIndex = message.indexOf("\'");
    int endIndex = message.indexOf("\'", startIndex + 1);
    if ((-1 < startIndex) && (startIndex < endIndex)) {
      StyleRange styleRange = new StyleRange();
      styleRange.start = startIndex + 1;
      styleRange.length = endIndex - startIndex;
      styleRange.fontStyle = SWT.BOLD;
      styledText.setStyleRange(styleRange);
    }

    setControl(container);
    setPageComplete(false);
    Dialog.applyDialogFont(container);
  }
  
}
