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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.deploy.ui.internal.AppYamlValidator;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.common.base.Predicate;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
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
public class FlexDeployPreferencesPanelTest {

  private static final String APP_YAML_FIELD_TOOLTIP =
      "app.yaml path, either absolute or relative to the project.";

  @Mock private IGoogleLoginService loginService;
  @Mock private ProjectRepository projectRepository;
  @Mock private Runnable layoutHandler;

  @Rule public ShellTestResource shellResource = new ShellTestResource();
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator().withFacetVersions(
      JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31, AppEngineFlexFacet.FACET_VERSION);

  private IProject project;

  @Before
  public void setUp() {
    project = projectCreator.getProject();
  }

  @Test
  public void testGetHelpContextId() {
    FlexDeployPreferencesPanel panel = createPanel(true /* requireValues */);

    assertEquals(
        "com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployAppEngineFlexProjectContext",
        panel.getHelpContextId());
  }

  @Test
  public void testDefaultAppYamlPathSet() {
    FlexDeployPreferencesPanel panel = createPanel(true /* requireValues */);

    Text appYamlField = findAppYamlField(panel);
    assertEquals("src/main/appengine/app.yaml", appYamlField.getText());
    assertTrue(getAppYamlPathValidationStatus(panel).isOK());
  }

  @Test
  public void testAppYamlPathValidation_nonExistingAppYaml() {
    FlexDeployPreferencesPanel panel = createPanel(true /* requireValues */);

    Text appYamlField = findAppYamlField(panel);
    appYamlField.setText("non/existing/app.yaml");
    assertFalse(getAppYamlPathValidationStatus(panel).isOK());
  }

  @Test
  public void testAppYamlPathValidation_noValidationIfRequireValuesIsFalse() {
    FlexDeployPreferencesPanel panel = createPanel(false /* requireValues */);

    Text appYamlField = findAppYamlField(panel);
    appYamlField.setText("non/existing/app.yaml");
    assertNull(getAppYamlPathValidationStatus(panel));
  }

  @Test
  public void testAppYamlPathValidation_absolutePathWorks() {
    FlexDeployPreferencesPanel panel = createPanel(true /* requireValues */);
    Text appYamlField = findAppYamlField(panel);

    IPath absolutePath = project.getLocation().append("src/main/appengine/app.yaml");
    assertTrue(absolutePath.isAbsolute());

    appYamlField.setText(absolutePath.toString());
    assertTrue(getAppYamlPathValidationStatus(panel).isOK());
  }

  private FlexDeployPreferencesPanel createPanel(boolean requireValues) {
    return new FlexDeployPreferencesPanel(shellResource.getShell(),
        project, loginService, layoutHandler, requireValues, projectRepository);
  }

  private static IStatus getAppYamlPathValidationStatus(FlexDeployPreferencesPanel panel) {
    DataBindingContext context = panel.getDataBindingContext();
    for (Object provider : context.getValidationStatusProviders()) {
      if (provider instanceof AppYamlValidator) {
        IObservableValue value = ((AppYamlValidator) provider).getValidationStatus();
        return (IStatus) value.getValue();
      }
    }
    return null;
  }

  private static Text findAppYamlField(Composite panel) {
    return (Text) CompositeUtil.findControl(panel, new Predicate<Control>() {
      @Override
      public boolean apply(Control control) {
        return control instanceof Text && APP_YAML_FIELD_TOOLTIP.equals(control.getToolTipText());
      }
    });
  }
}
