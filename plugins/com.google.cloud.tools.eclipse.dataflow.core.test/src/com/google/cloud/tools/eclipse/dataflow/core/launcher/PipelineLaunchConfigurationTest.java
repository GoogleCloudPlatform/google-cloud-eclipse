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
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineLaunchConfiguration.MissingRequiredProperties;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsProperty;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsType;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link PipelineLaunchConfiguration}.
 */
public class PipelineLaunchConfigurationTest {
  private final MajorVersion majorVersion = MajorVersion.ONE;

  @Test
  public void testCreateDefaultCreatesWithDefaultValues() {
    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    assertTrue(pipelineLaunchConfig.getArgumentValues().isEmpty());
    assertEquals(PipelineLaunchConfiguration.defaultRunner(majorVersion),
        pipelineLaunchConfig.getRunner());
  }

  @Test
  public void testFromLaunchConfigurationCopiesArgumentsFromLaunchConfiguration()
      throws CoreException {
    ILaunchConfiguration launchConfiguration = mock(ILaunchConfiguration.class);
    Map<String, String> requiredArgumentValues =
        ImmutableMap.of(
            "Spam", "foo", //$NON-NLS-1$ //$NON-NLS-2$
            "Ham", "bar", //$NON-NLS-1$ //$NON-NLS-2$
            "Eggs", "baz"); //$NON-NLS-1$ //$NON-NLS-2$
    when(launchConfiguration.getAttribute(
        eq(PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString()),
        anyMapOf(String.class, String.class))).thenReturn(requiredArgumentValues);

    // set a different runner from the default
    PipelineRunner runner = PipelineRunner.DATAFLOW_PIPELINE_RUNNER;
    when(launchConfiguration.getAttribute(eq(PipelineConfigurationAttr.RUNNER_ARGUMENT.toString()),
        anyString())).thenReturn(runner.getRunnerName());

    PipelineLaunchConfiguration pipelineLaunchConfig =
        PipelineLaunchConfiguration.fromLaunchConfiguration(majorVersion, launchConfiguration);

    assertEquals(requiredArgumentValues, pipelineLaunchConfig.getArgumentValues());
    assertEquals(runner, pipelineLaunchConfig.getRunner());
  }

  @Test
  public void testToLaunchConfigurationWritesArgumentsToLaunchConfiguration() {
    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    final PipelineRunner runner = PipelineRunner.DATAFLOW_PIPELINE_RUNNER;
    pipelineLaunchConfig.setRunner(runner);

    Map<String, String> argValues = ImmutableMap.of(
        "Spam", "foo",
        "Ham", "bar",
        "Eggs", "baz");
    pipelineLaunchConfig.setArgumentValues(argValues);

    ILaunchConfigurationWorkingCopy workingCopy = mock(ILaunchConfigurationWorkingCopy.class);
    pipelineLaunchConfig.toLaunchConfiguration(workingCopy);

    verify(workingCopy).setAttribute(
        PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(), argValues);
    verify(workingCopy).setAttribute(
        PipelineConfigurationAttr.RUNNER_ARGUMENT.toString(), runner.getRunnerName());
  }

  /**
   * With a property that can be defaulted and default properties enabled, but no default value set
   * for the property, the property is missing.
   */
  @Test
  public void testGetMissingRequiredPropertiesWithDefaultPropertyNotSet() {
    String propertyName = DataflowPreferences.PROJECT_PROPERTY;
    PipelineOptionsProperty property = new PipelineOptionsProperty(
        propertyName, false, true, Collections.<String>emptySet(), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(true);
    pipelineLaunchConfig.setUserOptionsName("myType");

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().contains(property));
  }

  /**
   * With a property that can be defaulted and default properties disabled, and no custom value set
   * for the property, the property is missing.
   */
  @Test
  public void testGetMissingRequiredPropertiesDefaultableNotUsingDefault() {
    PipelineOptionsProperty property = new PipelineOptionsProperty(
        DataflowPreferences.PROJECT_PROPERTY, false, true, Collections.<String>emptySet(), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(false);
    pipelineLaunchConfig.setUserOptionsName("myType");

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().contains(property));
  }

  /**
   * With a property that can be defaulted and default properties enabled, with a default value set
   * for the property, the property is present.
   */
  @Test
  public void testGetMissingRequiredPropertiesWithDefaultPropertiesAndDefault() {
    PipelineOptionsProperty property = new PipelineOptionsProperty(
        DataflowPreferences.PROJECT_PROPERTY, false, true, Collections.<String>emptySet(), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(true);
    pipelineLaunchConfig.setUserOptionsName("myType");

    DataflowPreferences prefs = mapPrefs(
        ImmutableMap.of(DataflowPreferences.PROJECT_PROPERTY, "bar"));

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().isEmpty());
  }

