package com.google.cloud.tools.eclipse.appengine.libraries.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

import com.google.cloud.tools.eclipse.appengine.libraries.Filter;
import com.google.cloud.tools.eclipse.appengine.libraries.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;

public class LibraryBuilder {

  private static final String LIBRARY = "library";
  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String SITE_URI = "siteUri";
  private static final String LIBRARY_FILE = "libraryFile";
  private static final String EXCLUSION_FILTER = "exclusionFilter";
  private static final String INCLUSION_FILTER = "inclusionFilter";
  private static final String MAVEN_COORDINATES = "mavenCoordinates";
  private static final String SOURCE_URI = "sourceUri";
  private static final String JAVADOC_URI = "javadocUri";
  private static final String PATTERN = "pattern";
  private static final String GROUP_ID = "groupId";
  private static final String REPOSITORY_URI = "repositoryUri";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String VERSION = "version";
  private static final String TYPE = "type";
  private static final String CLASSIFIER = "classifier";

  public Library build(IConfigurationElement configurationElement) {
    try {
      if (configurationElement.getName().equals(LIBRARY)) {
        Library library = new Library(configurationElement.getAttribute(ID));
        library.setName(configurationElement.getAttribute(NAME));
        library.setSiteUri(new URI(configurationElement.getAttribute(SITE_URI)));
        library.setLibraryFiles(getLibraryFiles(configurationElement.getChildren(LIBRARY_FILE)));
        return library;
      }
    } catch (InvalidRegistryObjectException | URISyntaxException e) {
      // TODO: handle exception
    }
    return null;
  }

  private List<LibraryFile> getLibraryFiles(IConfigurationElement[] children) throws InvalidRegistryObjectException, 
                                                                                     URISyntaxException {
    List<LibraryFile> libraryFiles = new ArrayList<>();
    for (IConfigurationElement libraryFileElement : children) {
      if (libraryFileElement.getName().equals(LIBRARY_FILE)) {
        MavenCoordinates mavenCoordinates = getMavenCoordinates(libraryFileElement.getChildren(MAVEN_COORDINATES));
        LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
        libraryFile.setExclusionFilters(getExclusionFilters(libraryFileElement.getChildren(EXCLUSION_FILTER)));
        libraryFile.setInclusionFilters(getInclusionFilters(libraryFileElement.getChildren(INCLUSION_FILTER)));
        libraryFile.setSourceUri(getUri(libraryFileElement.getAttribute(SOURCE_URI)));
        libraryFile.setJavadocUri(getUri(libraryFileElement.getAttribute(JAVADOC_URI)));
        libraryFiles.add(libraryFile);
      }
    }
    return libraryFiles;
  }

  private URI getUri(String uriString) throws URISyntaxException {
    if (uriString == null || uriString.isEmpty()) {
      return null;
    } else {
      return new URI(uriString);
    }
  }

  private MavenCoordinates getMavenCoordinates(IConfigurationElement[] children) {
    if (children.length != 1) {
      // error
    }
    IConfigurationElement mavenCoordinatesElement = children[0];
    String repository = mavenCoordinatesElement.getAttribute(REPOSITORY_URI);
    String groupId = mavenCoordinatesElement.getAttribute(GROUP_ID);
    String artifactId = mavenCoordinatesElement.getAttribute(ARTIFACT_ID);
    MavenCoordinates mavenCoordinates = new MavenCoordinates(repository, groupId, artifactId);
    mavenCoordinates.setVersion(mavenCoordinatesElement.getAttribute(VERSION));
    mavenCoordinates.setType(mavenCoordinatesElement.getAttribute(TYPE));
    mavenCoordinates.setClassifier(mavenCoordinatesElement.getAttribute(CLASSIFIER));
    return mavenCoordinates;
  }

  private List<Filter> getInclusionFilters(IConfigurationElement[] children) {
    List<Filter> inclusionFilters = new ArrayList<>();
    for (IConfigurationElement inclusionFilterElement : children) {
      inclusionFilters.add(new Filter(inclusionFilterElement.getAttribute(PATTERN)));
    }
    return inclusionFilters;
  }

  private List<Filter> getExclusionFilters(IConfigurationElement[] children) {
    List<Filter> exclusionFilters = new ArrayList<>();
    for (IConfigurationElement exclusionFilterElement : children) {
      exclusionFilters.add(new Filter(exclusionFilterElement.getAttribute(PATTERN)));
    }
    return exclusionFilters;
  }

}
