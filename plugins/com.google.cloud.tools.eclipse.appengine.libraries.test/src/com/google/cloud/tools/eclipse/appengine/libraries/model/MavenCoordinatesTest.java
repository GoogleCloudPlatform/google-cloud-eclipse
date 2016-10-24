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

package com.google.cloud.tools.eclipse.appengine.libraries.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MavenCoordinatesTest {

  @Test(expected = NullPointerException.class)
  public void testConstructorGroupIdNull() {
    new MavenCoordinates(null, "artifactId");
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorArtifactIdNull() {
    new MavenCoordinates("groupId", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorEmptyGroupId() {
    new MavenCoordinates("", "artifactId");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorEmptyArtifactId() {
    new MavenCoordinates("groupId", "");
  }

  @Test
  public void testConstructorValidArguments() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    assertThat(mavenCoordinates.getGroupId(), is("groupId"));
    assertThat(mavenCoordinates.getArtifactId(), is("artifactId"));
  }

  @Test
  public void testRepositoryDefaultsToCentral() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("b", "c");
    assertThat(mavenCoordinates.getRepository(), is(MavenCoordinates.MAVEN_CENTRAL_REPO));
  }

  @Test(expected = NullPointerException.class)
  public void testSetRepositoryNull() {
    new MavenCoordinates("a", "b").setRepository(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetEmptyRepository() {
    new MavenCoordinates("a", "b").setRepository("");;
  }

  @Test
  public void testVersionDefaultsToLatest() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    assertThat(mavenCoordinates.getVersion(), is(MavenCoordinates.LATEST_VERSION));
  }

  @Test(expected = NullPointerException.class)
  public void testSetNullVersion() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setVersion(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetEmptyVersion() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setVersion("");
  }

  @Test
  public void setVersion() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setVersion("1");
    assertThat(mavenCoordinates.getVersion(), is("1"));
  }

  @Test
  public void testTypeDefaultsToJar() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    assertThat(mavenCoordinates.getType(), is(MavenCoordinates.JAR_TYPE));
  }

  @Test(expected = NullPointerException.class)
  public void testSetNullType() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setType(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetEmptyType() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setType("");
  }

  @Test
  public void testSetType() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setType("war");
    assertThat(mavenCoordinates.getType(), is("war"));
  }

  @Test
  public void testClassifierDefaultsToNull() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    assertNull(mavenCoordinates.getClassifier());
  }

  @Test
  public void testSetClassifier() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setClassifier("d");
    assertThat(mavenCoordinates.getClassifier(), is("d"));
  }

  @Test
  public void testSetNullClassifier() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setClassifier(null);
    assertNull(mavenCoordinates.getClassifier());
  }

  @Test
  public void testSetEmptyClassifier() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setClassifier("");
    assertThat(mavenCoordinates.getClassifier(), is(""));
  }
}
