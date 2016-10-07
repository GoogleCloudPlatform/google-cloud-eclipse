/*******************************************************************************
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
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class LibraryFileTest {

  @Test(expected = NullPointerException.class)
  public void testConstructorNullArgument() {
    new LibraryFile(null);
  }

  @Test
  public void testConstructor() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    assertSame(mavenCoordinates, libraryFile.getMavenCoordinates());
  }

  @Test
  public void testSetNullFilters() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setFilters(null);
    assertNotNull(libraryFile.getFilters());
    assertTrue(libraryFile.getFilters().isEmpty());
  }

  @Test
  public void testSetExclusionFilters() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    List<Filter> exclusionFilters = Collections.singletonList(Filter.exclusionFilter("filter"));
    libraryFile.setFilters(exclusionFilters);
    assertNotNull(libraryFile.getFilters());
    assertThat(libraryFile.getFilters().size(), is(1));
    assertThat(libraryFile.getFilters().get(0).getPattern(), is("filter"));
  }

  @Test
  public void testSetFilters_preservesOrder() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
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
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setJavadocUri(null);
    assertNull(libraryFile.getJavadocUri());
  }

  @Test
  public void setJavadocUri() throws URISyntaxException {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setJavadocUri(new URI("http://example.com"));
    assertThat(libraryFile.getJavadocUri().toString(), is("http://example.com"));
  }

  @Test
  public void setNullSourceUri() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setSourceUri(null);
    assertNull(libraryFile.getSourceUri());
  }

  @Test
  public void setSourceUri() throws URISyntaxException {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setSourceUri(new URI("http://example.com"));
    assertThat(libraryFile.getSourceUri().toString(), is("http://example.com"));
  }

}
