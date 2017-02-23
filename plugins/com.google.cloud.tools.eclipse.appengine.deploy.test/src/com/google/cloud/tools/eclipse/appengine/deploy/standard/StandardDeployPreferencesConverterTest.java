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

package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.api.deploy.DefaultDeployConfiguration;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StandardDeployPreferencesConverterTest {

  @Mock private IEclipsePreferences preferences;

  @Test
  public void testToDeployConfiguration_projectId() {
    when(preferences.get(eq(StandardDeployPreferences.PREF_PROJECT_ID), anyString()))
        .thenReturn("projectid");
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertThat(configuration.getProject(), is("projectid"));
  }

  @Test
  public void testToDeployConfiguration_bucketNameIsNull() {
    when(preferences.get(eq(StandardDeployPreferences.PREF_CUSTOM_BUCKET), anyString()))
        .thenReturn(null);
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertNull(configuration.getBucket());
  }

  @Test
  public void testToDeployConfiguration_bucketNameIsEmpty() {
    when(preferences.get(eq(StandardDeployPreferences.PREF_CUSTOM_BUCKET), anyString()))
        .thenReturn("");
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertNull(configuration.getBucket());
  }

  @Test
  public void testToDeployConfiguration_bucketNameContainsProtocol() {
    when(preferences.get(eq(StandardDeployPreferences.PREF_CUSTOM_BUCKET), anyString()))
        .thenReturn("gs://bucket");
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertThat(configuration.getBucket(), is("gs://bucket"));
  }

  @Test
  public void testToDeployConfiguration_bucketNameDoesNotContainProtocol() {
    when(preferences.get(eq(StandardDeployPreferences.PREF_CUSTOM_BUCKET), anyString()))
        .thenReturn("bucket");
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertThat(configuration.getBucket(), is("gs://bucket"));
  }

  @Test
  public void testToDeployConfiguration_promote() {
    when(preferences.getBoolean(eq(StandardDeployPreferences.PREF_ENABLE_AUTO_PROMOTE),
        anyBoolean())).thenReturn(true);
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertTrue(configuration.getPromote());
  }

  @Test
  public void testToDeployConfiguration_promoteNotSetStopPreviousVersionIsUnset() {
    when(preferences.getBoolean(eq(StandardDeployPreferences.PREF_STOP_PREVIOUS_VERSION),
        anyBoolean())).thenReturn(true);
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertNull(configuration.getStopPreviousVersion());
  }

  @Test
  public void testToDeployConfiguration_promoteSetStopPreviousVersionIsSet() {
    when(preferences.getBoolean(eq(StandardDeployPreferences.PREF_ENABLE_AUTO_PROMOTE),
        anyBoolean())).thenReturn(true);
    when(preferences.getBoolean(eq(StandardDeployPreferences.PREF_STOP_PREVIOUS_VERSION),
        anyBoolean())).thenReturn(true);
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertTrue(configuration.getStopPreviousVersion());
  }

  @Test
  public void testToDeployConfiguration_promoteSetStopPreviousVersionIsUnset() {
    when(preferences.getBoolean(eq(StandardDeployPreferences.PREF_ENABLE_AUTO_PROMOTE),
        anyBoolean())).thenReturn(true);
    when(preferences.getBoolean(eq(StandardDeployPreferences.PREF_STOP_PREVIOUS_VERSION),
        anyBoolean())).thenReturn(false);
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertFalse(configuration.getStopPreviousVersion());
  }

  @Test
  public void testToDeployConfiguration_version() {
    when(preferences.get(eq(StandardDeployPreferences.PREF_CUSTOM_VERSION), anyString()))
        .thenReturn("version");
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertThat(configuration.getVersion(), is("version"));
  }

  @Test
  public void testToDeployConfiguration_versionIsNull() {
    when(preferences.get(eq(StandardDeployPreferences.PREF_CUSTOM_VERSION), anyString()))
        .thenReturn(null);
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertNull(configuration.getVersion());
  }

  @Test
  public void testToDeployConfiguration_versionIsEmpty() {
    when(preferences.get(eq(StandardDeployPreferences.PREF_CUSTOM_VERSION), anyString()))
        .thenReturn("");
    DefaultDeployConfiguration configuration =
        new StandardDeployPreferencesConverter(new StandardDeployPreferences(preferences))
            .toDeployConfiguration();
    assertNull(configuration.getVersion());
  }
}
