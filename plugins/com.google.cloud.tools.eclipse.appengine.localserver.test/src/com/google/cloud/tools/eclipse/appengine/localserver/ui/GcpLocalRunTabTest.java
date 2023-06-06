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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.Base64;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.Iam.Projects;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.Keys;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.Keys.Create;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.ServiceAccountKey;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GcpLocalRunTabTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private GoogleApiFactory apiFactory;
  @Mock private ProjectRepository projectRepository;
  @Mock private EnvironmentTab environmentTab;

  private Account account1 = TestAccountProvider.ACCOUNT_1;
  private Account account2 = TestAccountProvider.ACCOUNT_2;
  @Mock private Credential credential1;
  @Mock private Credential credential2;

  @Mock private ILaunchConfigurationWorkingCopy launchConfig;

  @Captor private ArgumentCaptor<Map<String, String>> mapCaptor; 

  private final List<GcpProject> projectsOfEmail1 = Arrays.asList(
      new GcpProject("project-A in email-1", "project-A"),
      new GcpProject("project-B in email-1", "project-B"));

  private final List<GcpProject> projectsOfEmail2 = Arrays.asList(
      new GcpProject("project-C in email-2", "project-C"),
      new GcpProject("project-D in email-2", "google.com:project-D"));

  private GcpLocalRunTab tab;
  private Shell shell;
  private AccountSelector accountSelector;
  private ProjectSelector projectSelector;
  private Text serviceKeyText;
  private Path keyFile;

  @Before
  public void setUp() {
    GoogleApiFactory.setInstance(apiFactory);
    shell = shellResource.getShell();
    selectAccount(null);
    tab = new GcpLocalRunTab(environmentTab, projectRepository);
    tab.createControl(shell);
    accountSelector = CompositeUtil.findControl(shell, AccountSelector.class);
    selectAccount(account1);

    projectSelector = CompositeUtil.findControl(shell, ProjectSelector.class);
    serviceKeyText = CompositeUtil.findControlAfterLabel(shell, Text.class, "Service key:");
    assertNotNull(accountSelector);
    assertNotNull(projectSelector);
    assertNotNull(serviceKeyText);

    keyFile = tempFolder.getRoot().toPath().resolve("key.json");
  }

  private void selectAccount(Account account) {
    try {
      if (account == null) {
        when(apiFactory.getAccount()).thenReturn(Optional.empty());
        when(apiFactory.getCredential()).thenReturn(Optional.empty());
      } else {
        when(apiFactory.getAccount()).thenReturn(Optional.of(account));
        when(apiFactory.getCredential()).thenReturn(Optional.of(account.getOAuth2Credential()));
        if (account.equals(TestAccountProvider.ACCOUNT_1)) {
          when(projectRepository.getProjects()).thenReturn(projectsOfEmail1);
        } else if (account.equals(TestAccountProvider.ACCOUNT_2)) {
          when(projectRepository.getProjects()).thenReturn(projectsOfEmail2);
        } else {
          throw new IllegalArgumentException("Used a test account not belonging to TestAccountProvider");
        }
        accountSelector.forceAccountCheck();
      }
    } catch (ProjectRepositoryException ex) {
      fail();
    }
  }
  
  @After
  public void tearDown() {
    tab.dispose();
    GoogleApiFactory.resetInstance();
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
    when(launchConfig.getAttribute(eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES),
        Matchers.anyMapOf(String.class, String.class)))
        .thenReturn(map);
    assertEquals(map, GcpLocalRunTab.getEnvironmentMap(launchConfig));
  }

  @Test
  public void testAccountSelectorLoaded() {
    selectAccount(null);
    assertEquals(0, accountSelector.getAccountCount());
    assertEquals("", accountSelector.getSelectedEmail());
  }

  @Test
  public void testProjectSelectorLoaded() {
    selectAccount(account1);
    assertEquals(projectsOfEmail1, projectSelector.getProjects());
    assertEquals("", projectSelector.getSelectedProjectId());
  }

  @Test
  public void testProjectSelectorLoaded_switchingAccounts() {
    selectAccount(account1);
    selectAccount(account2);
    assertEquals(projectsOfEmail2, projectSelector.getProjects());
    assertEquals("", projectSelector.getSelectedProjectId());
  }

  @Test
  public void testInitializeFrom_projectSelected() throws CoreException {
    selectAccount(account1);
    mockLaunchConfig(Optional.of(account1), "project-A", "");
    tab.initializeFrom(launchConfig);
    assertEquals("project-A", projectSelector.getSelectedProjectId());

    mockLaunchConfig(Optional.of(account1), "project-B", "");
    tab.initializeFrom(launchConfig);
    assertEquals("project-B", projectSelector.getSelectedProjectId());
  }

  @Test
  public void testInitializeFrom_serviceKeyEntered() throws CoreException {
    selectAccount(account1);
    mockLaunchConfig(Optional.empty(), "", "/usr/home/keystore/my-key.json");
    tab.initializeFrom(launchConfig);
    assertEquals("/usr/home/keystore/my-key.json", serviceKeyText.getText());
  }

  @Test
  public void testActivated_initializesUi() throws CoreException {
    selectAccount(account1);
    mockLaunchConfig(Optional.of(account1), "project-A", "/usr/home/keystore/my-key.json");
    tab.activated(launchConfig);
    assertEquals(account1.getEmail(), accountSelector.getSelectedEmail());
    assertEquals("project-A", projectSelector.getSelectedProjectId());
    assertEquals("/usr/home/keystore/my-key.json", serviceKeyText.getText());
  }

  @Test
  public void testPerformApply_activated() throws CoreException {
    mockLaunchConfig(Optional.of(account1), "project-A", "/usr/home/key.json");
    tab.activated(launchConfig);

    selectAccount(account2);
    projectSelector.selectProjectId("project-C");
    serviceKeyText.setText("/tmp/keys/another.json");

    tab.deactivated(launchConfig);

    verify(launchConfig).setAttribute("com.google.cloud.tools.eclipse.gcpEmulation.accountEmail",
        account2.getEmail());

    verify(launchConfig).setAttribute(eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES),
        mapCaptor.capture());
    assertEquals("project-C", mapCaptor.getValue().get("GOOGLE_CLOUD_PROJECT"));
    assertEquals("/tmp/keys/another.json",
        mapCaptor.getValue().get("GOOGLE_APPLICATION_CREDENTIALS"));
  }

  @Test
  public void testPerformApply_notActivated() throws CoreException {
    mockLaunchConfig(Optional.of(account1), "project-A", "/usr/home/key.json");
    tab.initializeFrom(launchConfig);

    selectAccount(account2);
    projectSelector.selectProjectId("project-C");
    serviceKeyText.setText("/tmp/keys/another.json");

    tab.performApply(launchConfig);

    verify(launchConfig, never()).setAttribute(
        "com.google.cloud.tools.eclipse.gcpEmulation.accountEmail", account2.getEmail());
    verify(launchConfig, never()).setAttribute(
        eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES),
        Matchers.anyMapOf(String.class, String.class));
  }

  @Test
  public void testPerformApply_updatesEnvironmentTab() throws CoreException {
    when(launchConfig.getAttribute(anyString(), anyString()))
      .thenAnswer(AdditionalAnswers.returnsSecondArg());
    selectAccount(account1);
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
    mockLaunchConfig(Optional.of(account1), "gcpProjectId", null /* serviceKey */);
    assertTrue(tab.isValid(launchConfig));
    assertNull(tab.getErrorMessage());
  }

  @Test
  public void testIsValid_emptyServiceKey() throws CoreException {
    mockLaunchConfig(Optional.of(account1), "gcpProjectId", "" /* serviceKey */);
    assertTrue(tab.isValid(launchConfig));
    assertNull(tab.getErrorMessage());
  }

  @Test
  public void testIsValid_nonExistingServicekeyPath() throws CoreException {
    mockLaunchConfig(Optional.of(account1), "gcpProjectId", "/non/existing/file.ever");
    assertFalse(tab.isValid(launchConfig));
    assertEquals("/non/existing/file.ever does not exist.", tab.getErrorMessage());
  }

  @Test
  public void testIsValid_servicekeyPathIsDirectory() throws CoreException {
    mockLaunchConfig(Optional.of(account1), "gcpProjectId", "/");
    assertFalse(tab.isValid(launchConfig));
    assertEquals("/ is a directory.", tab.getErrorMessage());
  }

  @Test
  public void testCreateKeyButtonEnablement() {
    selectAccount(account1);
    Button createKeyButton = CompositeUtil.findButton(shell, "Create New Key");
    tab.initializeFrom(launchConfig);

    assertTrue(projectSelector.getSelection().isEmpty());
    assertFalse(createKeyButton.isEnabled());

    selectAccount(account1);
    projectSelector.selectProjectId("project-A");
    assertTrue(createKeyButton.isEnabled());

    projectSelector.setSelection(new StructuredSelection());
    assertFalse(createKeyButton.isEnabled());
  }

  @Test
  public void testCreateServiceAccountKey() throws IOException, CoreException {
    selectAccount(account1);
    setUpServiceKeyCreation(apiFactory, false);
    mockLaunchConfig(Optional.of(account2), "google.com:project-D", "");
    selectAccount(account2);

    tab.initializeFrom(launchConfig);

    tab.createServiceAccountKey(keyFile);

    byte[] bytesRead = Files.readAllBytes(keyFile);
    assertEquals("key data in JSON format", new String(bytesRead, StandardCharsets.UTF_8));
  }

  
  
  @Test
  public void testCreateServiceAccountKey_replacesExistingKey() throws IOException, CoreException {
    selectAccount(account1);
    setUpServiceKeyCreation(false);

    Files.write(keyFile, new byte[] {0, 1, 2});
    tab.createServiceAccountKey(keyFile);

    byte[] bytesRead = Files.readAllBytes(keyFile);
    assertEquals("key data in JSON format", new String(bytesRead, StandardCharsets.UTF_8));
  }

  @Test
  public void testCreateServiceAccountKey_uiResult() throws CoreException, IOException {
    selectAccount(account1);
    setUpServiceKeyCreation(false);

    tab.createServiceAccountKey(keyFile);

    assertEquals(keyFile.toString(), serviceKeyText.getText());
    assertEquals("Created a service account key for the App Engine default service account:\n"
        + keyFile, tab.serviceKeyDecoration.getDescriptionText());
  }

  @Test
  public void testCreateServiceAccountKey_ioException() throws CoreException, IOException {
    selectAccount(account1);
    setUpServiceKeyCreation(true);

    tab.createServiceAccountKey(keyFile);

    assertFalse(Files.exists(keyFile));
    assertThat(tab.serviceKeyDecoration.getDescriptionText(),
        startsWith("Could not create a service account key:"));
    assertThat(tab.serviceKeyDecoration.getDescriptionText(), containsString("log from unit test"));
  }

  @Test
  public void testGetServiceAccountKeyPath() throws URISyntaxException {
    tab.initializeFrom(launchConfig);
    selectAccount(account1);
    projectSelector.selectProjectId("project-A");

    Path expected = Paths.get(Platform.getConfigurationLocation().getURL().toURI())
        .resolve("com.google.cloud.tools.eclipse")
        .resolve("app-engine-default-service-account-key-project-A.json");
    assertEquals(expected, tab.getServiceAccountKeyPath());
  }
  
  @Test
  public void testGetServiceAccountKeyPath_internal() throws URISyntaxException {
    tab.initializeFrom(launchConfig);
    selectAccount(account2);
    projectSelector.selectProjectId("google.com:project-D");

    Path expected = Paths.get(Platform.getConfigurationLocation().getURL().toURI())
        .resolve("com.google.cloud.tools.eclipse")
        .resolve("app-engine-default-service-account-key-google.com.project-D.json");
    assertEquals(expected, tab.getServiceAccountKeyPath());
  }
  
  private void mockLaunchConfig(Optional<Account> account, String gcpProjectId, String serviceKey)
      throws CoreException {
    
    if (account.isPresent()) {
      when(launchConfig.getAttribute("com.google.cloud.tools.eclipse.gcpEmulation.accountEmail", ""))
          .thenReturn(account.get().getEmail());
      selectAccount(account.get());
    }

    Map<String, String> environmentMap = new HashMap<>();
    environmentMap.put("GOOGLE_CLOUD_PROJECT", gcpProjectId);
    environmentMap.put("GOOGLE_APPLICATION_CREDENTIALS", serviceKey);
    when(launchConfig.getAttribute(eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES), 
        Matchers.anyMapOf(String.class, String.class)))
        .thenReturn(environmentMap);
  }

  private void setUpServiceKeyCreation(boolean throwException) throws CoreException, IOException {
    setUpServiceKeyCreation(apiFactory, throwException);
    mockLaunchConfig(Optional.of(account1), "project-A", "");
    tab.initializeFrom(launchConfig);
  }
  
  private static void setUpServiceKeyCreation(
      IGoogleApiFactory mockApiFactory, boolean throwException) throws IOException {
    Iam iam = Mockito.mock(Iam.class);
    Projects projects = Mockito.mock(Projects.class);
    ServiceAccounts serviceAccounts = Mockito.mock(ServiceAccounts.class);
    Keys keys = Mockito.mock(Keys.class);
    Create create = Mockito.mock(Create.class);

    ServiceAccountKey serviceAccountKey = new ServiceAccountKey();
    byte[] keyContent = "key data in JSON format".getBytes();
    serviceAccountKey.setPrivateKeyData(Base64.encodeBase64String(keyContent));

    when(mockApiFactory.newIamApi()).thenReturn(iam);
    when(iam.projects()).thenReturn(projects);
    when(projects.serviceAccounts()).thenReturn(serviceAccounts);
    when(serviceAccounts.keys()).thenReturn(keys);
    when(keys.create(anyString(), Matchers.any(CreateServiceAccountKeyRequest.class)))
        .thenReturn(create);

    if (throwException) {
      when(create.execute()).thenThrow(new IOException("log from unit test"));
    } else {
      when(create.execute()).thenReturn(serviceAccountKey);
    }
  }
}
