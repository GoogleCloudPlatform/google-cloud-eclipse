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

import static com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineRunner.DIRECT_PIPELINE_RUNNER;
import static com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineRunner.DIRECT_RUNNER;

import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsProperty;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsType;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * A POJO that contains options specific to launching a dataflow pipeline. Forces having a project
 * and major version as it doesn't make sense to launch without.
 */
public class PipelineLaunchConfiguration {
  /**
   * Properties that can be obtained via methods other than getArgumentValues.
   */
  static final Set<String> PROVIDED_PROPERTY_NAMES = ImmutableSet.of("runner");

  private final MajorVersion majorVersion;

  private PipelineRunner runner;
  private boolean useDefaultLaunchOptions = true;
  private Map<String, String> argumentValues = Collections.<String, String>emptyMap();
  private Optional<String> userOptionsName = Optional.absent();

  /**
   * Construct a DataflowPipelineLaunchConfiguration from the provided {@link ILaunchConfiguration}.
   */
  public static PipelineLaunchConfiguration fromLaunchConfiguration(MajorVersion majorVersion,
      ILaunchConfiguration launchConfiguration) throws CoreException {
    PipelineLaunchConfiguration configuration =
        new PipelineLaunchConfiguration(majorVersion);
    configuration.setValuesFromLaunchConfiguration(launchConfiguration);
    return configuration;
  }

  public static PipelineRunner defaultRunner(MajorVersion majorVersion) {
    switch (majorVersion) {
      case ONE:
        return DIRECT_PIPELINE_RUNNER;
      default:
        return DIRECT_RUNNER;
    }
  }

  /** Create a new pipeline launch configuration with no default runner. */
  @VisibleForTesting
  PipelineLaunchConfiguration(MajorVersion majorVersion) {
    Preconditions.checkNotNull(majorVersion);
    this.majorVersion = majorVersion;
  }

  /**
   * Return the configured runner if set, or the default runner. Should never be {@code null}.
   */
  public PipelineRunner getRunner() {
    if (runner != null) {
      return runner;
    }
    return defaultRunner(majorVersion);
  }

  public void setRunner(PipelineRunner runner) {
    this.runner = runner;
  }

  public String getUserOptionsName() {
    return userOptionsName.orNull();
  }

  public void setUserOptionsName(String userOptionsName) {
    this.userOptionsName = Optional.fromNullable(userOptionsName);
  }

  public Map<String, String> getArgumentValues() {
    return argumentValues;
  }

  public void setArgumentValues(Map<String, String> allRequiredArgs) {
    this.argumentValues = allRequiredArgs;
  }

  public boolean isUseDefaultLaunchOptions() {
    return useDefaultLaunchOptions;
  }

  public void setUseDefaultLaunchOptions(boolean useDefaultLaunchOptions) {
    this.useDefaultLaunchOptions = useDefaultLaunchOptions;
  }

