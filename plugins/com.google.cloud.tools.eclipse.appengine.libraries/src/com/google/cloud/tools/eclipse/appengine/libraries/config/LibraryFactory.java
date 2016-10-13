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
package com.google.cloud.tools.eclipse.appengine.libraries.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

import com.google.cloud.tools.eclipse.appengine.libraries.Filter;
import com.google.cloud.tools.eclipse.appengine.libraries.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;
import com.google.common.base.Strings;

public class LibraryFactory {

  private static final Logger logger = Logger.getLogger(LibraryFactory.class.getName());

  private static final String ELMT_LIBRARY = "library";
  private static final String ATTR_ID = "id";
  private static final String ATTR_NAME = "name";
  private static final String ATTR_SITE_URI = "siteUri";
  private static final String ELMT_LIBRARY_FILE = "libraryFile";
  private static final String ELMT_EXCLUSION_FILTER = "exclusionFilter";
  private static final String ELMT_INCLUSION_FILTER = "inclusionFilter";
  private static final String ELMT_MAVEN_COORDINATES = "mavenCoordinates";
  private static final String ATTR_SOURCE_URI = "sourceUri";
  private static final String ATTR_JAVADOC_URI = "javadocUri";
  private static final String ATTR_PATTERN = "pattern";
  private static final String ATTR_GROUP_ID = "groupId";
  private static final String ATTR_REPOSITORY_URI = "repositoryUri";
  private static final String ATTR_ARTIFACT_ID = "artifactId";
  private static final String ATTR_VERSION = "version";
  private static final String ATTR_TYPE = "type";
  private static final String ATTR_CLASSIFIER = "classifier";
  private static final String ATTR_EXPORT = "export";

  public Library create(IConfigurationElement configurationElement) throws LibraryFactoryException {
    try {
      if (configurationElement.getName().equals(ELMT_LIBRARY)) {
        Library library = new Library(configurationElement.getAttribute(ATTR_ID));
        library.setName(configurationElement.getAttribute(ATTR_NAME));
        library.setSiteUri(new URI(configurationElement.getAttribute(ATTR_SITE_URI)));
        library.setLibraryFiles(getLibraryFiles(configurationElement.getChildren(ELMT_LIBRARY_FILE)));
        return library;
      } else {
        throw new LibraryFactoryException(MessageFormat.format("Unexpected configuration element with name: {0}. "
                                                               + "Expected element is {1}",
                                                               configurationElement.getName(),
                                                               ELMT_LIBRARY));
      }
    } catch (InvalidRegistryObjectException | URISyntaxException exception) {
      throw new LibraryFactoryException("Error while creating Library instance", exception);
    }
  }

  private List<LibraryFile> getLibraryFiles(IConfigurationElement[] children) throws InvalidRegistryObjectException, 
                                                                                     URISyntaxException {
    List<LibraryFile> libraryFiles = new ArrayList<>();
    for (IConfigurationElement libraryFileElement : children) {
      if (libraryFileElement.getName().equals(ELMT_LIBRARY_FILE)) {
        MavenCoordinates mavenCoordinates = getMavenCoordinates(libraryFileElement.getChildren(ELMT_MAVEN_COORDINATES));
        LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
        libraryFile.setFilters(getFilters(libraryFileElement.getChildren()));
        libraryFile.setSourceUri(getUri(libraryFileElement.getAttribute(ATTR_SOURCE_URI)));
        libraryFile.setJavadocUri(getUri(libraryFileElement.getAttribute(ATTR_JAVADOC_URI)));
        libraryFile.setExport(Boolean.parseBoolean(libraryFileElement.getAttribute(ATTR_EXPORT)));
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
      logger.warning("Single configuration element for MavenCoordinates was expected, found: " + children.length);
    }
    IConfigurationElement mavenCoordinatesElement = children[0];
    String groupId = mavenCoordinatesElement.getAttribute(ATTR_GROUP_ID);
    String artifactId = mavenCoordinatesElement.getAttribute(ATTR_ARTIFACT_ID);
    MavenCoordinates mavenCoordinates = new MavenCoordinates(groupId, artifactId);
    String repository = mavenCoordinatesElement.getAttribute(ATTR_REPOSITORY_URI);
    if (!Strings.isNullOrEmpty(repository)) {
      mavenCoordinates.setRepository(repository);
    }
    String version = mavenCoordinatesElement.getAttribute(ATTR_VERSION);
    if (!Strings.isNullOrEmpty(version)) {
      mavenCoordinates.setVersion(version);
    }
    String type = mavenCoordinatesElement.getAttribute(ATTR_TYPE);
    if (!Strings.isNullOrEmpty(type)) {
      mavenCoordinates.setType(type);
    }
    String classifier = mavenCoordinatesElement.getAttribute(ATTR_CLASSIFIER);
    if (!Strings.isNullOrEmpty(classifier)) {
      mavenCoordinates.setClassifier(classifier);
    }
    return mavenCoordinates;
  }

  private List<Filter> getFilters(IConfigurationElement[] children) {
    List<Filter> filters = new ArrayList<>();
    for (IConfigurationElement childElement : children) {
      switch (childElement.getName()) {
      case ELMT_EXCLUSION_FILTER:
        filters.add(Filter.exclusionFilter(childElement.getAttribute(ATTR_PATTERN)));
        break;
      case ELMT_INCLUSION_FILTER:
        filters.add(Filter.inclusionFilter(childElement.getAttribute(ATTR_PATTERN)));
        break;
      default:
        // other child element of libraryFile, e.g.: mavenCoordinates
        break;
      }
    }
    return filters;
  }

  public static class LibraryFactoryException extends Exception {

    public LibraryFactoryException(String message, Throwable cause) {
      super(message, cause);
    }

    public LibraryFactoryException(String message) {
      super(message);
    }
  }
}
