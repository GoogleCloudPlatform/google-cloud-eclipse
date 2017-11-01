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

package com.google.cloud.tools.eclipse.appengine.newproject.flex;

import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig.Template;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineWizardPage;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import com.google.cloud.tools.eclipse.appengine.newproject.maven.MavenCoordinatesInput;
import com.google.cloud.tools.eclipse.appengine.newproject.maven.MavenCoordinatesUi;
import com.google.common.base.Preconditions;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

public class AppEngineFlexJarWizardPage extends AppEngineWizardPage {

  private Combo combo;

  AppEngineFlexJarWizardPage() {
    super(false);
    setTitle(Messages.getString("app.engine.flex.project")); //$NON-NLS-1$
    setDescription(Messages.getString("create.app.engine.flex.project")); //$NON-NLS-1$
  }

  @Override
  protected void setHelp(Composite container) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
        "com.google.cloud.tools.eclipse.appengine.newproject.NewFlexProjectContext"); //$NON-NLS-1$
  }

  @Override
  protected MavenCoordinatesInput createMavenCoordinatesInput(Composite container) {
    MavenCoordinatesUi ui = new MavenCoordinatesUi(container, SWT.NONE);
    ui.setText(Messages.getString("MAVEN_PROJECT_COORDINATES"));
    return ui;
  }

  @Override
  protected void createCustomFields(Composite container) {
    super.createCustomFields(container);

    Label label = new Label(container, SWT.LEAD);
    label.setText(Messages.getString("FLEX_JAR_SAMPLE_TEMPLATE")); //$NON-NLS-1$
    combo = new Combo(container, SWT.READ_ONLY);
    combo.add(Messages.getString("FLEX_JAR_NO_WEB_FRAMEWORK_TEMPLATE"));
    combo.setData(Messages.getString("FLEX_JAR_NO_WEB_FRAMEWORK_TEMPLATE"), Template.DEFAULT);
    combo.add(Messages.getString("FLEX_SPRING_BOOT_TEMPLATE"));
    combo.setData(Messages.getString("FLEX_SPRING_BOOT_TEMPLATE"), Template.SPRING_BOOT);
    combo.select(0);
  }

  @Override
  protected Template getTemplate() {
    Preconditions.checkState(combo.getSelectionIndex() != -1);
    return (Template) combo.getData(combo.getText());
  }
}
