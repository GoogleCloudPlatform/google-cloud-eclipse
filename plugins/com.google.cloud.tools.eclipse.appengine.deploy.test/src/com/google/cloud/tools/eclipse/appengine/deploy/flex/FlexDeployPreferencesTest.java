/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.deploy.flex;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class FlexDeployPreferencesTest {

  private FlexDeployPreferences preferences;
  private IEclipsePreferences preferenceStore;

  @Before
  public void setUp() {
    IProject project = mock(IProject.class);
    when(project.getName()).thenReturn("");
    preferences = new FlexDeployPreferences(project);
    preferenceStore =
        new ProjectScope(project).getNode("com.google.cloud.tools.eclipse.appengine.deploy");
  }

  @After
  public void tearDown() throws BackingStoreException {
    preferenceStore.removeNode();
  }

  @Test
  public void testDefaultAppEngineDirectory() {
    assertEquals("src/main/appengine", FlexDeployPreferences.DEFAULT_APP_ENGINE_DIRECTORY);
    assertEquals(preferences.getAppEngineDirectory(), "src/main/appengine");
  }

  @Test
  public void testSetAppEngineDirectory() {
    assertEquals(preferences.getAppEngineDirectory(), "src/main/appengine");
    preferences.setAppEngineDirectory("another/directory");
    assertEquals("another/directory", preferences.getAppEngineDirectory());
    preferences.setAppEngineDirectory(null);
    assertEquals("", preferences.getProjectId());
  }

  @Test
  public void testResetToDefault() {
    preferences.setAppEngineDirectory("another/directory");
    preferences.resetToDefaults();
    assertEquals("src/main/appengine", preferences.getAppEngineDirectory());
  }

  @Test
  public void testDoesNotPersistWithoutSave() {
    assertEquals("", preferenceStore.get(FlexDeployPreferences.PREF_APP_ENGINE_DIRECTORY, ""));
    preferences.setAppEngineDirectory("another/directory");
    assertEquals("", preferenceStore.get(FlexDeployPreferences.PREF_APP_ENGINE_DIRECTORY, ""));
  }

  @Test
  public void testSave() throws BackingStoreException {
    assertEquals("", preferenceStore.get(FlexDeployPreferences.PREF_APP_ENGINE_DIRECTORY, ""));
    preferences.setAppEngineDirectory("another/directory");
    preferences.save();
    assertEquals("another/directory",
        preferenceStore.get(FlexDeployPreferences.PREF_APP_ENGINE_DIRECTORY, ""));
  }
}
