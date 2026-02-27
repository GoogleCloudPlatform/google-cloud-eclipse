/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.launcher;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsProperty;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsType;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.WritableDataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowDependencyManager;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for {@link DataflowPipelineLaunchDelegate}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DataflowPipelineLaunchDelegateTest {
  private DataflowPipelineLaunchDelegate dataflowDelegate;
  private final NullProgressMonitor monitor = new NullProgressMonitor();

  @Captor private ArgumentCaptor<Map<String, String>> variableMapCaptor;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private DataflowDependencyManager dependencyManager;
  @Mock private JavaLaunchDelegate javaDelegate;
  @Mock private IWorkspaceRoot workspaceRoot;
  @Mock private IProject project;
  @Mock private PipelineOptionsHierarchyFactory pipelineOptionsHierarchyFactory;
  @Mock private PipelineOptionsHierarchy pipelineOptionsHierarchy;
  @Mock private ILaunchConfigurationWorkingCopy configurationWorkingCopy;
  @Mock private GoogleApiFactory apiFactory;

  private final Map<String, String> pipelineArguments = new HashMap<>();
  private final Map<String, String> environmentMap = new HashMap<>();

  private Credential credential;

  @Before
  public void setup() throws Exception {
    GoogleApiFactory.setInstance(apiFactory);
    when(pipelineOptionsHierarchyFactory.forProject(
            eq(project), eq(MajorVersion.ONE), any(IProgressMonitor.class)))
        .thenReturn(pipelineOptionsHierarchy);

    credential = new GoogleCredential.Builder()
        .setJsonFactory(mock(JsonFactory.class))
        .setTransport(mock(HttpTransport.class))
        .setClientSecrets("clientId", "clientSecret").build();
    credential.setRefreshToken("fake-refresh-token");
    when(apiFactory.getCredential()).thenReturn(Optional.of(credential));

    when(dependencyManager.getProjectMajorVersion(project)).thenReturn(MajorVersion.ONE);
    dataflowDelegate = new DataflowPipelineLaunchDelegate(javaDelegate,
        pipelineOptionsHierarchyFactory, dependencyManager, workspaceRoot);

    pipelineArguments.put("accountEmail", "");
    when(configurationWorkingCopy.getAttribute(
        eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES), anyMapOf(String.class, String.class)))
        .thenReturn(environmentMap);
  }

  @After
  public void tearDown() {
    GoogleApiFactory.resetInstance();
  }
  
  @Test
  public void testGoogleApplicationCredentialsEnvironmentVariable() {
    assertEquals("GOOGLE_APPLICATION_CREDENTIALS",
        DataflowPipelineLaunchDelegate.GOOGLE_APPLICATION_CREDENTIALS_ENVIRONMENT_VARIABLE);
  }

  @Test
  public void testSetCredential_assumesAccountEmailIsGiven() throws CoreException {
    pipelineArguments.remove("accountEmail");

    try {
      dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
      fail();
    } catch (NullPointerException ex) {
      assertEquals("account email missing in launch configuration or preferences", ex.getMessage());
    }
  }

  @Test
  public void testSetCredential_credentialEnvironmentVariableSet_serviceAccountKey()
      throws IOException {
    pipelineArguments.put("serviceAccountKey", tempFolder.newFile().getAbsolutePath());
    environmentMap.put("GOOGLE_APPLICATION_CREDENTIALS", "user-set-path");

    try {
      dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
      fail();
    } catch (CoreException ex) {
      assertEquals("You cannot define the environment variable GOOGLE_APPLICATION_CREDENTIALS"
          + " when launching Dataflow pipelines from Cloud Tools for Eclipse.", ex.getMessage());
    }
  }

  @Test
  public void testSetCredential_credentialEnvironmentVariableSet_loginAccount() {
    pipelineArguments.put("accountEmail", "bogus@example.com");
    environmentMap.put("GOOGLE_APPLICATION_CREDENTIALS", "user-set-path");

    try {
      dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
      fail();
    } catch (CoreException ex) {
      assertEquals("You cannot define the environment variable GOOGLE_APPLICATION_CREDENTIALS"
          + " when launching Dataflow pipelines from Cloud Tools for Eclipse.", ex.getMessage());
    }
  }

  @Test
  public void testSetCredential_noCredentialGiven() {
    try {
      dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
      fail();
    } catch (CoreException ex) {
      assertEquals("No Google account selected for this launch.", ex.getMessage());
    }
  }

  @Test
  public void testSetCredential_savedAccountNotLoggedIn() {
    pipelineArguments.put("accountEmail", "bogus@example.com");
    when(apiFactory.getCredential()).thenReturn(Optional.empty());  // not logged in

    try {
      dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
      fail();
    } catch (CoreException ex) {
      assertEquals("The Google account saved for this launch is not logged in.", ex.getMessage());
    }
  }

  @Test
  public void testSetCredential_loginAccount() throws CoreException, IOException {
    pipelineArguments.put("accountEmail", "bogus@example.com");

    dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
    verifyLoginAccountSet();
  }

  @Test
  public void testSetCredential_serviceAccountTakesPrecedence() throws CoreException, IOException {
    String keyFile = tempFolder.newFile().getAbsolutePath();
    pipelineArguments.put("accountEmail", "bogus@example.com");
    pipelineArguments.put("serviceAccountKey", keyFile);

    dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
    verifyServiceAccountKeySet(keyFile);
  }

  private void verifyLoginAccountSet() throws IOException {
    verify(configurationWorkingCopy).setAttribute(
        eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES), variableMapCaptor.capture());
    String jsonCredentialPath = variableMapCaptor.getValue().get("GOOGLE_APPLICATION_CREDENTIALS");
    assertNotNull(jsonCredentialPath);
    assertThat(jsonCredentialPath, containsString("google-ct4e-"));
    assertThat(jsonCredentialPath, endsWith(".json"));

    Path credentialFile = Paths.get(jsonCredentialPath);
    assertTrue(Files.exists(credentialFile));

    String contents = new String(Files.readAllBytes(credentialFile), StandardCharsets.UTF_8);
    assertThat(contents, containsString("fake-refresh-token"));
  }

  private void verifyServiceAccountKeySet(String keyFileGiven) {
    verify(configurationWorkingCopy).setAttribute(
        eq(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES), variableMapCaptor.capture());
    String keyFile = variableMapCaptor.getValue().get("GOOGLE_APPLICATION_CREDENTIALS");
    assertEquals(keyFileGiven, keyFile);
  }

  @Test
  public void testSetCredential_originalEnvironmentMapUntouched_loginAccount()
      throws CoreException, IOException {
    pipelineArguments.put("accountEmail", "bogus@example.com");

    dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
    verifyLoginAccountSet();
    assertTrue(environmentMap.isEmpty());
  }

  @Test
  public void testSetCredential_originalEnvironmentMapUntouched_serviceAccount()
      throws CoreException, IOException {
    String keyFile = tempFolder.newFile().getAbsolutePath();
    pipelineArguments.put("serviceAccountKey", keyFile);

    dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
    verifyServiceAccountKeySet(keyFile);
    assertTrue(environmentMap.isEmpty());
  }

  @Test
  public void testSetCredential_nonExistingServiceAccountKey() {
    pipelineArguments.put("serviceAccountKey", "/non/existing/file.ext");

    try {
      dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
      fail();
    } catch (CoreException ex) {
      assertThat(ex.getMessage(), startsWith("Cannot open service account key file: "));
      assertThat(ex.getMessage(), containsString("existing"));
      assertThat(ex.getMessage(), endsWith("file.ext"));
    }
  }

  @Test
  public void testSetCredential_directoryAsServiceAccountKey() {
    pipelineArguments.put("serviceAccountKey", "/");

    try {
      dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
      fail();
    } catch (CoreException ex) {
      assertThat(ex.getMessage(), startsWith("Not a file but directory: "));
    }
  }

  @Test
  public void testSetCredential_serviceAccountKey() throws CoreException, IOException {
    String keyFile = tempFolder.newFile().getAbsolutePath();
    pipelineArguments.put("serviceAccountKey", keyFile);

    dataflowDelegate.setCredential(configurationWorkingCopy, pipelineArguments);
    verifyServiceAccountKeySet(keyFile);
  }

  @Test
  public void testLaunchWithLaunchConfigurationWithIncompleteArgsThrowsIllegalArgumentException()
      throws CoreException {
    ILaunchConfiguration configuration = mockILaunchConfiguration();
    Map<String, String> incompleteRequiredArguments = ImmutableMap.of();
    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(),
            ImmutableMap.<String, String>of())).thenReturn(incompleteRequiredArguments);

    Set<PipelineOptionsProperty> properties =
        ImmutableSet.of(requiredProperty("foo"), requiredProperty("bar-baz"));
    when(
        pipelineOptionsHierarchy.getRequiredOptionsByType(
            "com.google.cloud.dataflow.sdk.options.BlockingDataflowPipelineOptions"))
        .thenReturn(
            ImmutableMap.of(
                new PipelineOptionsType(
                    "MyOptions", Collections.<PipelineOptionsType>emptySet(), properties),
                properties));

    String mode = "run";
    ILaunch launch = mock(ILaunch.class);

    try {
      dataflowDelegate.launch(configuration, mode, launch, monitor);
      fail();
    } catch (IllegalArgumentException ex) {
      assertTrue(ex.getMessage().contains("Dataflow Pipeline Configuration is not valid"));
    }
  }

  @Test
  public void testLaunchWithProjectThatDoesNotExistThrowsCoreException() throws CoreException {
    ILaunchConfiguration configuration = mockILaunchConfiguration();
    when(project.exists()).thenReturn(false);

    String mode = "run";
    ILaunch launch = mock(ILaunch.class);

    try {
      dataflowDelegate.launch(configuration, mode, launch, monitor);
      fail();
    } catch (CoreException ex) {
      assertTrue(
          ex.getMessage().contains("Project \"Test-project,Name\" does not exist"));
    }
  }

  @Test
  public void testLaunchWithValidLaunchConfigurationCreatesJsonCredential()
      throws CoreException, IOException {
    ILaunchConfiguration configuration = mockILaunchConfiguration();
    when(configuration.getAttribute(
        "com.google.cloud.dataflow.eclipse.ALL_ARGUMENT_VALUES", new HashMap<String, String>()))
        .thenReturn(ImmutableMap.of("accountEmail", "bogus@example.com"));

    when(configuration.copy("dataflow_tmp_config_working_copy-testConfiguration"))
        .thenReturn(configurationWorkingCopy);

    WritableDataflowPreferences globalPreferences = WritableDataflowPreferences.global();
    globalPreferences.setDefaultAccountEmail("bogus@example.com");
    globalPreferences.save();

    dataflowDelegate.launch(configuration, "run" /* mode */, mock(ILaunch.class), monitor);
    verifyLoginAccountSet();
  }

  @Test
  public void testLaunchWithValidLaunchConfigurationSendsWorkingCopyToLaunchDelegate()
      throws CoreException {
    ILaunchConfiguration configuration = mockILaunchConfiguration();

    when(
        pipelineOptionsHierarchy.getPropertyNames(
            "com.google.cloud.dataflow.sdk.options.BlockingDataflowPipelineOptions"))
        .thenReturn(ImmutableSet.<String>of("foo", "bar", "baz", "Extra"));

    Map<String, String> argumentValues =
        ImmutableMap.<String, String>builder()
            .put("foo", "Spam")
            .put("bar", "Eggs")
            .put("baz", "Ham")
            .put("Extra", "PipelineArgs")
            .put("NotUsedInThisRunner", "Ignored")
            .put("accountEmail", "bogus@example.com")
            .build();

    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(),
            Collections.<String, String>emptyMap())).thenReturn(argumentValues);

    String javaArgs = "ExtraJavaArgs";
    when(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""))
        .thenReturn(javaArgs);
    when(configuration.copy("dataflow_tmp_config_working_copy-testConfiguration"))
        .thenReturn(configurationWorkingCopy);

    ILaunch launch = mock(ILaunch.class);

    dataflowDelegate.launch(configuration, "run", launch, monitor);

    Set<String> expectedArgumentComponents = ImmutableSet.of(
        "--runner=BlockingDataflowPipelineRunner", "--foo=Spam", "--bar=Eggs", "--baz=Ham",
        "--Extra=PipelineArgs", "ExtraJavaArgs");

    ArgumentCaptor<String> programArgumentsCaptor = ArgumentCaptor.forClass(String.class);

    verify(javaDelegate)
        .launch(eq(configurationWorkingCopy), eq("run"), eq(launch), any(IProgressMonitor.class));
    verify(configurationWorkingCopy)
        .setAttribute(
            eq(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS),
            programArgumentsCaptor.capture());

    String providedArguments = programArgumentsCaptor.getValue();
    String[] argumentComponents = providedArguments.split(" ");
    assertEquals(expectedArgumentComponents.size(), argumentComponents.length);
    assertTrue(expectedArgumentComponents.containsAll(Arrays.asList(argumentComponents)));
  }

  @Test
  public void testLaunchWithEmptyArgumentsDoesNotPassEmptyArguments() throws CoreException {
    ILaunchConfiguration configuration = mockILaunchConfiguration();

    when(
        pipelineOptionsHierarchy.getPropertyNames(
            "com.google.cloud.dataflow.sdk.options.BlockingDataflowPipelineOptions"))
        .thenReturn(ImmutableSet.of("foo", "bar", "baz", "Extra", "Empty"));

    // Need an order-preserving null-accepting map
    Map<String, String> argumentValues = new LinkedHashMap<>();
    argumentValues.put("foo", "Spam");
    argumentValues.put("bar", "Eggs");
    argumentValues.put("baz", "Ham");
    argumentValues.put("Extra", "PipelineArgs");
    argumentValues.put("NotUsedInThisRunner", "Ignored");
    argumentValues.put("Empty", "");
    argumentValues.put("accountEmail", "bogus@example.com");
    argumentValues.put("Null", null);

    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(),
            Collections.<String, String>emptyMap())).thenReturn(argumentValues);

    String javaArgs = "ExtraJavaArgs";
    when(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""))
        .thenReturn(javaArgs);
    when(configuration.copy("dataflow_tmp_config_working_copy-testConfiguration"))
        .thenReturn(configurationWorkingCopy);

    ILaunch launch = mock(ILaunch.class);

    dataflowDelegate.launch(configuration, "run", launch, monitor);

    Set<String> expectedArgumentComponents = ImmutableSet.of(
        "--runner=BlockingDataflowPipelineRunner", "--foo=Spam", "--bar=Eggs", "--baz=Ham",
        "--Extra=PipelineArgs", "ExtraJavaArgs");

    ArgumentCaptor<String> programArgumentsCaptor = ArgumentCaptor.forClass(String.class);

    verify(javaDelegate)
        .launch(eq(configurationWorkingCopy), eq("run"), eq(launch), any(IProgressMonitor.class));
    verify(configurationWorkingCopy)
        .setAttribute(
            eq(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS),
            programArgumentsCaptor.capture());

    String[] argumentComponents = programArgumentsCaptor.getValue().split(" ");
    assertEquals(expectedArgumentComponents.size(), argumentComponents.length);
    assertTrue(expectedArgumentComponents.containsAll(Arrays.asList(argumentComponents)));
  }

  private static PipelineOptionsProperty requiredProperty(String name) {
    return new PipelineOptionsProperty(name, false, true, Collections.<String>emptySet(), null);
  }

  private ILaunchConfiguration mockILaunchConfiguration() throws CoreException {
    ILaunchConfiguration configuration = mock(ILaunchConfiguration.class);
    String configurationName = "testConfiguration";
    when(configuration.getName()).thenReturn(configurationName);

    PipelineRunner runner = PipelineRunner.BLOCKING_DATAFLOW_PIPELINE_RUNNER;
    when(configuration.getAttribute(eq(PipelineConfigurationAttr.RUNNER_ARGUMENT.toString()),
        anyString())).thenReturn(runner.getRunnerName());

    String projectName = "Test-project,Name";
    when(configuration.getAttribute(eq(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME),
        anyString())).thenReturn(projectName);
    when(workspaceRoot.getProject(projectName)).thenReturn(project);
    when(project.exists()).thenReturn(true);

    return configuration;
  }
}
