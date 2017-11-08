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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.cloud.tools.login.Account;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GcpEmulationTabTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  @Mock private IGoogleLoginService loginService;
  @Mock private ProjectRepository projectRepository;

  @Mock private Account account1;
  @Mock private Account account2;
  @Mock private Credential credential1;
  @Mock private Credential credential2;

  @Mock private ILaunchConfigurationWorkingCopy launchConfig;

  private final List<GcpProject> projectsOfEmail1 = Arrays.asList(
      new GcpProject("project-A in email-1", "email-1-project-A"),
      new GcpProject("project-B in email-1", "email-1-project-B"));

  private final List<GcpProject> projectsOfEmail2 = Arrays.asList(
      new GcpProject("project-C in email-2", "email-2-project-C"),
      new GcpProject("project-D in email-2", "email-2-project-D"));

  private GcpEmulationTab tab;
  private Shell shell;
  private AccountSelector accountSelector;
  private ProjectSelector projectSelector;

  @Before
  public void setUp() throws ProjectRepositoryException {
    shell = shellResource.getShell();

    when(account1.getEmail()).thenReturn("email-1@example.com");
    when(account2.getEmail()).thenReturn("email-2@example.com");
    when(account1.getOAuth2Credential()).thenReturn(credential1);
    when(account2.getOAuth2Credential()).thenReturn(credential2);
    when(loginService.getAccounts()).thenReturn(new HashSet<>(Arrays.asList(account1, account2)));

    when(projectRepository.getProjects(credential1)).thenReturn(projectsOfEmail1);
    when(projectRepository.getProjects(credential2)).thenReturn(projectsOfEmail2);

    tab = new GcpEmulationTab(loginService, projectRepository);
    tab.createControl(shell);

    accountSelector = CompositeUtil.findControl(shell, AccountSelector.class);
    projectSelector = CompositeUtil.findControl(shell, ProjectSelector.class);
  }

  @Test
  public void testUiComponents() {
    assertNotNull(accountSelector);
    assertNotNull(projectSelector);
    assertNotNull(CompositeUtil.findControlAfterLabel(shell, Text.class, "Service key:"));
  }

  @Test
  public void testGetAttribute() throws CoreException {
    when(launchConfig.getAttribute(eq("attribute-key"), anyString())).thenReturn("expected value");
    String value = GcpEmulationTab.getAttribute(launchConfig, "attribute-key", "defualt");
    assertEquals("expected value", value);
  }

  @Test
  public void testGetAttribute_defaultValue() throws CoreException {
    when(launchConfig.getAttribute(anyString(), anyString()))
        .then(AdditionalAnswers.returnsLastArg());
    String value = GcpEmulationTab.getAttribute(launchConfig, "non-existing-key", "default");
    assertEquals("default", value);
  }

  @Test
  public void testAccountSelectorLoaded() {
    assertEquals(2, accountSelector.getAccountCount());
    assertEquals("", accountSelector.getSelectedEmail());
  }
  
  @Test
  public void testProjectSelectorLoaded() {
    accountSelector.selectAccount("email-1@example.com");
    assertEquals(projectsOfEmail1, projectSelector.getProjects());
    assertEquals("", projectSelector.getSelectProjectId());
  }
  
  @Test
  public void testProjectSelectorLoaded_switchingAccounts() {
    accountSelector.selectAccount("email-1@example.com");
    accountSelector.selectAccount("email-2@example.com");
    assertEquals(projectsOfEmail2, projectSelector.getProjects());
    assertEquals("", projectSelector.getSelectProjectId());
  }
  
  @Test
  public void testInitializeFrom_accountSelected() throws CoreException {
    mockLaunchConfig("email-1@example.com", "", "");
    tab.initializeFrom(launchConfig);
    assertEquals("email-1@example.com", accountSelector.getSelectedEmail());

    mockLaunchConfig("email-2@example.com", "", "");
    tab.initializeFrom(launchConfig);
    assertEquals("email-2@example.com", accountSelector.getSelectedEmail());
  }

  @Test
  public void testInitializeFrom_projectSelected() throws CoreException {
    mockLaunchConfig("email-1@example.com", "email-1-project-A", "");
    tab.initializeFrom(launchConfig);
    assertEquals("email-1-project-A", projectSelector.getSelectProjectId());

    mockLaunchConfig("email-2@example.com", "email-2-project-D", "");
    tab.initializeFrom(launchConfig);
    assertEquals("email-2-project-D", projectSelector.getSelectProjectId());
  }

  @Test
  public void testInitializeFrom_serviceKeyEntered() throws CoreException {
    Text serviceKey = CompositeUtil.findControlAfterLabel(shell, Text.class, "Service key:");

    mockLaunchConfig("", "", "/usr/home/keystore/my-key.json");
    tab.initializeFrom(launchConfig);
    assertEquals("/usr/home/keystore/my-key.json", serviceKey.getText());
  }

  private void mockLaunchConfig(String accountEmail, String gcpProjectId, String serviceKey)
      throws CoreException {
    when(launchConfig.getAttribute("com.google.cloud.tools.eclipse.gcpEmulation.accountEmail", ""))
        .thenReturn(accountEmail);
    when(launchConfig.getAttribute("com.google.cloud.tools.eclipse.gcpEmulation.gcpProject", ""))
        .thenReturn(gcpProjectId);
    when(launchConfig.getAttribute("com.google.cloud.tools.eclipse.gcpEmulation.serviceKey", ""))
        .thenReturn(serviceKey);
  }

  @Test
  public void testPerformApply() throws CoreException {
    mockLaunchConfig("email-1@example.com", "email-1-project-A", "/usr/home/key.json");
    tab.initializeFrom(launchConfig);

    accountSelector.selectAccount("email-2@example.com");
    projectSelector.selectProjectId("email-2-project-C");
    Text serviceKeyText = CompositeUtil.findControlAfterLabel(shell, Text.class, "Service key:");
    serviceKeyText.setText("/tmp/keys/another.json");

    tab.performApply(launchConfig);

    verify(launchConfig).setAttribute("com.google.cloud.tools.eclipse.gcpEmulation.accountEmail",
        "email-2@example.com");
    verify(launchConfig).setAttribute("com.google.cloud.tools.eclipse.gcpEmulation.gcpProject",
        "email-2-project-C");
    verify(launchConfig).setAttribute("com.google.cloud.tools.eclipse.gcpEmulation.serviceKey",
        "/tmp/keys/another.json");
  }
}
