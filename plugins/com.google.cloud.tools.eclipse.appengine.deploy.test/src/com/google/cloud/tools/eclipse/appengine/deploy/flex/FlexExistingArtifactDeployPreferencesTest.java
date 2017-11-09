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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class FlexExistingArtifactDeployPreferencesTest {

  private FlexExistingArtifactDeployPreferences preferences;
  private IEclipsePreferences preferenceStore;

  @Before
  public void setUp() {
    preferences = new FlexExistingArtifactDeployPreferences();
    preferenceStore =
        InstanceScope.INSTANCE.getNode("com.google.cloud.tools.eclipse.appengine.deploy");
  }

  @After
  public void tearDown() throws BackingStoreException {
    preferenceStore.clear();
  }

  @Test
  public void testDefaultAppYamlPath() {
    assertEquals(preferences.getAppYamlPath(), "app.yaml");
  }

  @Test
  public void testSetAppYamlPath() {
    preferences.setAppYamlPath("project/some/directory/app.yaml");
    assertEquals("project/some/directory/app.yaml", preferences.getAppYamlPath());
    preferences.setAppYamlPath(null);
    assertEquals("", preferences.getAppYamlPath());
  }

  @Test
  public void testSetDeployArtifactPath() {
    preferences.setDeployArtifactPath("project/target/app.war");
    assertEquals("project/target/app.war", preferences.getDeployArtifactPath());
    preferences.setDeployArtifactPath(null);
    assertEquals("", preferences.getDeployArtifactPath());
  }

  @Test
  public void testResetToDefault() {
    preferences.setAppYamlPath("project/some/directory/app.yaml");
    preferences.resetToDefaults();
    assertEquals("app.yaml", preferences.getAppYamlPath());
  }

  @Test
  public void testDoesNotPersistWithoutSave() {
    assertEquals("", preferenceStore.get("app.yaml.path", ""));
    preferences.setAppYamlPath("project/some/directory/app.yaml");
    preferences.setDeployArtifactPath("project/target/app.war");
    assertEquals("", preferenceStore.get("app.yaml.path", ""));
    assertEquals("", preferenceStore.get("deploy.artifact.path", ""));
  }

  @Test
  public void testSave() throws BackingStoreException {
    assertEquals("", preferenceStore.get("app.yaml.path", ""));
    assertEquals("", preferenceStore.get("deploy.artifact.path", ""));
    preferences.setAppYamlPath("project/some/directory/app.yaml");
    preferences.setDeployArtifactPath("project/target/app.war");
    preferences.save();
    assertEquals("project/some/directory/app.yaml", preferenceStore.get("app.yaml.path", ""));
    assertEquals("project/target/app.war", preferenceStore.get("deploy.artifact.path", ""));
  }
}
