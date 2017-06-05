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

import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineWizardPage;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import com.google.cloud.tools.eclipse.appengine.newproject.PageValidator;
import com.google.cloud.tools.eclipse.appengine.newproject.maven.MavenCoordinatesUi;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class AppEngineFlexWizardPage extends AppEngineWizardPage {

  // TODO: push this down to the super class when we implement a unified wizard:
  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1326
  private MavenCoordinatesUi mavenCoordinatesUi;

  public AppEngineFlexWizardPage() {
    super(false);
    setTitle(Messages.getString("app.engine.flex.project")); //$NON-NLS-1$
    setDescription(Messages.getString("create.app.engine.flex.project")); //$NON-NLS-1$
  }

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);

    Composite composite = (Composite) getControl();
    mavenCoordinatesUi = new MavenCoordinatesUi(composite, true /* dynamic enabling */);
    mavenCoordinatesUi.addChangeListener(new PageValidator(this));
    GridLayoutFactory.swtDefaults().generateLayout(composite);
  }

  @Override
  public boolean validatePage() {
    if (!super.validatePage()) {
      return false;
    }
    return mavenCoordinatesUi.setValidationMessage(this);
  }

  @Override
  public void setHelp(Composite container) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
        "com.google.cloud.tools.eclipse.appengine.newproject.NewFlexProjectContext"); //$NON-NLS-1$
  }

  // TODO: push these methods down to the super class when we implement a unified wizard:
  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1326
  public boolean asMavenProject() {
    return mavenCoordinatesUi.uiEnabled();
  }

  public String getMavenGroupId() {
    return mavenCoordinatesUi.getGroupId();
  }

  public String getMavenArtifactId() {
    return mavenCoordinatesUi.getArtifactId();
  }

  public String getMavenVersion() {
    return mavenCoordinatesUi.getVersion();
  }

}
