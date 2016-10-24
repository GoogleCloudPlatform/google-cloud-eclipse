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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StandardDeployPreferencesTest {

  @Test
  public void testDefaultProjectId() {
    assertThat(StandardDeployPreferences.DEFAULT.getProjectId(), isEmptyString());
  }

  @Test
  public void testDefaultOverrideDefaultVersioning() {
    assertFalse(StandardDeployPreferences.DEFAULT.isOverrideDefaultVersioning());
  }

  @Test
  public void testDefaultVersion() {
    assertThat(StandardDeployPreferences.DEFAULT.getVersion(), isEmptyString());
  }

  @Test
  public void testDefaultAutoPromote() {
    assertTrue(StandardDeployPreferences.DEFAULT.isAutoPromote());
  }

  @Test
  public void testDefaultOverrideDefaultBucket() {
    assertFalse(StandardDeployPreferences.DEFAULT.isOverrideDefaultBucket());
  }

  @Test
  public void testDefaultBucket() {
    assertThat(StandardDeployPreferences.DEFAULT.getBucket(), isEmptyString());
  }

  @Test
  public void testDefaultStopPreviousVersion() {
    assertTrue(StandardDeployPreferences.DEFAULT.isStopPreviousVersion());
  }

}
