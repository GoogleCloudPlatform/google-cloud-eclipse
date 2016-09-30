package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
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
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    assertSame(mavenCoordinates, libraryFile.getMavenCoordinates());
  }

  @Test
  public void testSetNullExclusionFilters() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setExclusionFilters(null);
    assertNotNull(libraryFile.getExclusionFilters());
  }

  @Test
  public void testSetExclusionFilters() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    List<ExclusionFilter> exclusionFilters = Collections.singletonList(new ExclusionFilter("d"));
    libraryFile.setExclusionFilters(exclusionFilters);
    assertNotNull(libraryFile.getExclusionFilters());
    assertThat(libraryFile.getExclusionFilters().size(), is(1));
    assertThat(libraryFile.getExclusionFilters().get(0).getPattern(), is("d"));
  }

  @Test
  public void testSetNullInclusionFilters() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setInclusionFilters(null);
    assertNotNull(libraryFile.getInclusionFilters());
  }

  @Test
  public void testSetInclusionFilters() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    List<InclusionFilter> inclusionFilters = Collections.singletonList(new InclusionFilter("d"));
    libraryFile.setInclusionFilters(inclusionFilters);
    assertNotNull(libraryFile.getInclusionFilters());
    assertThat(libraryFile.getInclusionFilters().size(), is(1));
    assertThat(libraryFile.getInclusionFilters().get(0).getPattern(), is("d"));
  }

  @Test
  public void setNullJavadocUri() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setJavadocUri(null);
    assertNull(libraryFile.getJavadocUri());
  }

  @Test
  public void setJavadocUri() throws URISyntaxException {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setJavadocUri(new URI("http://example.com"));
    assertThat(libraryFile.getJavadocUri().toString(), is("http://example.com"));
  }

  @Test
  public void setNullSourceUri() {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setSourceUri(null);
    assertNull(libraryFile.getSourceUri());
  }

  @Test
  public void setSourceUri() throws URISyntaxException {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("a", "b", "c");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setSourceUri(new URI("http://example.com"));
    assertThat(libraryFile.getSourceUri().toString(), is("http://example.com"));
  }

}