  /**
   * With a property that can be defaulted and default properties disabled, with a custom value set
   * for the property, the property is present.
   */
  @Test
  public void testGetMissingRequiredPropertiesWithoutDefaultPropertiesAndCustomValue() {
    PipelineOptionsProperty property = new PipelineOptionsProperty(
        DataflowPreferences.PROJECT_PROPERTY, false, true, Collections.<String>emptySet(), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setArgumentValues(
        ImmutableMap.of(DataflowPreferences.PROJECT_PROPERTY, "bar"));
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setUseDefaultLaunchOptions(false);

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().isEmpty());
  }


  /**
   * With a property that cannot be defaulted, with default properties enabled and with no custom
   * value, the property is missing.
   */
  @Test
  public void testGetMissingRequiredPropertiesWithUndefaultableUseDefaultsAndAbsentCustom() {
    PipelineOptionsProperty property =
        new PipelineOptionsProperty("foo", false, true, Collections.<String>emptySet(), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(true);
    pipelineLaunchConfig.setUserOptionsName("myType");

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().contains(property));
  }

  /**
   * With a property that cannot be defaulted, with default properties disabled, and with no custom
   * value, the property is missing.
   */
  @Test
  public void testGetMissingRequiredPropertiesWithNoDefaultAndAbsentCustom() {
    PipelineOptionsProperty property =
        new PipelineOptionsProperty("foo", false, true, Collections.<String>emptySet(), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(false);
    pipelineLaunchConfig.setUserOptionsName("myType");

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().contains(property));
  }

  /**
   * With a property that cannot be defaulted, with default properties enabled and with a custom
   * value, the property is not missing.
   */
  @Test
  public void testGetMissingRequiredPropertiesWithUseDefaultEnabledDefaultAndPresentCustom() {
    PipelineOptionsProperty property =
        new PipelineOptionsProperty("foo", false, true, Collections.<String>emptySet(), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(true);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setArgumentValues(ImmutableMap.of("foo", "bar"));

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().isEmpty());
  }

  /**
   * With a property that cannot be defaulted, with default properties disabled and with a custom
   * value, the property is not missing.
   */
  @Test
  public void testGetMissingRequiredPropertiesWithNotDefaultDisabledDefaultAndPresentCustom() {
    PipelineOptionsProperty property =
        new PipelineOptionsProperty("foo", false, true, Collections.<String>emptySet(), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(false);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setArgumentValues(ImmutableMap.of("foo", "bar"));

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().isEmpty());
  }

  /**
   * With a property that can be defaulted and default properties enabled, but no default value set
   * for the property, the group is missing.
   */
  @Test
  public void testGetMissingRequiredGroupsWithDefaultPropertyNotSet() {
    String propertyName = DataflowPreferences.PROJECT_PROPERTY;
    String groupName = "myGroup";
    PipelineOptionsProperty property =
        new PipelineOptionsProperty(propertyName, false, true, ImmutableSet.of(groupName), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setUseDefaultLaunchOptions(true);

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingGroups().containsKey(groupName));
    assertEquals(ImmutableSet.of(property), missingProperties.getMissingGroups().get(groupName));
  }

  /**
   * With a property that can be defaulted and default properties disabled, and no custom value set
   * for the property, the group is missing.
   */
  @Test
  public void testGetMissingRequiredGroupsDefaultableNotUsingDefault() {
    String propertyName = DataflowPreferences.PROJECT_PROPERTY;
    String groupName = "myGroup";
    PipelineOptionsProperty property =
        new PipelineOptionsProperty(propertyName, false, true, ImmutableSet.of(groupName), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setUseDefaultLaunchOptions(false);

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingGroups().containsKey(groupName));
    assertEquals(ImmutableSet.of(property), missingProperties.getMissingGroups().get(groupName));
  }

  /**
   * With a property that can be defaulted and default properties enabled, with a default value set
   * for the property, the group is present.
   */
  @Test
  public void testGetMissingRequiredGroupsWithDefaultPropertiesUsingDefault() {
    String propertyName = DataflowPreferences.PROJECT_PROPERTY;
    String groupName = "myGroup";
    PipelineOptionsProperty property =
        new PipelineOptionsProperty(propertyName, false, true, ImmutableSet.of(groupName), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setUseDefaultLaunchOptions(true);

    DataflowPreferences prefs = mapPrefs(
        ImmutableMap.of(DataflowPreferences.PROJECT_PROPERTY, "bar"));

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingGroups().isEmpty());
  }

