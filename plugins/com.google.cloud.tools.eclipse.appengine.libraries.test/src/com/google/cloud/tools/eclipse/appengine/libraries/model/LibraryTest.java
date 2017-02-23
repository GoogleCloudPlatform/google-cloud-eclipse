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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
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
  public void testSetExport() {
    library.setExport(false);
    assertFalse(library.isExport());
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
  public void testRecommendationDefaultsToOptional() {
    assertThat(library.getRecommendation(), is(LibraryRecommendation.OPTIONAL));
  }

  @Test
  public void testSetRecommendation() {
    library.setRecommendation(LibraryRecommendation.REQUIRED);
    assertThat(library.getRecommendation(), is(LibraryRecommendation.REQUIRED));
  }

  @Test(expected = NullPointerException.class)
  public void testSetRecommendation_null() {
    library.setRecommendation(null);
  }
}