  public MajorVersion getMajorVersion() {
    return majorVersion;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    PipelineLaunchConfiguration other = (PipelineLaunchConfiguration) obj;
    return Objects.equals(majorVersion, other.majorVersion)
        && Objects.equals(argumentValues, other.argumentValues)
        && useDefaultLaunchOptions == other.useDefaultLaunchOptions
        && Objects.equals(runner, other.runner)
        && Objects.equals(userOptionsName, other.userOptionsName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(majorVersion, argumentValues, useDefaultLaunchOptions, runner,
        userOptionsName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass())
        .add("runnerName", runner != null ? runner.getRunnerName() : "(default runner)")
        .add("argumentValues", argumentValues).toString();
  }

  private void setValuesFromLaunchConfiguration(ILaunchConfiguration configuration)
      throws CoreException {
    // never set a default runner unless actually specified
    String runnerArgument = configuration
        .getAttribute(PipelineConfigurationAttr.RUNNER_ARGUMENT.toString(), (String) null);
    if (runnerArgument != null) {
      setRunner(PipelineRunner.fromRunnerName(runnerArgument));
    }

    setUseDefaultLaunchOptions(configuration.getAttribute(
        PipelineConfigurationAttr.USE_DEFAULT_LAUNCH_OPTIONS.toString(),
        isUseDefaultLaunchOptions()));

    setArgumentValues(configuration.getAttribute(
        PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(), getArgumentValues()));

    setUserOptionsName(configuration.getAttribute(
        PipelineConfigurationAttr.USER_OPTIONS_NAME.toString(), getUserOptionsName()));
  }

  /**
   * Stores the Dataflow Pipeline-specific options of this LaunchConfiguration inside the provided
   * {@link ILaunchConfigurationWorkingCopy}.
   */
  public void toLaunchConfiguration(ILaunchConfigurationWorkingCopy configuration) {
    if (runner != null) {
      configuration.setAttribute(PipelineConfigurationAttr.RUNNER_ARGUMENT.toString(),
          runner.getRunnerName());
    }
    configuration.setAttribute(
        PipelineConfigurationAttr.USE_DEFAULT_LAUNCH_OPTIONS.toString(), useDefaultLaunchOptions);
    configuration.setAttribute(
        PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(), argumentValues);
    configuration.setAttribute(
        PipelineConfigurationAttr.USER_OPTIONS_NAME.toString(), userOptionsName.orNull());
  }

  /**
   * Checks if this DataflowPipelineLaunchConfiguration is superficially valid. This requires the
   * runner to be supported and all of the required options to be present in the allRequiredArgs
   * map.
   */
  public boolean isValid(PipelineOptionsHierarchy hierarchy, DataflowPreferences preferences) {
    return getMissingRequiredProperties(hierarchy, preferences).isEmpty();
  }

  public Map<PipelineOptionsType, Set<PipelineOptionsProperty>> getRequiredProperties(
      PipelineOptionsHierarchy hierarchy) {
    Map<PipelineOptionsType, Set<PipelineOptionsProperty>> requiredOptions;
    if (userOptionsName.isPresent()) {
      requiredOptions =
          hierarchy.getRequiredOptionsByType(getRunner().getOptionsClass(), userOptionsName.get());
    } else {
      requiredOptions = hierarchy.getRequiredOptionsByType(getRunner().getOptionsClass());
    }
    return requiredOptions;
  }

  /**
   * Gets the collection of properties that must be provided by the user but are not filled in. A
   * non-empty result means the launch configuration is not valid.
   */
  public MissingRequiredProperties getMissingRequiredProperties(
      PipelineOptionsHierarchy hierarchy, DataflowPreferences preferences) {
    Map<PipelineOptionsType, Set<PipelineOptionsProperty>> requiredOptions =
        getRequiredProperties(hierarchy);

    Collection<Set<PipelineOptionsProperty>> requiredPropertySets = requiredOptions.values();

    Set<PipelineOptionsProperty> missingValues =
        getMissingValues(requiredPropertySets, preferences);

    Map<String, Set<PipelineOptionsProperty>> missingGroups =
        getMissingGroups(requiredPropertySets, preferences);

    return new MissingRequiredProperties(missingValues, missingGroups);
  }

  private Set<PipelineOptionsProperty> getMissingValues(
      Collection<Set<PipelineOptionsProperty>> requiredPropertySets,
      DataflowPreferences preferences) {
    Set<PipelineOptionsProperty> missingValues = new HashSet<>();

    for (Set<PipelineOptionsProperty> requiredProperties : requiredPropertySets) {
      for (PipelineOptionsProperty requiredProperty : requiredProperties) {
        String propertyName = requiredProperty.getName();
        if (PROVIDED_PROPERTY_NAMES.contains(propertyName)
            || !requiredProperty.isUserValueRequired()) {
          continue;
        }

        if (requiredProperty.getGroups().isEmpty()) {
          if (!isPropertySpecified(propertyName, preferences)) {
            missingValues.add(requiredProperty);
          }
        }
      }
    }
    return missingValues;
  }

  private Map<String, Set<PipelineOptionsProperty>> getMissingGroups(
      Collection<Set<PipelineOptionsProperty>> requiredPropertySets,
      DataflowPreferences preferences) {
    Map<String, Set<PipelineOptionsProperty>> requiredGroups =
        getRequiredGroups(requiredPropertySets);

    Map<String, Set<PipelineOptionsProperty>> missingGroups = new HashMap<>();
    for (Map.Entry<String, Set<PipelineOptionsProperty>> groupProperties :
      requiredGroups.entrySet()) {

      boolean groupSatisfied = false;
      for (PipelineOptionsProperty groupProperty : groupProperties.getValue()) {
        if (isPropertySpecified(groupProperty.getName(), preferences)) {
          groupSatisfied = true;
          break;
        }
      }
      if (!groupSatisfied) {
        missingGroups.put(groupProperties.getKey(), groupProperties.getValue());
      }
    }
    return missingGroups;
  }

  private Map<String, Set<PipelineOptionsProperty>> getRequiredGroups(
      Collection<Set<PipelineOptionsProperty>> requiredPropertySets) {
    Map<String, Set<PipelineOptionsProperty>> requiredGroups = new HashMap<>();
    for (Set<PipelineOptionsProperty> requiredProperties : requiredPropertySets) {
      for (PipelineOptionsProperty requiredProperty : requiredProperties) {
        if (PROVIDED_PROPERTY_NAMES.contains(requiredProperty.getName())
            || !requiredProperty.isUserValueRequired()) {
          continue;
        }

        if (!requiredProperty.getGroups().isEmpty()) {
          for (String group : requiredProperty.getGroups()) {
            if (!requiredGroups.containsKey(group)) {
              requiredGroups.put(group, new HashSet<PipelineOptionsProperty>());
            }
            requiredGroups.get(group).add(requiredProperty);
          }
        }
      }
    }
    return requiredGroups;
  }

  private boolean isPropertySpecified(String propertyName, DataflowPreferences preferences) {
    if (isUseDefaultLaunchOptions()
        && DataflowPreferences.SUPPORTED_DEFAULT_PROPERTIES.contains(propertyName)) {
      return !Strings.isNullOrEmpty(preferences.asDefaultPropertyMap().get(propertyName));
    } else {
      return !Strings.isNullOrEmpty(argumentValues.get(propertyName));
    }
  }

  /**
   * The missing properties and groups causing a launch configuration to be invalid.
   */
  public static class MissingRequiredProperties {
    private final Set<PipelineOptionsProperty> missingProperties;
    private final Map<String, Set<PipelineOptionsProperty>> missingGroups;

    private MissingRequiredProperties(Set<PipelineOptionsProperty> missingProperties,
        Map<String, Set<PipelineOptionsProperty>> missingGroups) {
      this.missingProperties = missingProperties;
      this.missingGroups = missingGroups;
    }

    public Set<PipelineOptionsProperty> getMissingProperties() {
      return missingProperties;
    }

    public Map<String, Set<PipelineOptionsProperty>> getMissingGroups() {
      return missingGroups;
    }

    public boolean isEmpty() {
      return missingProperties.isEmpty() && missingGroups.isEmpty();
    }
  }

  public Map<PipelineOptionsType, Set<PipelineOptionsProperty>> getOptionsHierarchy(
      PipelineOptionsHierarchy hierarchy) {
    String userOptionsName = getUserOptionsName();
    if (userOptionsName != null) {
      return hierarchy.getOptionsHierarchy(getRunner().getOptionsClass(), userOptionsName);
    } else {
      return hierarchy.getOptionsHierarchy(getRunner().getOptionsClass());
    }
  }
}
