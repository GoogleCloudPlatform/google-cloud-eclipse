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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.ProjectOrWorkspaceDataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowDependencyManager;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link ILaunchConfigurationDelegate} to launch a Google Cloud Dataflow Pipeline.
 *
 * <p>The JavaLaunchDelegate is responsible for most of the launching.
 */
public class DataflowPipelineLaunchDelegate extends ForwardingLaunchConfigurationDelegate {
  private static final String ARGUMENT_FORMAT_STR = "--%s=%s";

  @VisibleForTesting
  static final String DATAFLOW_LAUNCH_CONFIG_WORKING_COPY_PREFIX =
      "dataflow_tmp_config_working_copy-";
  @VisibleForTesting
  static final String RUNNER_COMMAND_LINE_STRING = "runner";

  private static final Joiner SPACE_JOINER = Joiner.on(" ").skipNulls();

  private final JavaLaunchDelegate delegate;
  private final PipelineOptionsHierarchyFactory optionsRetrieverFactory;
  private final IWorkspaceRoot workspaceRoot;
  private final DataflowDependencyManager dependencyManager;

  public DataflowPipelineLaunchDelegate() {
    this(
        new JavaLaunchDelegate(),
        new ClasspathPipelineOptionsHierarchyFactory(),
        DataflowDependencyManager.create(),
        ResourcesPlugin.getWorkspace().getRoot());
  }

  @VisibleForTesting
  DataflowPipelineLaunchDelegate(
      JavaLaunchDelegate javaLaunchDelegate,
      PipelineOptionsHierarchyFactory optionsHierarchyFactory,
      DataflowDependencyManager dependencyManager,
      IWorkspaceRoot workspaceRoot) {
    this.delegate = javaLaunchDelegate;
    this.optionsRetrieverFactory = optionsHierarchyFactory;
    this.dependencyManager = dependencyManager;
    this.workspaceRoot = workspaceRoot;
  }

  @Override
  public void launch(
      ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 3);
    PipelineLaunchConfiguration pipelineConfig =
        PipelineLaunchConfiguration.fromLaunchConfiguration(configuration);

    String projectName =
        configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
    IProject project = workspaceRoot.getProject(projectName);
    checkArgument(
        project.exists(),
        "Project with name %s must exist to launch. Got launch attributes %s",
        projectName,
        configuration.getAttributes());
    MajorVersion majorVersion = dependencyManager.getProjectMajorVersion(project);

    PipelineOptionsHierarchy hierarchy;
    try {
      hierarchy = optionsRetrieverFactory.forProject(project, majorVersion, progress.newChild(1));
    } catch (PipelineOptionsRetrievalException e) {
      throw new CoreException(new Status(Status.ERROR, DataflowCorePlugin.PLUGIN_ID,
          "Could not retrieve Pipeline Options Hierarchy for project " + projectName, e));
    }

    if (!pipelineConfig.isValid(hierarchy, getPreferences(pipelineConfig))) {
      throw new IllegalArgumentException(
          "Provided Dataflow Pipeline Configuration is not valid: " + pipelineConfig.toString());
    }

    List<String> argComponents = getArguments(configuration, pipelineConfig, hierarchy);

    ILaunchConfigurationWorkingCopy workingCopy =
        configuration.copy(DATAFLOW_LAUNCH_CONFIG_WORKING_COPY_PREFIX + configuration.getName());
    workingCopy.setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, SPACE_JOINER.join(argComponents));

    delegate.launch(workingCopy, mode, launch, progress.newChild(1));
  }

  private List<String> getArguments(
      ILaunchConfiguration configuration,
      PipelineLaunchConfiguration pipelineConfig,
      PipelineOptionsHierarchy optionsHierarchy)
      throws CoreException {
    List<String> argComponents = new ArrayList<>();

    argComponents.add(String.format(ARGUMENT_FORMAT_STR, RUNNER_COMMAND_LINE_STRING,
        pipelineConfig.getRunner().getRunnerName()));

    Set<String> pipelineArgs;
    if (pipelineConfig.getUserOptionsName() != null
        && !pipelineConfig.getUserOptionsName().isEmpty()) {
      pipelineArgs = optionsHierarchy.getPropertyNames(
          pipelineConfig.getRunner().getOptionsClass(), pipelineConfig.getUserOptionsName());
    } else {
      pipelineArgs =
          optionsHierarchy.getPropertyNames(pipelineConfig.getRunner().getOptionsClass());
    }
    Map<String, String> argumentValues = new HashMap<>(pipelineConfig.getArgumentValues());

    if (pipelineConfig.isUseDefaultLaunchOptions()) {
      DataflowPreferences preferences = getPreferences(pipelineConfig);
      argumentValues.putAll(preferences.asDefaultPropertyMap());
    }

    for (Map.Entry<String, String> argValueEntry : argumentValues.entrySet()) {
      if (!Strings.isNullOrEmpty(argValueEntry.getValue())
          && pipelineArgs.contains(argValueEntry.getKey())) {
        argComponents.add(
            String.format(ARGUMENT_FORMAT_STR, argValueEntry.getKey(), argValueEntry.getValue()));
      }
    }

    argComponents.add(
        configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""));
    return argComponents;
  }

  private DataflowPreferences getPreferences(PipelineLaunchConfiguration config) {
    if (config.getEclipseProjectName() != null) {
      IProject project = workspaceRoot.getProject(config.getEclipseProjectName());
      if (project.exists()) {
        return ProjectOrWorkspaceDataflowPreferences.forProject(project);
      }
    }
    return ProjectOrWorkspaceDataflowPreferences.forWorkspace();
  }

  @Override
  protected ILaunchConfigurationDelegate2 delegate() {
    return delegate;
  }
}
