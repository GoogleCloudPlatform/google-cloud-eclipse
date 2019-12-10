/*
 * Copyright 2019 Google Inc.
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

import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.eclipse.appengine.libraries.ui.CloudLibrariesSelectionPage;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineProjectWizardTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  @Test
  public void testLibrariesSelectionPage_appEngineLibrariesGroup() {
    verifySupportedLibrariesGroupLabel("appengine", "App Engine Standard Libraries");
  }

  @Test
  public void testLibrariesSelectionPage_nonAppEngineLibrariesGroup() {
    verifySupportedLibrariesGroupLabel("some libraries group", "Other Libraries");
  }

  private void verifySupportedLibrariesGroupLabel(
      String supportedLibrariesGroup, String expectedLabel) {
    AppEngineWizardPage wizardPage = new AppEngineWizardPage() {
      @Override
      public void setHelp(Composite container) {}

      @Override
      protected String getSupportedLibrariesGroup() {
        return supportedLibrariesGroup;
      }
    };

    AppEngineProjectWizard wizard = new AppEngineProjectWizard(wizardPage) {
      @Override
      public CreateAppEngineWtpProject getAppEngineProjectCreationOperation(
          AppEngineProjectConfig config, IAdaptable uiInfoAdapter) {
        return null;
      }
    };

    CloudLibrariesSelectionPage librariesPage =
        (CloudLibrariesSelectionPage) wizard.getPage("cloudPlatformLibrariesPage");
    librariesPage.createControl(shellResource.getShell());

    Group group = CompositeUtil.findControl((Composite) librariesPage.getControl(), Group.class);
    assertEquals(expectedLabel, group.getText());
  }
}