  /**
   * With a property that can be defaulted and default properties disabled, with a custom value set
   * for the property, the group is present.
   */
  @Test
  public void testGetMissingRequiredGroupsWithoutDefaultPropertiesAndCustomValue() {
    String propertyName = DataflowPreferences.PROJECT_PROPERTY;
    String groupName = "myGroup";
    PipelineOptionsProperty property =
        new PipelineOptionsProperty(propertyName, false, true, ImmutableSet.of(groupName), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(false);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setArgumentValues(
        ImmutableMap.of(DataflowPreferences.PROJECT_PROPERTY, "bar"));

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingGroups().isEmpty());
  }

  /**
   * With a property that cannot be defaulted, with default properties enabled and with no custom
   * value, the group is missing.
   */
  @Test
  public void testGetMissingRequiredGroupsWithUndefaultableUseDefaultsAndAbsentCustom() {
    String propertyName = "foo";
    String groupName = "myGroup";
    PipelineOptionsProperty property =
        new PipelineOptionsProperty(propertyName, false, true, ImmutableSet.of(groupName), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setUseDefaultLaunchOptions(true);

    DataflowPreferences prefs = absentPrefs();
    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingGroups().containsKey(groupName));
    assertEquals(ImmutableSet.of(property), missingProperties.getMissingGroups().get(groupName));
  }

  /**
   * With a property that cannot be defaulted, with default properties disabled, and with no custom
   * value, the group is missing.
   */
  @Test
  public void testGetMissingRequiredGroupsWithNoDefaultAndAbsentCustom() {
    String propertyName = "foo";
    String groupName = "myGroup";
    PipelineOptionsProperty property =
        new PipelineOptionsProperty(propertyName, false, true, ImmutableSet.of(groupName), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setUseDefaultLaunchOptions(false);

    DataflowPreferences prefs = absentPrefs();
    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingGroups().containsKey(groupName));
    assertEquals(ImmutableSet.of(property), missingProperties.getMissingGroups().get(groupName));
  }

  /**
   * With a property that cannot be defaulted, with default properties enabled and with a custom
   * value, the group is not missing.
   */
  @Test
  public void testGetMissingRequiredGroupsWithUseDefaultEnabledDefaultAndPresentCustom() {
    String propertyName = "foo";
    String groupName = "myGroup";
    PipelineOptionsProperty property =
        new PipelineOptionsProperty(propertyName, false, true, ImmutableSet.of(groupName), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(true);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setArgumentValues(ImmutableMap.of("foo", "bar"));

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, absentPrefs());

    assertTrue(missingProperties.getMissingGroups().isEmpty());
  }

  /**
   * With a property that cannot be defaulted, with default properties disabled and with a custom
   * value, the group is not missing.
   */
  @Test
  public void testGetMissingRequiredGroupWithNotDefaultDisabledDefaultAndPresentCustom() {
    PipelineOptionsProperty property =
        new PipelineOptionsProperty("foo", false, true, Collections.<String>emptySet(), null);
    PipelineOptionsHierarchy opts = options(property);

    PipelineLaunchConfiguration pipelineLaunchConfig =
        new PipelineLaunchConfiguration(majorVersion);
    pipelineLaunchConfig.setUseDefaultLaunchOptions(false);
    pipelineLaunchConfig.setUserOptionsName("myType");
    pipelineLaunchConfig.setArgumentValues(ImmutableMap.of("foo", "bar"));

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties =
        pipelineLaunchConfig.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().isEmpty());
  }

  private static PipelineOptionsHierarchy options(PipelineOptionsProperty property) {
    PipelineOptionsHierarchy hierarchy = mock(PipelineOptionsHierarchy.class);
    PipelineOptionsType type = new PipelineOptionsType(
        "myType", Collections.<PipelineOptionsType>emptySet(), ImmutableSet.of(property));

    Map<PipelineOptionsType, Set<PipelineOptionsProperty>> requiredOptions =
        ImmutableMap.of(type, (Set<PipelineOptionsProperty>) ImmutableSet.of(property));
    when(hierarchy.getRequiredOptionsByType(Mockito.<String>any(), Mockito.eq("myType")))
        .thenReturn(requiredOptions);
    return hierarchy;
  }

  private static DataflowPreferences absentPrefs() {
    return mapPrefs(Collections.<String, String>emptyMap());
  }

  private static DataflowPreferences mapPrefs(Map<String, String> asMap) {
    DataflowPreferences preferences = mock(DataflowPreferences.class);
    when(preferences.asDefaultPropertyMap()).thenReturn(asMap);
    return preferences;
  }
}
