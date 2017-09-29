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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

public class LibraryTest {
  
  private Library library = new Library("a");

  @Test(expected = NullPointerException.class)
  public void testConstructorNullArgument() {
    new Library(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorEmptyArgument() {
    new Library("");
  }

  @Test
  public void testConstructor() {
    assertThat(library.getId(), is("a"));
  }

  @Test
  public void testGetContainerPath() {
    assertThat(library.getContainerPath().toString(),
               is("com.google.cloud.tools.eclipse.appengine.libraries" + "/" + "a"));
  }

  @Test
  public void testSetNullName() {
    library.setName(null);
    assertNull(library.getName());
  }

  @Test
  public void testSetName() {
    library.setName("b");
    assertThat(library.getName(), is("b"));
  }
  
  @Test
  public void testSetToolTip() {
    library.setToolTip("some help");
    assertThat(library.getToolTip(), is("some help"));
  }
  
  @Test
  public void testSetGroup() {
    library.setGroup("One Platform");
    assertThat(library.getGroup(), is("One Platform"));
  }

  @Test
  public void testSetNullSiteUri() {
    library.setSiteUri(null);
    assertNull(library.getSiteUri());
  }

  @Test
  public void testSetSiteUri() throws URISyntaxException {
    library.setSiteUri(new URI("http://example.com"));
    assertThat(library.getSiteUri().toString(), is("http://example.com"));
  }

  @Test
  public void testLibraryFilesDefaultsToEmpty() {
    assertNotNull(library.getLibraryFiles());
    assertTrue(library.getLibraryFiles().isEmpty());
  }

  @Test(expected = NullPointerException.class)
  public void setLibraryFilesNullDoesNotChangeIt() {
    library.setLibraryFiles(null);
  }

  @Test
  public void setLibraryFiles() {
    library.setLibraryFiles(
        Arrays.asList(new LibraryFile(new MavenCoordinates("groupId", "artifactId"))));
    assertNotNull(library.getLibraryFiles());
    assertThat(library.getLibraryFiles().size(), is(1));
    LibraryFile actual = library.getLibraryFiles().get(0);
    assertThat(actual.getMavenCoordinates().getRepository(), is("central"));
    assertThat(actual.getMavenCoordinates().getGroupId(), is("groupId"));
    assertThat(actual.getMavenCoordinates().getArtifactId(), is("artifactId"));
  }

  @Test
  public void testExportDefaultsToTrue() {
    assertTrue(library.isExport());
  }

  @Test
  public void testResolvedDefaultsToTrue() throws CoreException {
    assertTrue(library.isResolved());
    library.resolveDependencies(); // no-op because the library is marked resolved
    library.setResolved(false);
    assertFalse(library.isResolved());
  }

  @Test
  public void testResolvedDuplicates() throws CoreException {
    MavenCoordinates coordinates19 = new MavenCoordinates.Builder()
        .setGroupId("com.google.guava")
        .setArtifactId("guava")
        .setVersion("19.0")
        .build();    
    MavenCoordinates coordinates20 = new MavenCoordinates.Builder()
        .setGroupId("com.google.guava")
        .setArtifactId("guava")
        .setVersion("20.0")
        .build();    
    
    LibraryFile guava19 = new LibraryFile(coordinates19);
    LibraryFile guava20 = new LibraryFile(coordinates20);
    
    List<LibraryFile> guavas = new ArrayList<>();
    guavas.add(guava19);
    guavas.add(guava20);
    
    List<LibraryFile> actual = Library.resolveDuplicates(guavas);
    assertEquals(1, actual.size());
    assertEquals("20.0", actual.get(0).getMavenCoordinates().getVersion());
  }
  
  @Test
  public void testResolvedDuplicates_semanticVersioning() throws CoreException {
    MavenCoordinates coordinates1 = new MavenCoordinates.Builder()
        .setGroupId("com.google.guava")
        .setArtifactId("guava")
        .setVersion("19.0.1")
        .build();    
    MavenCoordinates coordinates2 = new MavenCoordinates.Builder()
        .setGroupId("com.google.guava")
        .setArtifactId("guava")
        .setVersion("19.0.3")
        .build();
    MavenCoordinates coordinates3 = new MavenCoordinates.Builder()
        .setGroupId("com.google.guava")
        .setArtifactId("guava")
        .setVersion("19.0.2")
        .build();
    
    LibraryFile guava1 = new LibraryFile(coordinates1);
    LibraryFile guava2 = new LibraryFile(coordinates2);
    LibraryFile guava3 = new LibraryFile(coordinates3);
    
    List<LibraryFile> guavas = new ArrayList<>();
    guavas.add(guava1);
    guavas.add(guava2);
    guavas.add(guava3);
    
    List<LibraryFile> actual = Library.resolveDuplicates(guavas);
    assertEquals(1, actual.size());
    assertEquals("19.0.3", actual.get(0).getMavenCoordinates().getVersion());
  }
  
  @Test
  public void testSetExport() {
    library.setExport(false);
    assertFalse(library.isExport());
    library.setExport(true);
    assertTrue(library.isExport());
  }

  @Test
  public void testLibraryDependenciesDefaultsToEmptyList() {
    assertNotNull(library.getLibraryDependencies());
    assertTrue(library.getLibraryDependencies().isEmpty());
  }

  @Test(expected = NullPointerException.class)
  public void testSetLibraryDependencies_null() {
    library.setLibraryDependencies(null);
  }

  @Test
  public void testSetLibraryDependencies() {
    library.setLibraryDependencies(Collections.singletonList("libraryId"));
    assertNotNull(library.getLibraryDependencies());
    assertThat(library.getLibraryDependencies().size(), is(1));
    assertThat(library.getLibraryDependencies().get(0), is("libraryId"));
  }
  
  @Test
  public void testSetJavaVersion() {
    assertEquals("1.7", library.getJavaVersion());
    library.setJavaVersion("1.9");
    assertEquals("1.9", library.getJavaVersion());
  }
  
  @Test
  public void testToString() {
    library.setName("foo");
    String s = library.toString();

    assertTrue(s, s.startsWith("Library"));
    assertTrue(s, s.contains(library.getName()));
    assertTrue(s, s.contains(library.getId()));
  }
}
