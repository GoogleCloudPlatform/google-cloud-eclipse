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

import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineLaunchConfiguration.MissingRequiredProperties;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsProperty;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsType;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link PipelineLaunchConfiguration}.
 */
public class PipelineLaunchConfigurationTest {
  private MajorVersion majorVersion = MajorVersion.ONE;

  @Test
  public void testCreateDefaultCreatesWithDefaultValues() {
    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    assertTrue(lc.getArgumentValues().isEmpty());
    assertEquals(PipelineLaunchConfiguration.defaultRunner(majorVersion), lc.getRunner());
  }

  @Test
  public void testFromLaunchConfigurationCopiesArgumentsFromLaunchConfiguration()
      throws CoreException {
    ILaunchConfiguration launchConfiguration = mock(ILaunchConfiguration.class);
    Map<String, String> requiredArgumentValues =
        ImmutableMap.<String, String>builder()
            .put("Spam", "foo")
            .put("Ham", "bar")
            .put("Eggs", "baz")
            .build();
    when(
        launchConfiguration.getAttribute(
            PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(),
            Collections.<String, String>emptyMap())).thenReturn(requiredArgumentValues);

    PipelineRunner runner = PipelineRunner.DATAFLOW_PIPELINE_RUNNER;
    when(
        launchConfiguration.getAttribute(
            PipelineConfigurationAttr.RUNNER_ARGUMENT.toString(),
            PipelineLaunchConfiguration.defaultRunner(majorVersion).getRunnerName()))
        .thenReturn(runner.getRunnerName());

    PipelineLaunchConfiguration lc =
        PipelineLaunchConfiguration.fromLaunchConfiguration(launchConfiguration);

    assertEquals(requiredArgumentValues, lc.getArgumentValues());
    assertEquals(runner, lc.getRunner());
  }

  @Test
  public void testToLaunchConfigurationWritesArgumentsToLaunchConfiguration() {
    final PipelineRunner runner = PipelineRunner.DATAFLOW_PIPELINE_RUNNER;
    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();

    Map<String, String> argValues =
        ImmutableMap.<String, String>builder()
            .put("Spam", "foo")
            .put("Ham", "bar")
            .put("Eggs", "baz")
            .build();
    lc.setArgumentValues(argValues);

    lc.setRunner(runner);

    ILaunchConfigurationWorkingCopy wc = mock(ILaunchConfigurationWorkingCopy.class);

    lc.toLaunchConfiguration(wc);

    verify(wc).setAttribute(PipelineConfigurationAttr.ALL_ARGUMENT_VALUES.toString(), argValues);
    verify(wc).setAttribute(
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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(true);
    lc.setUserOptionsName("myType");

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(false);
    lc.setUserOptionsName("myType");

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(true);
    lc.setUserOptionsName("myType");

    DataflowPreferences prefs = mapPrefs(
        ImmutableMap.<String, String>builder()
            .put(DataflowPreferences.PROJECT_PROPERTY, "bar")
            .build());

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setArgumentValues(
        ImmutableMap.<String, String>builder()
            .put(DataflowPreferences.PROJECT_PROPERTY, "bar")
            .build());
    lc.setUserOptionsName("myType");
    lc.setUseDefaultLaunchOptions(false);

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(true);
    lc.setUserOptionsName("myType");

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(false);
    lc.setUserOptionsName("myType");

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(true);
    lc.setUserOptionsName("myType");
    lc.setArgumentValues(ImmutableMap.<String, String>builder().put("foo", "bar").build());

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(false);
    lc.setUserOptionsName("myType");
    lc.setArgumentValues(ImmutableMap.<String, String>builder().put("foo", "bar").build());

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUserOptionsName("myType");
    lc.setUseDefaultLaunchOptions(true);

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUserOptionsName("myType");
    lc.setUseDefaultLaunchOptions(false);

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUserOptionsName("myType");
    lc.setUseDefaultLaunchOptions(true);

    DataflowPreferences prefs = mapPrefs(
        ImmutableMap.<String, String>builder()
            .put(DataflowPreferences.PROJECT_PROPERTY, "bar")
            .build());

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(false);
    lc.setUserOptionsName("myType");
    lc.setArgumentValues(
        ImmutableMap.<String, String>builder()
            .put(DataflowPreferences.PROJECT_PROPERTY, "bar")
            .build());

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUserOptionsName("myType");
    lc.setUseDefaultLaunchOptions(true);

    DataflowPreferences prefs = absentPrefs();
    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUserOptionsName("myType");
    lc.setUseDefaultLaunchOptions(false);

    DataflowPreferences prefs = absentPrefs();
    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(true);
    lc.setUserOptionsName("myType");
    lc.setArgumentValues(ImmutableMap.<String, String>builder().put("foo", "bar").build());

    MissingRequiredProperties missingProperties =
        lc.getMissingRequiredProperties(opts, absentPrefs());

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

    PipelineLaunchConfiguration lc = PipelineLaunchConfiguration.createDefault();
    lc.setUseDefaultLaunchOptions(false);
    lc.setUserOptionsName("myType");
    lc.setArgumentValues(ImmutableMap.<String, String>builder().put("foo", "bar").build());

    DataflowPreferences prefs = absentPrefs();

    MissingRequiredProperties missingProperties = lc.getMissingRequiredProperties(opts, prefs);

    assertTrue(missingProperties.getMissingProperties().isEmpty());
  }

  private PipelineOptionsHierarchy options(PipelineOptionsProperty property) {
    PipelineOptionsHierarchy hierarchy = mock(PipelineOptionsHierarchy.class);
    PipelineOptionsType type = new PipelineOptionsType(
        "myType", Collections.<PipelineOptionsType>emptySet(), ImmutableSet.of(property));

    Map<PipelineOptionsType, Set<PipelineOptionsProperty>> requiredOptions =
        ImmutableMap.<PipelineOptionsType, Set<PipelineOptionsProperty>>builder()
            .put(type, ImmutableSet.of(property))
            .build();
    when(hierarchy.getRequiredOptionsByType(Mockito.<String>any(), Mockito.eq("myType")))
        .thenReturn(requiredOptions);
    return hierarchy;
  }

  private DataflowPreferences absentPrefs() {
    return new DataflowPreferences() {
      @Override
      public String getDefaultStagingLocation() {
        return null;
      }

      @Override
      public String getDefaultGcpTempLocation() {
        return null;
      }

      @Override
      public String getDefaultProject() {
        return null;
      }

      @Override
      public Map<String, String> asDefaultPropertyMap() {
        return Collections.emptyMap();
      }
    };
  }

  private DataflowPreferences mapPrefs(final Map<String, String> asMap) {
    return new DataflowPreferences() {
      @Override
      public String getDefaultStagingLocation() {
        return null;
      }

      @Override
      public String getDefaultGcpTempLocation() {
        return null;
      }

      @Override
      public String getDefaultProject() {
        return null;
      }

      @Override
      public Map<String, String> asDefaultPropertyMap() {
        return asMap;
      }
    };
  }
}
