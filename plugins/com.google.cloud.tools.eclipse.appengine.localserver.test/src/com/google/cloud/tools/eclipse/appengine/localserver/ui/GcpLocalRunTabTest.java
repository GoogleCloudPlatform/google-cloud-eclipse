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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GcpLocalRunTabTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  @Mock private IGoogleLoginService loginService;
  @Mock private ProjectRepository projectRepository;
  @Mock private EnvironmentTab environmentTab;

  @Mock private Account account1;
  @Mock private Account account2;
  @Mock private Credential credential1;
  @Mock private Credential credential2;

  @Mock private ILaunchConfigurationWorkingCopy launchConfig;

  @Captor private ArgumentCaptor<Map<String, String>> mapCaptor; 

  private final List<GcpProject> projectsOfEmail1 = Arrays.asList(
      new GcpProject("project-A in email-1", "email-1-project-A"),
      new GcpProject("project-B in email-1", "email-1-project-B"));

  private final List<GcpProject> projectsOfEmail2 = Arrays.asList(
      new GcpProject("project-C in email-2", "email-2-project-C"),
      new GcpProject("project-D in email-2", "email-2-project-D"));

  private GcpLocalRunTab tab;
  private Shell shell;
  private AccountSelector accountSelector;
  private ProjectSelector projectSelector;
  private Text serviceKeyText;

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

    tab = new GcpLocalRunTab(environmentTab, loginService, projectRepository);
    tab.createControl(shell);

    accountSelector = CompositeUtil.findControl(shell, AccountSelector.class);
    projectSelector = CompositeUtil.findControl(shell, ProjectSelector.class);
    serviceKeyText = CompositeUtil.findControlAfterLabel(shell, Text.class, "Service key:");
    assertNotNull(accountSelector);
    assertNotNull(projectSelector);
    assertNotNull(serviceKeyText);
  }

  @After
  public void tearDown() {
    tab.dispose();
  }

  @Test
  public void testGetName() {
    assertEquals("Cloud Platform", tab.getName());
  }

  @Test
  public void testGetImage() {
    assertNotNull(tab.getImage());
  }

  @Test
  public void testGetAttribute() throws CoreException {
    when(launchConfig.getAttribute(eq("attribute-key"), anyString())).thenReturn("expected value");
    String value = GcpLocalRunTab.getAttribute(launchConfig, "attribute-key", "default");
    assertEquals("expected value", value);
  }

  @Test
  public void testGetAttribute_defaultValue() throws CoreException {
    when(launchConfig.getAttribute(anyString(), anyString()))
        .then(AdditionalAnswers.returnsLastArg());
    String value = GcpLocalRunTab.getAttribute(launchConfig, "non-existing-key", "default");
    assertEquals("default", value);
  }

  @Test
  public void testGetEnvironmentMap_defaultMap() {
    assertTrue(GcpLocalRunTab.getEnvironmentMap(launchConfig).isEmpty());
  }

  @Test
  public void testGetEnvironmentMap() throws CoreException {
    Map<String, String> map = new HashMap<>();
    when(launchConfig.getAttribute(eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES), anyMap()))
        .thenReturn(map);
    assertEquals(map, GcpLocalRunTab.getEnvironmentMap(launchConfig));
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
    mockLaunchConfig("", "", "/usr/home/keystore/my-key.json");
    tab.initializeFrom(launchConfig);
    assertEquals("/usr/home/keystore/my-key.json", serviceKeyText.getText());
  }

  @Test
  public void testActivated_initializesUi() throws CoreException {
    mockLaunchConfig("email-1@example.com", "email-1-project-A", "/usr/home/keystore/my-key.json");
    tab.activated(launchConfig);
    assertEquals("email-1@example.com", accountSelector.getSelectedEmail());
    assertEquals("email-1-project-A", projectSelector.getSelectProjectId());
    assertEquals("/usr/home/keystore/my-key.json", serviceKeyText.getText());
  }

  private void mockLaunchConfig(String accountEmail, String gcpProjectId, String serviceKey)
      throws CoreException {
    when(launchConfig.getAttribute("com.google.cloud.tools.eclipse.gcpEmulation.accountEmail", ""))
        .thenReturn(accountEmail);

    Map<String, String> environmentMap = new HashMap<>();
    environmentMap.put("GOOGLE_CLOUD_PROJECT", gcpProjectId);
    environmentMap.put("GOOGLE_APPLICATION_CREDENTIALS", serviceKey);
    when(launchConfig.getAttribute(eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES), anyMap()))
        .thenReturn(environmentMap);
  }

  @Test
  public void testPerformApply_activated() throws CoreException {
    mockLaunchConfig("email-1@example.com", "email-1-project-A", "/usr/home/key.json");
    tab.activated(launchConfig);

    accountSelector.selectAccount("email-2@example.com");
    projectSelector.selectProjectId("email-2-project-C");
    serviceKeyText.setText("/tmp/keys/another.json");

    tab.deactivated(launchConfig);

    verify(launchConfig).setAttribute("com.google.cloud.tools.eclipse.gcpEmulation.accountEmail",
        "email-2@example.com");

    verify(launchConfig).setAttribute(eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES),
        mapCaptor.capture());
    assertEquals("email-2-project-C", mapCaptor.getValue().get("GOOGLE_CLOUD_PROJECT"));
    assertEquals("/tmp/keys/another.json",
        mapCaptor.getValue().get("GOOGLE_APPLICATION_CREDENTIALS"));
  }

  @Test
  public void testPerformApply_notActivated() throws CoreException {
    mockLaunchConfig("email-1@example.com", "email-1-project-A", "/usr/home/key.json");
    tab.initializeFrom(launchConfig);

    accountSelector.selectAccount("email-2@example.com");
    projectSelector.selectProjectId("email-2-project-C");
    serviceKeyText.setText("/tmp/keys/another.json");

    tab.performApply(launchConfig);

    verify(launchConfig, never()).setAttribute(
        "com.google.cloud.tools.eclipse.gcpEmulation.accountEmail", "email-2@example.com");
    verify(launchConfig, never()).setAttribute(
        eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES), anyMap());
  }

  @Test
  public void testPerformApply_updatesEnvironmentTab() throws CoreException {
    when(launchConfig.getAttribute(anyString(), anyString()))
      .thenAnswer(AdditionalAnswers.returnsSecondArg());
    tab.activated(launchConfig);
    tab.deactivated(launchConfig);
    verify(environmentTab).initializeFrom(any(ILaunchConfiguration.class));
    verify(environmentTab).performApply(any(ILaunchConfigurationWorkingCopy.class));
  }

  @Test
  public void testIsValid_defaultValues() {
    assertTrue(tab.isValid(launchConfig));
    assertNull(tab.getErrorMessage());
  }

  @Test
  public void testIsValid_nullServiceKey() throws CoreException {
    mockLaunchConfig("email", "gcpProjectId", null /* serviceKey */);
    assertTrue(tab.isValid(launchConfig));
    assertNull(tab.getErrorMessage());
  }

  @Test
  public void testIsValid_emptyServiceKey() throws CoreException {
    mockLaunchConfig("email", "gcpProjectId", "" /* serviceKey */);
    assertTrue(tab.isValid(launchConfig));
    assertNull(tab.getErrorMessage());
  }

  @Test
  public void testIsValid_nonExistingServicekeyPath() throws CoreException {
    mockLaunchConfig("email", "gcpProjectId", "/non/existing/file.ever");
    assertFalse(tab.isValid(launchConfig));
    assertEquals("/non/existing/file.ever does not exist.", tab.getErrorMessage());
  }

  @Test
  public void testIsValid_servicekeyPathIsDirectory() throws CoreException {
    mockLaunchConfig("email", "gcpProjectId", "/");
    assertFalse(tab.isValid(launchConfig));
    assertEquals("/ is a directory.", tab.getErrorMessage());
  }
}
