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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.AppEngineDeployPreferencesPanel;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.internal.DeployArtifactValidator;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FlexExistingArtifactDeployPreferencesPanelTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private Runnable layoutHandler;

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  @Before
  public void setup() {
    TestAccountProvider.setAsDefaultProvider();
  }
  
  @Test
  public void testDeployArtifactPathField() {
    AppEngineDeployPreferencesPanel panel = createPanel();

    Text deployArtifactField = findDeployArtifactField(panel);
    assertNotNull(deployArtifactField);
    IStatus status = getDeployArtifactPathValidationStatus(panel);
    assertEquals("Missing WAR or JAR path.", status.getMessage());
    assertFalse(status.isOK());
  }

  @Test
  public void testDeployArtifactPathValidation_nonExistingFile() {
    AppEngineDeployPreferencesPanel panel = createPanel();

    Text deployArtifactField = findDeployArtifactField(panel);
    deployArtifactField.setText("non/existing/file.war");
    IStatus status = getDeployArtifactPathValidationStatus(panel);
    assertThat(status.getMessage(), startsWith("File does not exist: "));
    assertFalse(status.isOK());
  }

  @Test
  public void testDeployArtifactPathValidation_notWarOrJar() {
    AppEngineDeployPreferencesPanel panel = createPanel();

    Text deployArtifactField = findDeployArtifactField(panel);
    deployArtifactField.setText("script.sh");
    IStatus status = getDeployArtifactPathValidationStatus(panel);
    assertEquals("File extension is not \"war\" or \"jar\".", status.getMessage());
    assertFalse(status.isOK());
  }

  private AppEngineDeployPreferencesPanel createPanel() {
    return new FlexExistingArtifactDeployPreferencesPanel(shellResource.getShell(),
        layoutHandler, true /* requireValues */, projectRepository);
  }

  private static IStatus getDeployArtifactPathValidationStatus(
      AppEngineDeployPreferencesPanel panel) {
    DataBindingContext context = panel.getDataBindingContext();
    for (Object provider : context.getValidationStatusProviders()) {
      if (provider instanceof DeployArtifactValidator) {
        IObservableValue value = ((DeployArtifactValidator) provider).getValidationStatus();
        return (IStatus) value.getValue();
      }
    }
    return null;
  }

  private static Text findDeployArtifactField(Composite panel) {
    Control control = CompositeUtil.findControlAfterLabel(panel, Text.class, "WAR/JAR:");
    assertNotNull("Could not locate WAR/JAR field", control);
    return (Text) control;
  }
}
