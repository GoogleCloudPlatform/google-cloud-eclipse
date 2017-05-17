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
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class DeployPreferencesTest {

  private DeployPreferences preferences;
  private IEclipsePreferences preferenceStore;

  @Before
  public void setUp() {
    IProject project = mock(IProject.class);
    when(project.getName()).thenReturn("");
    preferences = new DeployPreferences(project);
    preferenceStore =
        new ProjectScope(project).getNode("com.google.cloud.tools.eclipse.appengine.deploy");
  }

  @After
  public void tearDown() throws BackingStoreException {
    preferenceStore.removeNode();
  }

  @Test
  public void testDefaultProjectId() {
    assertThat(DeployPreferences.DEFAULT_PROJECT_ID, isEmptyString());
    assertThat(preferences.getProjectId(), isEmptyString());
  }

  @Test
  public void testSetProjectId() {
    assertThat(preferences.getProjectId(), isEmptyString());
    preferences.setProjectId("someproject32");
    assertThat(preferences.getProjectId(), is("someproject32"));
    preferences.setProjectId(null);
    assertThat(preferences.getProjectId(), isEmptyString());
  }

  @Test
  public void testDefaultAccountEmail() {
    assertThat(DeployPreferences.DEFAULT_ACCOUNT_EMAIL, isEmptyString());
    assertThat(preferences.getAccountEmail(), isEmptyString());
  }

  @Test
  public void testSetAccountEmail() {
    assertThat(preferences.getAccountEmail(), isEmptyString());
    preferences.setAccountEmail("someemail72");
    assertThat(preferences.getAccountEmail(), is("someemail72"));
    preferences.setAccountEmail(null);
    assertThat(preferences.getAccountEmail(), isEmptyString());
  }

  @Test
  public void testDefaultVersion() {
    assertThat(DeployPreferences.DEFAULT_CUSTOM_VERSION, isEmptyString());
    assertThat(preferences.getVersion(), isEmptyString());
  }

  @Test
  public void testSetVersion() {
    assertThat(preferences.getVersion(), isEmptyString());
    preferences.setVersion("someversion97");
    assertThat(preferences.getVersion(), is("someversion97"));
    preferences.setVersion(null);
    assertThat(preferences.getVersion(), isEmptyString());
  }

  @Test
  public void testDefaultAutoPromote() {
    assertTrue(DeployPreferences.DEFAULT_ENABLE_AUTO_PROMOTE);
    assertTrue(preferences.isAutoPromote());
  }

  @Test
  public void testSetAutoPromote() {
    assertTrue(preferences.isAutoPromote());
    preferences.setAutoPromote(false);
    assertFalse(preferences.isAutoPromote());
    preferences.setAutoPromote(true);
    assertTrue(preferences.isAutoPromote());
  }

  @Test
  public void testDefaultBucket() {
    assertThat(DeployPreferences.DEFAULT_CUSTOM_BUCKET, isEmptyString());
    assertThat(preferences.getBucket(), isEmptyString());
  }

  @Test
  public void testSetBucket() {
    assertThat(preferences.getBucket(), isEmptyString());
    preferences.setBucket("somebucket45");
    assertThat(preferences.getBucket(), is("somebucket45"));
    preferences.setBucket(null);
    assertThat(preferences.getBucket(), isEmptyString());
  }

  @Test
  public void testDefaultStopPreviousVersion() {
    assertTrue(DeployPreferences.DEFAULT_STOP_PREVIOUS_VERSION);
    assertTrue(preferences.isStopPreviousVersion());
  }

  @Test
  public void testSetStopPreviousVersion() {
    assertTrue(preferences.isStopPreviousVersion());
    preferences.setStopPreviousVersion(false);
    assertFalse(preferences.isStopPreviousVersion());
    preferences.setStopPreviousVersion(true);
    assertTrue(preferences.isStopPreviousVersion());
  }

  @Test
  public void testDefaultIncludeOptionalConfigurationFiles() {
    assertTrue(DeployPreferences.DEFAULT_INCLUDE_OPTIONAL_CONFIGURATION_FILES);
    assertTrue(preferences.isIncludeOptionalConfigurationFiles());
  }

  @Test
  public void testSetIncludeOptionalConfigurationFiles() {
    assertTrue(preferences.isIncludeOptionalConfigurationFiles());
    preferences.setIncludeOptionalConfigurationFiles(false);
    assertFalse(preferences.isIncludeOptionalConfigurationFiles());
    preferences.setIncludeOptionalConfigurationFiles(true);
    assertTrue(preferences.isIncludeOptionalConfigurationFiles());
  }

  @Test
  public void testResetToDefault() {
    setAllFieldsWithExamples();
    preferences.resetToDefaults();

    assertThat(preferences.getProjectId(), isEmptyString());
    assertThat(preferences.getAccountEmail(), isEmptyString());
    assertThat(preferences.getVersion(), isEmptyString());
    assertTrue(preferences.isAutoPromote());
    assertThat(preferences.getBucket(), isEmptyString());
    assertTrue(preferences.isStopPreviousVersion());
    assertTrue(preferences.isIncludeOptionalConfigurationFiles());
  }

  @Test
  public void testDoesNotPersistWithoutSave() {
    verifyEmptyPreferenceStore();
    setAllFieldsWithExamples();
    verifyEmptyPreferenceStore();
  }

  @Test
  public void testSave() throws BackingStoreException {
    verifyEmptyPreferenceStore();
    setAllFieldsWithExamples();
    preferences.save();

    assertThat(preferenceStore.get(DeployPreferences.PREF_PROJECT_ID, ""), is("someproject32"));
    assertThat(preferenceStore.get(DeployPreferences.PREF_ACCOUNT_EMAIL, ""), is("someemail72"));
    assertThat(preferenceStore.get(DeployPreferences.PREF_CUSTOM_VERSION, ""), is("someversion97"));
    assertFalse(preferenceStore.getBoolean(DeployPreferences.PREF_ENABLE_AUTO_PROMOTE, true));
    assertThat(preferenceStore.get(DeployPreferences.PREF_CUSTOM_BUCKET, ""), is("somebucket45"));
    assertFalse(preferenceStore.getBoolean(DeployPreferences.PREF_STOP_PREVIOUS_VERSION, true));
    assertFalse(preferenceStore.getBoolean(
        DeployPreferences.PREF_INCLUDE_OPTIONAL_CONFIGURATION_FILES, true));
  }

  private void setAllFieldsWithExamples() {
    preferences.setProjectId("someproject32");
    preferences.setAccountEmail("someemail72");
    preferences.setVersion("someversion97");
    preferences.setAutoPromote(false);
    preferences.setBucket("somebucket45");
    preferences.setStopPreviousVersion(false);
    preferences.setIncludeOptionalConfigurationFiles(false);
  }

  private void verifyEmptyPreferenceStore() {
    assertThat(preferenceStore.get(DeployPreferences.PREF_PROJECT_ID, ""), isEmptyString());
    assertThat(preferenceStore.get(DeployPreferences.PREF_ACCOUNT_EMAIL, ""), isEmptyString());
    assertThat(preferenceStore.get(DeployPreferences.PREF_CUSTOM_VERSION, ""), isEmptyString());
    assertTrue(preferenceStore.getBoolean(DeployPreferences.PREF_ENABLE_AUTO_PROMOTE, true));
    assertThat(preferenceStore.get(DeployPreferences.PREF_CUSTOM_BUCKET, ""), isEmptyString());
    assertTrue(preferenceStore.getBoolean(DeployPreferences.PREF_STOP_PREVIOUS_VERSION, true));
    assertTrue(preferenceStore.getBoolean(
        DeployPreferences.PREF_INCLUDE_OPTIONAL_CONFIGURATION_FILES, true));
  }
}
