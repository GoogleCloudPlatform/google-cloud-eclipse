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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LibraryFileTest {

  private MavenCoordinates mavenCoordinates;

  @Before
  public void setUp() {
    mavenCoordinates = new MavenCoordinates.Builder()
        .setGroupId("groupId")
        .setArtifactId("artifactId")
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorNullArgument() {
    new LibraryFile(null);
  }

  @Test
  public void testConstructor() {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    assertSame(mavenCoordinates, libraryFile.getMavenCoordinates());
  }

  @Test
  public void testSetNullFilters() {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setFilters(null);
    assertNotNull(libraryFile.getFilters());
    assertTrue(libraryFile.getFilters().isEmpty());
  }

  @Test
  public void testSetExclusionFilters() {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    List<Filter> exclusionFilters = Collections.singletonList(Filter.exclusionFilter("filter"));
    libraryFile.setFilters(exclusionFilters);
    assertNotNull(libraryFile.getFilters());
    assertThat(libraryFile.getFilters().size(), is(1));
    assertThat(libraryFile.getFilters().get(0).getPattern(), is("filter"));
  }

  @Test
  public void testSetFilters_preservesOrder() {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    List<Filter> filters = Arrays.asList(Filter.exclusionFilter("exclusionFilter1"),
                                         Filter.inclusionFilter("inclusionFilter1"),
                                         Filter.inclusionFilter("inclusionFilter2"),
                                         Filter.exclusionFilter("exclusionFilter2"));
    libraryFile.setFilters(filters);
    assertNotNull(libraryFile.getFilters());
    assertThat(libraryFile.getFilters().size(), is(4));
    assertThat(libraryFile.getFilters().get(0).getPattern(), is("exclusionFilter1"));
    assertTrue(libraryFile.getFilters().get(0).isExclude());
    assertThat(libraryFile.getFilters().get(1).getPattern(), is("inclusionFilter1"));
    assertFalse(libraryFile.getFilters().get(1).isExclude());
    assertThat(libraryFile.getFilters().get(2).getPattern(), is("inclusionFilter2"));
    assertFalse(libraryFile.getFilters().get(2).isExclude());
    assertThat(libraryFile.getFilters().get(3).getPattern(), is("exclusionFilter2"));
    assertTrue(libraryFile.getFilters().get(3).isExclude());
  }

  @Test
  public void setNullJavadocUri() {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setJavadocUri(null);
    assertNull(libraryFile.getJavadocUri());
  }

  @Test
  public void setJavadocUri() throws URISyntaxException {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setJavadocUri(new URI("http://example.com"));
    assertThat(libraryFile.getJavadocUri().toString(), is("http://example.com"));
  }

  @Test
  public void setNullSourceUri() {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setSourceUri(null);
    assertNull(libraryFile.getSourceUri());
  }

  @Test
  public void setSourceUri() throws URISyntaxException {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setSourceUri(new URI("http://example.com"));
    assertThat(libraryFile.getSourceUri().toString(), is("http://example.com"));
  }

  @Test
  public void testExportDefaultsToTrue() {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    assertTrue(libraryFile.isExport());
  }

  @Test
  public void testSetExport() {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setExport(false);
    assertFalse(libraryFile.isExport());
  }
  
  @Test
  public void testToString() {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    assertEquals("groupId:artifactId:LATEST", libraryFile.toString());
  }
  
  @Test
  public void testCompareTo() {
    MavenCoordinates m1 = new MavenCoordinates.Builder()
        .setArtifactId("A1").setGroupId("G1").setVersion("1").build();
    MavenCoordinates m2 = new MavenCoordinates.Builder()
        .setArtifactId("B1").setGroupId("G1").setVersion("2").build();
    MavenCoordinates m3 = new MavenCoordinates.Builder()
        .setArtifactId("A1").setGroupId("E1").setVersion("1").build();
    MavenCoordinates m4 = new MavenCoordinates.Builder()
        .setArtifactId("A1").setGroupId("G1").setVersion("2").build();
    MavenCoordinates m5 = new MavenCoordinates.Builder()
        .setArtifactId("A1").setGroupId("G1").setVersion("1").build();
    
    LibraryFile libraryFile1 = new LibraryFile(m1);
    LibraryFile libraryFile2 = new LibraryFile(m2);
    LibraryFile libraryFile3 = new LibraryFile(m3);
    LibraryFile libraryFile4 = new LibraryFile(m4);
    LibraryFile libraryFile5 = new LibraryFile(m5); // same as libraryFile1
    
    SortedSet<LibraryFile> files = new TreeSet<>();
    files.add(libraryFile1);
    files.add(libraryFile2);
    files.add(libraryFile3);
    files.add(libraryFile4);
    files.add(libraryFile5);
    
    Iterator<LibraryFile> iterator = files.iterator();
    assertSame(libraryFile3, iterator.next());
    assertSame(libraryFile1, iterator.next());
    assertSame(libraryFile4, iterator.next());
    assertSame(libraryFile2, iterator.next());
    assertFalse(iterator.hasNext());
    
    // consistent with equals
    assertEquals(libraryFile1, libraryFile5);
    assertEquals(libraryFile1.hashCode(), libraryFile5.hashCode());
  }
  
  @Test
  public void testEquals() {
    MavenCoordinates m1 = new MavenCoordinates.Builder()
        .setArtifactId("A1")
        .setGroupId("G1")
        .setVersion("1")
        .build();
    LibraryFile libraryFile1 = new LibraryFile(m1);

    assertNotEquals(libraryFile1, null);
    assertNotEquals(libraryFile1, libraryFile1.getMavenCoordinates().toStringCoordinates());
  }
  
  @Test
  public void testUpdateVersion() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates.Builder()
        .setGroupId("com.google.guava")
        .setArtifactId("guava")
        .setVersion("15.0")
        .build();
    
    LibraryFile file = new LibraryFile(mavenCoordinates);
    file.updateVersion();

    assertFalse(file.getMavenCoordinates().getVersion().isEmpty());
    assertNotEquals("15.0", file.getMavenCoordinates().getVersion());
  }
  
  // DefaultArtifactVersion.compareTo isn't well documented so test it here since we depend on this
  // logic.
  @Test
  public void testCompareVersions() {
    DefaultArtifactVersion newer = new DefaultArtifactVersion("0.25.0-alpha");
    DefaultArtifactVersion older = new DefaultArtifactVersion("0.8.0");
    Assert.assertTrue(newer.compareTo(older) > 0);
  }

}
