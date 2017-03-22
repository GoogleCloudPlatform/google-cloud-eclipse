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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexDeployPreferences;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FlexDeployPreferencesPanelTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();
  @Mock private IProject project;
  @Mock private FlexDeployPreferences preferences;

  @Test
  public void testGetHelpContextId() throws Exception {
    when(preferences.getUseDeploymentPreferences()).thenReturn(false);
    when(preferences.getDockerDirectory()).thenReturn("/non/existent/docker/directory");
    Shell parent = shellTestResource.getShell();
    assertThat(new FlexDeployPreferencesPanel(parent, project, preferences).getHelpContextId(),
        is("com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployAppEngineFlexProjectContext"));
  }
}
