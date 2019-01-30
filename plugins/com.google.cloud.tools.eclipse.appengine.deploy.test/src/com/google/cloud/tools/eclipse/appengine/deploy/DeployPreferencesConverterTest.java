/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.configuration.DeployConfiguration;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeployPreferencesConverterTest {

  @Mock private IEclipsePreferences preferences;
  private final List<Path> deployables = new ArrayList<>();
  
  @Before 
  public void setUp() {
    deployables.add(Paths.get("foo"));
  }

  @Test
  public void testToDeployConfiguration_projectId() {
    when(preferences.get(eq(DeployPreferences.PREF_PROJECT_ID), anyString()))
        .thenReturn("projectid");
    DeployConfiguration configuration = DeployPreferencesConverter
        .toDeployConfiguration(new DeployPreferences(preferences), deployables);
    assertThat(configuration.getProjectId(), is("projectid"));
  }

  @Test
  public void testToDeployConfiguration_bucketNameIsNull() {
    when(preferences.get(eq(DeployPreferences.PREF_CUSTOM_BUCKET), anyString())).thenReturn(null);
    DeployConfiguration configuration =
        DeployPreferencesConverter.toDeployConfiguration(new DeployPreferences(preferences), deployables);
    assertNull(configuration.getBucket());
  }

  @Test
  public void testToDeployConfiguration_bucketNameIsEmpty() {
    when(preferences.get(eq(DeployPreferences.PREF_CUSTOM_BUCKET), anyString())).thenReturn("");
    DeployConfiguration configuration =
        DeployPreferencesConverter.toDeployConfiguration(new DeployPreferences(preferences), deployables);
    assertNull(configuration.getBucket());
  }

  @Test
  public void testToDeployConfiguration_bucketNameContainsProtocol() {
    when(preferences.get(eq(DeployPreferences.PREF_CUSTOM_BUCKET), anyString()))
        .thenReturn("gs://bucket");
    DeployConfiguration configuration =
        DeployPreferencesConverter.toDeployConfiguration(new DeployPreferences(preferences), deployables);
    assertThat(configuration.getBucket(), is("gs://bucket"));
  }

  @Test
  public void testToDeployConfiguration_bucketNameDoesNotContainProtocol() {
    when(preferences.get(eq(DeployPreferences.PREF_CUSTOM_BUCKET), anyString()))
        .thenReturn("bucket");
    DeployConfiguration configuration =
        DeployPreferencesConverter.toDeployConfiguration(new DeployPreferences(preferences), deployables);
    assertThat(configuration.getBucket(), is("gs://bucket"));
  }

  @Test
  public void testToDeployConfiguration_promote() {
    when(preferences.getBoolean(eq(DeployPreferences.PREF_ENABLE_AUTO_PROMOTE), anyBoolean()))
        .thenReturn(true);
    DeployConfiguration configuration =
        DeployPreferencesConverter.toDeployConfiguration(new DeployPreferences(preferences), deployables);
    Boolean promote = configuration.getPromote();
    assertNotNull(promote);
    assertTrue(promote);
  }

  @Test
  public void testToDeployConfiguration_promoteNotSetStopPreviousVersionIsUnset() {
    when(preferences.getBoolean(eq(DeployPreferences.PREF_STOP_PREVIOUS_VERSION), anyBoolean()))
        .thenReturn(true);
    DeployConfiguration configuration =
        DeployPreferencesConverter.toDeployConfiguration(new DeployPreferences(preferences), deployables);
    assertNull(configuration.getStopPreviousVersion());
  }

  @Test
  public void testToDeployConfiguration_promoteSetStopPreviousVersionIsSet() {
    when(preferences.getBoolean(eq(DeployPreferences.PREF_ENABLE_AUTO_PROMOTE), anyBoolean()))
        .thenReturn(true);
    when(preferences.getBoolean(eq(DeployPreferences.PREF_STOP_PREVIOUS_VERSION), anyBoolean()))
        .thenReturn(true);
    DeployConfiguration configuration = DeployPreferencesConverter
        .toDeployConfiguration(new DeployPreferences(preferences), deployables);
    Boolean stopPreviousVersion = configuration.getStopPreviousVersion();
    assertNotNull(stopPreviousVersion);
    assertTrue(stopPreviousVersion);
  }

  @Test
  public void testToDeployConfiguration_promoteSetStopPreviousVersionIsUnset() {
    when(preferences.getBoolean(eq(DeployPreferences.PREF_ENABLE_AUTO_PROMOTE), anyBoolean()))
        .thenReturn(true);
    when(preferences.getBoolean(eq(DeployPreferences.PREF_STOP_PREVIOUS_VERSION), anyBoolean()))
        .thenReturn(false);
    DeployConfiguration configuration = DeployPreferencesConverter
        .toDeployConfiguration(new DeployPreferences(preferences), deployables);
    Boolean stopPreviousVersion = configuration.getStopPreviousVersion();
    assertNotNull(stopPreviousVersion);
    assertFalse(stopPreviousVersion);
  }

  @Test
  public void testToDeployConfiguration_version() {
    when(preferences.get(eq(DeployPreferences.PREF_CUSTOM_VERSION), anyString()))
        .thenReturn("version");
    DeployConfiguration configuration = DeployPreferencesConverter
        .toDeployConfiguration(new DeployPreferences(preferences), deployables);
    assertThat(configuration.getVersion(), is("version"));
  }

  @Test
  public void testToDeployConfiguration_versionIsNull() {
    when(preferences.get(eq(DeployPreferences.PREF_CUSTOM_VERSION), anyString())).thenReturn(null);
    DeployConfiguration configuration = DeployPreferencesConverter
        .toDeployConfiguration(new DeployPreferences(preferences), deployables);
    assertNull(configuration.getVersion());
  }

  @Test
  public void testToDeployConfiguration_versionIsEmpty() {
    when(preferences.get(eq(DeployPreferences.PREF_CUSTOM_VERSION), anyString())).thenReturn("");
    DeployConfiguration configuration = DeployPreferencesConverter
        .toDeployConfiguration(new DeployPreferences(preferences), deployables);
    assertNull(configuration.getVersion());
  }
}
