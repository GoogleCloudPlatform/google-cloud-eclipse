package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Test;

public class LibraryTest {

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
    Library library = new Library("a");
    assertThat(library.getId(), is("a"));
  }

  @Test
  public void testGetContainerPath() {
    Library library = new Library("a");
    assertThat(library.getContainerPath().toOSString(),
               is("com.google.cloud.tools.eclipse.appengine.libraries" + "/" + "a"));
  }

  @Test
  public void testSetNullName() {
    Library library = new Library("a");
    library.setName(null);
    assertNull(library.getName());
  }

  @Test
  public void testSetName() {
    Library library = new Library("a");
    library.setName("b");
    assertThat(library.getName(), is("b"));
  }

  @Test
  public void testSetNullSiteUri() {
    Library library = new Library("a");
    library.setSiteUri(null);
    assertNull(library.getSiteUri());
  }

  @Test
  public void testSetSiteUri() throws URISyntaxException {
    Library library = new Library("a");
    library.setSiteUri(new URI("http://example.com"));
    assertThat(library.getSiteUri().toString(), is("http://example.com"));
  }

  @Test
  public void testLibraryFilesDefaultsToEmpty() {
    Library library = new Library("a");
    assertNotNull(library.getLibraryFiles());
    assertTrue(library.getLibraryFiles().isEmpty());
  }

  @Test
  public void setLibraryFilesNullDoesNotChangeIt() {
    Library library = new Library("a");
    library.setLibraryFiles(Arrays.asList(new LibraryFile(new MavenCoordinates("a", "b", "c"))));
    assertNotNull(library.getLibraryFiles());
    assertThat(library.getLibraryFiles().size(), is(1));

    library.setLibraryFiles(null);
    assertNotNull(library.getLibraryFiles());
    assertThat(library.getLibraryFiles().size(), is(1));
    LibraryFile actual = library.getLibraryFiles().get(0);
    assertThat(actual.getMavenCoordinates().getRepository(), is("a"));
    assertThat(actual.getMavenCoordinates().getGroupId(), is("b"));
    assertThat(actual.getMavenCoordinates().getArtifactId(), is("c"));
  }
}
