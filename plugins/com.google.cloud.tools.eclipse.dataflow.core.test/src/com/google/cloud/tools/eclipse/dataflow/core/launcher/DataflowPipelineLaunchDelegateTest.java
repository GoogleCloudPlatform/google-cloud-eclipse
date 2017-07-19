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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
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
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for {@link DataflowPipelineLaunchDelegate}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DataflowPipelineLaunchDelegateTest {
  private DataflowPipelineLaunchDelegate dataflowDelegate;
  private NullProgressMonitor monitor = new NullProgressMonitor();

  @Mock
  private DataflowDependencyManager dependencyManager;

  @Mock
  private JavaLaunchDelegate javaDelegate;

  @Mock
  private IWorkspaceRoot workspaceRoot;

  @Mock
  private IProject project;

  @Mock
  private PipelineOptionsHierarchyFactory pipelineOptionsHierarchyFactory;

  @Mock
  private PipelineOptionsHierarchy pipelineOptionsHierarchy;

  @Mock
  private IGoogleLoginService loginService;

  private Credential credential;

  @Before
  public void setup() throws Exception {
    when(pipelineOptionsHierarchyFactory.forProject(
            eq(project), eq(MajorVersion.ONE), any(IProgressMonitor.class)))
        .thenReturn(pipelineOptionsHierarchy);

    credential = new GoogleCredential.Builder()
        .setJsonFactory(mock(JsonFactory.class))
        .setTransport(mock(HttpTransport.class))
        .setClientSecrets("clientId", "clientSecret").build();
    credential.setRefreshToken("fake-refresh-token");
    when(loginService.getCredential("bogus@example.com")).thenReturn(credential);

    when(dependencyManager.getProjectMajorVersion(project)).thenReturn(MajorVersion.ONE);
    dataflowDelegate = new DataflowPipelineLaunchDelegate(javaDelegate,
        pipelineOptionsHierarchyFactory, dependencyManager, workspaceRoot, loginService);
  }

  @Test
  public void testGoogleApplicationCredentialsEnvironmentVariable() {
    assertEquals("GOOGLE_APPLICATION_CREDENTIALS",
        DataflowPipelineLaunchDelegate.GOOGLE_APPLICATION_CREDENTIALS_ENVIRONMENT_VARIABLE);
  }

  @Test
  public void testSetLoginCredential_assumesAccountEmailIsGiven() throws CoreException {
    ILaunchConfigurationWorkingCopy workingCopy = mockILaunchConfigurationWorkingCopy();

    try {
      dataflowDelegate.setLoginCredential(workingCopy, null /* accountEmail */);
      fail();
    } catch (NullPointerException ex) {
      assertEquals("account email missing in launch configuration or preferences", ex.getMessage());
    }
  }

  @Test
  public void testSetLoginCredential_userSetsCredentialEnvironmentVariable() throws CoreException {
    Map<String, String> environmentVariables = ImmutableMap.of(
        "GOOGLE_APPLICATION_CREDENTIALS", "user-set-path");
    ILaunchConfigurationWorkingCopy workingCopy =
        mockILaunchConfigurationWorkingCopy(environmentVariables);

    try {
      dataflowDelegate.setLoginCredential(workingCopy, "bogus@example.com");
      fail();
    } catch (CoreException ex) {
      assertEquals("You cannot define the environment variable GOOGLE_APPLICATION_CREDENTIALS"
          + " when launching Dataflow pipelines from Cloud Tools for Eclipse.", ex.getMessage());
    }
  }

  @Test
  public void testSetLoginCredential_noAccountSelected() throws CoreException {
    ILaunchConfigurationWorkingCopy workingCopy = mockILaunchConfigurationWorkingCopy();
    when(loginService.getCredential("bogus@example.com")).thenReturn(null);  // not logged in

    try {
      dataflowDelegate.setLoginCredential(workingCopy, "" /* accountEmail */);
      fail();
    } catch (CoreException ex) {
      assertEquals("No Google account selected for this launch.", ex.getMessage());
    }
  }

  @Test
  public void testSetLoginCredential_savedAccountNotLoggedIn() throws CoreException {
    ILaunchConfigurationWorkingCopy workingCopy = mockILaunchConfigurationWorkingCopy();
    when(loginService.getCredential("bogus@example.com")).thenReturn(null);  // not logged in

    try {
      dataflowDelegate.setLoginCredential(workingCopy, "bogus@example.com");
      fail();
    } catch (CoreException ex) {
      assertEquals("The Google account saved for this lanuch is not logged in.", ex.getMessage());
    }
  }

  @Test
  public void testSetLoginCredential() throws CoreException, IOException {
    Map<String, String> environmentVariables = new HashMap<>();
    ILaunchConfigurationWorkingCopy workingCopy =
        mockILaunchConfigurationWorkingCopy(environmentVariables);

    dataflowDelegate.setLoginCredential(workingCopy, "bogus@example.com");
    String jsonCredentialPath = environmentVariables.get("GOOGLE_APPLICATION_CREDENTIALS");
    assertNotNull(jsonCredentialPath);
    assertThat(jsonCredentialPath, containsString("google-ct4e-"));
    assertThat(jsonCredentialPath, endsWith(".json"));

    Path credentialFile = Paths.get(jsonCredentialPath);
    assertTrue(Files.exists(credentialFile));

    String contents = new String(Files.readAllBytes(credentialFile), StandardCharsets.UTF_8);
    assertThat(contents, containsString("fake-refresh-token"));
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
  public void testLaunchWithProjectThatDoesNotExistThrowsIllegalArgumentException()
      throws CoreException {
    ILaunchConfiguration configuration = mockILaunchConfiguration();
    when(project.exists()).thenReturn(false);

    String mode = "run";
    ILaunch launch = mock(ILaunch.class);

    try {
      dataflowDelegate.launch(configuration, mode, launch, monitor);
      fail();
    } catch (IllegalArgumentException ex) {
      assertTrue(
          ex.getMessage().contains("Project with name Test-project,Name must exist to launch."));
    }
  }

  @Test
  public void testLaunchWithValidLaunchConfigurationCreatesJsonCredential() throws CoreException {
    ILaunchConfiguration configuration = mockILaunchConfiguration();
    when(configuration.getAttribute(
        "com.google.cloud.dataflow.eclipse.ALL_ARGUMENT_VALUES", new HashMap<String, String>()))
        .thenReturn(ImmutableMap.of("accountEmail", "bogus@example.com"));

    Map<String, String> environmentVariables = new HashMap<>();
    ILaunchConfigurationWorkingCopy expectedConfiguration =
        mockILaunchConfigurationWorkingCopy(environmentVariables);
    when(configuration.copy("dataflow_tmp_config_working_copy-testConfiguration"))
        .thenReturn(expectedConfiguration);

    WritableDataflowPreferences globalPreferences = WritableDataflowPreferences.global();
    globalPreferences.setDefaultAccountEmail("bogus@example.com");
    globalPreferences.save();

    dataflowDelegate.launch(configuration, "run" /* mode */, mock(ILaunch.class), monitor);

    String jsonCredentialPath = environmentVariables.get("GOOGLE_APPLICATION_CREDENTIALS");
    assertNotNull(jsonCredentialPath);
    assertTrue(Files.exists(Paths.get(jsonCredentialPath)));
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
    ILaunchConfigurationWorkingCopy expectedConfiguration = mockILaunchConfigurationWorkingCopy();
    when(configuration.copy("dataflow_tmp_config_working_copy-testConfiguration"))
        .thenReturn(expectedConfiguration);

    String mode = "run";
    ILaunch launch = mock(ILaunch.class);

    dataflowDelegate.launch(configuration, mode, launch, monitor);

    Set<String> expectedArgumentComponents = ImmutableSet.of(
        "--runner=BlockingDataflowPipelineRunner", "--foo=Spam", "--bar=Eggs", "--baz=Ham",
        "--Extra=PipelineArgs", "ExtraJavaArgs");

    ArgumentCaptor<String> programArgumentsCaptor = ArgumentCaptor.forClass(String.class);

    verify(javaDelegate)
        .launch(eq(expectedConfiguration), eq(mode), eq(launch), any(IProgressMonitor.class));
    verify(expectedConfiguration)
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

    // Need a order-preserving null-accepting map
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
    ILaunchConfigurationWorkingCopy expectedConfiguration = mockILaunchConfigurationWorkingCopy();
    when(configuration.copy("dataflow_tmp_config_working_copy-testConfiguration"))
        .thenReturn(expectedConfiguration);

    String mode = "run";
    ILaunch launch = mock(ILaunch.class);

    dataflowDelegate.launch(configuration, mode, launch, monitor);

    Set<String> expectedArgumentComponents = ImmutableSet.of(
        "--runner=BlockingDataflowPipelineRunner", "--foo=Spam", "--bar=Eggs", "--baz=Ham",
        "--Extra=PipelineArgs", "ExtraJavaArgs");

    ArgumentCaptor<String> programArgumentsCaptor = ArgumentCaptor.forClass(String.class);

    verify(javaDelegate)
        .launch(eq(expectedConfiguration), eq(mode), eq(launch), any(IProgressMonitor.class));
    verify(expectedConfiguration)
        .setAttribute(
            eq(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS),
            programArgumentsCaptor.capture());

    String[] argumentComponents = programArgumentsCaptor.getValue().split(" ");
    assertEquals(expectedArgumentComponents.size(), argumentComponents.length);
    assertTrue(expectedArgumentComponents.containsAll(Arrays.asList(argumentComponents)));
  }

  private PipelineOptionsProperty requiredProperty(String name) {
    return new PipelineOptionsProperty(name, false, true, Collections.<String>emptySet(), null);
  }

  private ILaunchConfiguration mockILaunchConfiguration() throws CoreException {
    ILaunchConfiguration configuration = mock(ILaunchConfiguration.class);
    String configurationName = "testConfiguration";
    when(configuration.getName()).thenReturn(configurationName);

    PipelineRunner runner = PipelineRunner.BLOCKING_DATAFLOW_PIPELINE_RUNNER;
    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.RUNNER_ARGUMENT.toString(),
            PipelineLaunchConfiguration.defaultRunner(MajorVersion.ONE).getRunnerName()))
        .thenReturn(runner.getRunnerName());

    String projectName = "Test-project,Name";
    when(configuration.getAttribute(
        eq(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME), anyString()))
        .thenReturn(projectName);
    when(workspaceRoot.getProject(projectName)).thenReturn(project);
    when(project.exists()).thenReturn(true);

    return configuration;
  }

  private static ILaunchConfigurationWorkingCopy mockILaunchConfigurationWorkingCopy()
      throws CoreException {
    return mockILaunchConfigurationWorkingCopy(new HashMap<String, String>());
  }

  private static ILaunchConfigurationWorkingCopy mockILaunchConfigurationWorkingCopy(
      Map<String, String> environmentVariables) throws CoreException {
    ILaunchConfigurationWorkingCopy workingCopy = mock(ILaunchConfigurationWorkingCopy.class);
    when(workingCopy.getAttribute(
        ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap<String, String>()))
        .thenReturn(environmentVariables);
    return workingCopy;
  }
}
