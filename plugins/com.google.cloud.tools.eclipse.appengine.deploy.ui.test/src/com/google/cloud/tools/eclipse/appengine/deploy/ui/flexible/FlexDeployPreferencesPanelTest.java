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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.cloud.tools.login.Account;
import com.google.common.base.Predicate;
import java.util.Arrays;
import java.util.HashSet;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ValidationStatusProvider;
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
import org.osgi.service.prefs.BackingStoreException;

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
  public void setUp() throws ProjectRepositoryException, BackingStoreException {
    Credential credential = mock(Credential.class);
    Account account = mock(Account.class);
    when(account.getEmail()).thenReturn("user@example.com");
    when(account.getOAuth2Credential()).thenReturn(credential);
    when(loginService.getAccounts()).thenReturn(new HashSet<>(Arrays.asList(account)));

    GcpProject gcpProject = new GcpProject("name", "gcp-project-id");
    when(projectRepository.getProjects(credential)).thenReturn(Arrays.asList(gcpProject));
    when(projectRepository.getProject(credential, "gcp-project-id")).thenReturn(gcpProject);

    // Make the panel auto-select a project to suppress the project validation error.
    project = projectCreator.getProject();
    DeployPreferences preferences = new DeployPreferences(project);
    preferences.setProjectId("gcp-project-id");
    preferences.save();
  }

  @Test
  public void testGetHelpContextId() throws InterruptedException {
    FlexDeployPreferencesPanel panel = createPanel(true /* requireValues */);

    assertEquals(
        "com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployAppEngineFlexProjectContext",
        panel.getHelpContextId());
  }

  @Test
  public void testDefaultAppYamlPathSet() throws InterruptedException {
    FlexDeployPreferencesPanel panel = createPanel(true /* requireValues */);

    Text appYamlField = findAppYamlField(panel);
    assertEquals("src/main/appengine/app.yaml", appYamlField.getText());
    assertFalse(hasValidationError(panel));
  }

  @Test
  public void testAppYamlPathValidation() throws InterruptedException {
    FlexDeployPreferencesPanel panel = createPanel(true /* requireValues */);

    Text appYamlField = findAppYamlField(panel);
    appYamlField.setText("non/existing/app.yaml");
    assertTrue(hasValidationError(panel));
  }

  @Test
  public void testAppYamlPathValidation_noValidationIfRequireValuesIsFalse()
      throws InterruptedException {
    FlexDeployPreferencesPanel panel = createPanel(false /* requireValues */);

    Text appYamlField = findAppYamlField(panel);
    appYamlField.setText("non/existing/app.yaml");
    assertFalse(hasValidationError(panel));
  }

  @Test
  public void testAppYamlPathValidation_absolutePathWorks() throws InterruptedException {
    FlexDeployPreferencesPanel panel = createPanel(false /* requireValues */);
    Text appYamlField = findAppYamlField(panel);

    IPath absolutePath = project.getLocation().append("src/main/appengine/app.yaml");
    assertTrue(absolutePath.isAbsolute());

    appYamlField.setText(absolutePath.toString());
    assertFalse(hasValidationError(panel));
  }

  private FlexDeployPreferencesPanel createPanel(boolean requireValues)
      throws InterruptedException {
    FlexDeployPreferencesPanel panel = new FlexDeployPreferencesPanel(shellResource.getShell(),
        project, loginService, layoutHandler, requireValues, projectRepository);
    findAccountSelector(panel).selectAccount("user@example.com");
    panel.latestGcpProjectQueryJob.join();
    return panel;
  }

  private static boolean hasValidationError(FlexDeployPreferencesPanel panel) {
    DataBindingContext context = panel.getDataBindingContext();
    for (Object provider : context.getValidationStatusProviders()) {
      IObservableValue value = ((ValidationStatusProvider) provider).getValidationStatus();
      IStatus status = (IStatus) value.getValue();
      if (!status.isOK()) {
        return true;
      }
    }
    return false;
  }

  private static Text findAppYamlField(Composite panel) {
    return (Text) CompositeUtil.findControl(panel, new Predicate<Control>() {
      @Override
      public boolean apply(Control control) {
        return control instanceof Text && APP_YAML_FIELD_TOOLTIP.equals(control.getToolTipText());
      }
    });
  }

  private static AccountSelector findAccountSelector(Composite panel) {
    return CompositeUtil.findControl(panel, AccountSelector.class);
  }
}
