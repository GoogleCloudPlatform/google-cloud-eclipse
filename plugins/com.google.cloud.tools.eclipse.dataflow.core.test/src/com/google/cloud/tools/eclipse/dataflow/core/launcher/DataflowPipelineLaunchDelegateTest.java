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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsProperty;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsType;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowDependencyManager;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link DataflowPipelineLaunchDelegate}.
 */
public class DataflowPipelineLaunchDelegateTest {
  private DataflowPipelineLaunchDelegate dataflowDelegate;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

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

  private MajorVersion majorVersion = MajorVersion.ONE;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(
            pipelineOptionsHierarchyFactory.forProject(
                Mockito.eq(project), Mockito.eq(majorVersion), Mockito.<IProgressMonitor>any()))
        .thenReturn(pipelineOptionsHierarchy);

    when(dependencyManager.getProjectMajorVersion(project)).thenReturn(MajorVersion.ONE);
    dataflowDelegate = new DataflowPipelineLaunchDelegate(
        javaDelegate, pipelineOptionsHierarchyFactory, dependencyManager, workspaceRoot);
  }

  @Test
  public void testLaunchWithLaunchConfigurationWithIncompleteArgsThrowsIllegalArgumentException()
      throws CoreException {
    ILaunchConfiguration configuration = mock(ILaunchConfiguration.class);
    Map<String, String> incompleteRequiredArguments = ImmutableMap.<String, String>of();
    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(),
            ImmutableMap.<String, String>of())).thenReturn(incompleteRequiredArguments);

    PipelineRunner runner = PipelineRunner.BLOCKING_DATAFLOW_PIPELINE_RUNNER;
    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.RUNNER_ARGUMENT.toString(),
            PipelineLaunchConfiguration.defaultRunner(majorVersion).getRunnerName()))
        .thenReturn(runner.getRunnerName());
    Set<PipelineOptionsProperty> properties =
        ImmutableSet.of(requiredProperty("foo"), requiredProperty("bar-baz"));
    when(
        pipelineOptionsHierarchy.getRequiredOptionsByType(
            "com.google.cloud.dataflow.sdk.options.BlockingDataflowPipelineOptions"))
        .thenReturn(
            ImmutableMap.<PipelineOptionsType, Set<PipelineOptionsProperty>>builder()
                .put(
                    new PipelineOptionsType(
                        "MyOptions", Collections.<PipelineOptionsType>emptySet(), properties),
                    properties)
                .build());

    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.RUNNER_ARGUMENT.toString(),
            PipelineLaunchConfiguration.defaultRunner(majorVersion).getRunnerName()))
        .thenReturn(runner.getRunnerName());

    String projectName = "Test-project,Name";
    when(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""))
        .thenReturn(projectName);
    when(workspaceRoot.getProject(projectName)).thenReturn(project);
    when(project.exists()).thenReturn(true);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Dataflow Pipeline Configuration is not valid");

    String mode = "run";
    ILaunch launch = mock(ILaunch.class);
    NullProgressMonitor monitor = new NullProgressMonitor();

    dataflowDelegate.launch(configuration, mode, launch, monitor);
  }

  @Test
  public void testLaunchWithProjectThatDoesNotExistThrowsIllegalArgumentException()
      throws CoreException {
    ILaunchConfiguration configuration = mock(ILaunchConfiguration.class);
    String projectName = "Test-project,Name";
    when(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""))
        .thenReturn(projectName);
    when(workspaceRoot.getProject(projectName)).thenReturn(project);
    when(project.exists()).thenReturn(false);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Project with name");
    thrown.expectMessage(projectName);
    thrown.expectMessage("must exist to launch");

    String mode = "run";
    ILaunch launch = mock(ILaunch.class);
    NullProgressMonitor monitor = new NullProgressMonitor();

    dataflowDelegate.launch(configuration, mode, launch, monitor);
  }

  @Test
  public void testLaunchWithValidLaunchConfigurationSendsWorkingCopyToLaunchDelegate()
      throws CoreException {
    ILaunchConfiguration configuration = mock(ILaunchConfiguration.class);
    String configurationName = "testConfiguration";
    when(configuration.getName()).thenReturn(configurationName);

    PipelineRunner runner = PipelineRunner.BLOCKING_DATAFLOW_PIPELINE_RUNNER;
    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.RUNNER_ARGUMENT.toString(),
            PipelineLaunchConfiguration.defaultRunner(majorVersion).getRunnerName()))
        .thenReturn(runner.getRunnerName());

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
            .build();

    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(),
            Collections.<String, String>emptyMap())).thenReturn(argumentValues);

    String javaArgs = "ExtraJavaArgs";
    when(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""))
        .thenReturn(javaArgs);
    ILaunchConfigurationWorkingCopy expectedConfiguration =
        mock(ILaunchConfigurationWorkingCopy.class);
    when(
        configuration.copy(DataflowPipelineLaunchDelegate.DATAFLOW_LAUNCH_CONFIG_WORKING_COPY_PREFIX
            + configurationName)).thenReturn(expectedConfiguration);

    String projectName = "Test-project,Name";
    when(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""))
        .thenReturn(projectName);
    when(workspaceRoot.getProject(projectName)).thenReturn(project);
    when(project.exists()).thenReturn(true);

    String mode = "run";
    ILaunch launch = mock(ILaunch.class);
    NullProgressMonitor monitor = new NullProgressMonitor();

    dataflowDelegate.launch(configuration, mode, launch, monitor);

    Set<String> expectedArgumentComponents = ImmutableSet.of("--runner=" + runner.getRunnerName(),
        "--foo=Spam", "--bar=Eggs", "--baz=Ham", "--Extra=PipelineArgs", "ExtraJavaArgs");

    ArgumentCaptor<String> programArgumentsCaptor = ArgumentCaptor.forClass(String.class);

    verify(javaDelegate)
        .launch(Mockito.eq(expectedConfiguration), Mockito.eq(mode), Mockito.eq(launch),
            Mockito.<IProgressMonitor>any());
    verify(expectedConfiguration)
        .setAttribute(
            Mockito.<String>eq(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS),
            programArgumentsCaptor.capture());

    String providedArguments = programArgumentsCaptor.getValue();
    String[] argumentComponents = providedArguments.split(" ");
    assertEquals(expectedArgumentComponents.size(), argumentComponents.length);
    assertTrue(expectedArgumentComponents.containsAll(Arrays.asList(argumentComponents)));
  }

  @Test
  public void testLaunchWithEmptyArgumentsDoesNotPassEmptyArguments() throws CoreException {
    ILaunchConfiguration configuration = mock(ILaunchConfiguration.class);
    String configurationName = "testConfiguration";
    when(configuration.getName()).thenReturn(configurationName);

    PipelineRunner runner = PipelineRunner.BLOCKING_DATAFLOW_PIPELINE_RUNNER;
    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.RUNNER_ARGUMENT.toString(),
            PipelineLaunchConfiguration.defaultRunner(majorVersion).getRunnerName()))
        .thenReturn(runner.getRunnerName());

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
    argumentValues.put("Null", null);

    when(
        configuration.getAttribute(
            PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(),
            Collections.<String, String>emptyMap())).thenReturn(argumentValues);

    String javaArgs = "ExtraJavaArgs";
    when(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""))
        .thenReturn(javaArgs);
    ILaunchConfigurationWorkingCopy expectedConfiguration =
        mock(ILaunchConfigurationWorkingCopy.class);
    when(
        configuration.copy(DataflowPipelineLaunchDelegate.DATAFLOW_LAUNCH_CONFIG_WORKING_COPY_PREFIX
            + configurationName)).thenReturn(expectedConfiguration);

    String projectName = "Test-project,Name";
    when(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""))
        .thenReturn(projectName);
    when(workspaceRoot.getProject(projectName)).thenReturn(project);
    when(project.exists()).thenReturn(true);

    String mode = "run";
    ILaunch launch = mock(ILaunch.class);
    NullProgressMonitor monitor = new NullProgressMonitor();

    dataflowDelegate.launch(configuration, mode, launch, monitor);

    Set<String> expectedArgumentComponents = ImmutableSet.of("--runner=" + runner.getRunnerName(),
        "--foo=Spam", "--bar=Eggs", "--baz=Ham", "--Extra=PipelineArgs", "ExtraJavaArgs");

    ArgumentCaptor<String> programArgumentsCaptor = ArgumentCaptor.forClass(String.class);

    verify(javaDelegate)
        .launch(Mockito.eq(expectedConfiguration), Mockito.eq(mode), Mockito.eq(launch),
            Mockito.<IProgressMonitor>any());
    verify(expectedConfiguration)
        .setAttribute(
            Mockito.<String>eq(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS),
            programArgumentsCaptor.capture());

    String[] argumentComponents = programArgumentsCaptor.getValue().split(" ");
    assertEquals(expectedArgumentComponents.size(), argumentComponents.length);
    assertTrue(expectedArgumentComponents.containsAll(Arrays.asList(argumentComponents)));
  }

  private PipelineOptionsProperty requiredProperty(String name) {
    return new PipelineOptionsProperty(name, false, true, Collections.<String>emptySet(), null);
  }
}
