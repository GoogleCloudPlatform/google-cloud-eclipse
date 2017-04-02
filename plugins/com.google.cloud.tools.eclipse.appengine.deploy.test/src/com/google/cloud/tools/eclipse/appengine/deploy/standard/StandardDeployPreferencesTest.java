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

import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.junit.Rule;
import org.junit.Test;

public class StandardDeployPreferencesTest {

  @Rule public final TestProjectCreator projectCreator = new TestProjectCreator();
  private final StandardDeployPreferences defaultPreferences =
      StandardDeployPreferences.getDefaultPreferences();

  @Test
  public void testDefaultProjectId() {
    assertThat(defaultPreferences.getProjectId(), isEmptyString());
  }

  @Test
  public void testSetProjectId() {
    IProject project = projectCreator.getProject();
    StandardDeployPreferences preferences = new StandardDeployPreferences(project);
    assertThat(preferences.getProjectId(), isEmptyString());
    preferences.setProjectId("someproject32");
    assertEquals("someproject32", preferences.getProjectId());
    preferences.setProjectId(null);
    assertThat(preferences.getProjectId(), isEmptyString());
  }

  @Test
  public void testDefaultVersion() {
    assertThat(defaultPreferences.getVersion(), isEmptyString());
  }

  @Test
  public void testDefaultAutoPromote() {
    assertTrue(defaultPreferences.isAutoPromote());
  }

  @Test
  public void testDefaultBucket() {
    assertThat(defaultPreferences.getBucket(), isEmptyString());
  }

  @Test
  public void testDefaultStopPreviousVersion() {
    assertTrue(defaultPreferences.isStopPreviousVersion());
  }

  @Test
  public void testIncludeOptionalConfigurationFiles() {
    assertTrue(defaultPreferences.isIncludeOptionalConfigurationFiles());
  }

}
