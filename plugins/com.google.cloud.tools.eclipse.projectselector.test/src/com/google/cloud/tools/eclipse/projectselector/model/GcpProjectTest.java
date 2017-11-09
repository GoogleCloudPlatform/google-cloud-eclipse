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

package com.google.cloud.tools.eclipse.projectselector.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GcpProjectTest {

  @Test
  public void testGetName() {
    assertThat(new GcpProject("projectName", "projectId").getName(), is("projectName"));
  }

  @Test
  public void testGetId() {
    assertThat(new GcpProject("projectName", "projectId").getId(), is("projectId"));
  }

  @Test
  public void testHashcodeForSameNameSameId() {
    assertThat(new GcpProject("name", "id").hashCode(), is(new GcpProject("name", "id").hashCode()));
  }

  @Test
  public void testHashcodeForSameNameDifferentId() {
    assertThat(new GcpProject("name", "id1").hashCode(),
               is(not(new GcpProject("name", "id2").hashCode())));
  }

  @Test
  public void testHashcodeForDifferentNameSameId() {
    assertThat(new GcpProject("name1", "id1").hashCode(),
               is(new GcpProject("name2", "id1").hashCode()));
  }

  @Test
  public void testHashcodeForDifferentNameDifferentId() {
    assertThat(new GcpProject("name1", "id1").hashCode(),
               is(not(new GcpProject("name2", "id2").hashCode())));
  }

  @Test
  public void testHashcodeForNullId() {
    assertThat(new GcpProject("name", null).hashCode(), is(0));
  }

  @Test
  public void testHashcodeForEmptyId() {
    assertThat(new GcpProject("name", "").hashCode(), is(0));
  }

  @Test
  public void testEqualsItself() {
    GcpProject gcpProject = new GcpProject("name", "id");
    assertTrue(gcpProject.equals(gcpProject));
  }

  @Test
  public void testEqualsWithNull() {
    assertFalse(new GcpProject("name", "id").equals(null));
  }

  @Test
  public void testEqualsWithOtherClass() {
    assertFalse(new GcpProject("name", "id").equals(new Object()));
  }

  @Test
  public void testEqualsForSameNameSameId() {
    assertTrue(new GcpProject("name", "id").equals(new GcpProject("name", "id")));
  }

  @Test
  public void testEqualsForSameNameDifferentId() {
    assertFalse(new GcpProject("name", "id1").equals(new GcpProject("name", "id2")));
  }

  @Test
  public void testEqualsForDifferentNameSameId() {
    assertTrue(new GcpProject("name1", "id1").equals(new GcpProject("name2", "id1")));
  }

  @Test
  public void testEqualsForDifferentNameDifferentId() {
    assertFalse(new GcpProject("name1", "id1").equals(new GcpProject("name2", "id2")));
  }

  @Test
  public void testEqualsForNullId() {
    assertTrue(new GcpProject("name", null).equals(new GcpProject("name", null)));
  }

  @Test
  public void testEqualsForNullIdAndNonNullId() {
    assertFalse(new GcpProject("name", null).equals(new GcpProject("name", "id")));
  }

  @Test
  public void testEqualsForEmptyId() {
    assertTrue(new GcpProject("name", "").equals(new GcpProject("name", "")));
  }

  @Test
  public void testEqualsForEmptyVsNullId() {
    assertFalse(new GcpProject("name", "").equals(new GcpProject("name", null)));
  }

  @Test
  public void testHasAppEngineInfo_appEngineNotSet() {
    assertFalse(new GcpProject("name", "id").hasAppEngineInfo());
  }

  @Test
  public void testHasAppEngineInfo_appEngineSetToExisting() {
    GcpProject gcpProject = new GcpProject("name", "id");
    gcpProject.setAppEngine(AppEngine.withId("id"));
    assertTrue(gcpProject.hasAppEngineInfo());
  }

  @Test
  public void testHasAppEngineInfo_appEngineSetToNonExistent() {
    GcpProject gcpProject = new GcpProject("name", "id");
    gcpProject.setAppEngine(AppEngine.NO_APPENGINE_APPLICATION);
    assertTrue(gcpProject.hasAppEngineInfo());
  }

  @Test
  public void testSetAppEngine_null() {
    GcpProject gcpProject = new GcpProject("name", "id");
    gcpProject.setAppEngine(null);
    assertNull(gcpProject.getAppEngine());
  }

  @Test
  public void testSetAppEngine() {
    GcpProject gcpProject = new GcpProject("name", "id");
    gcpProject.setAppEngine(AppEngine.withId("id"));
    assertNotNull(gcpProject.getAppEngine());
  }
}
