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

import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.util.ArtifactRetriever;
import com.google.cloud.tools.eclipse.util.DependencyResolver;
import com.google.common.base.Strings;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

class LibraryFactory {

  private static final Logger logger = Logger.getLogger(LibraryFactory.class.getName());

  private static final String ELEMENT_NAME_LIBRARY = "library"; //$NON-NLS-1$
  private static final String ELEMENT_NAME_LIBRARY_FILE = "libraryFile"; //$NON-NLS-1$
  private static final String ELEMENT_NAME_EXCLUSION_FILTER = "exclusionFilter"; //$NON-NLS-1$
  private static final String ELEMENT_NAME_INCLUSION_FILTER = "inclusionFilter"; //$NON-NLS-1$
  private static final String ELEMENT_NAME_MAVEN_COORDINATES = "mavenCoordinates"; //$NON-NLS-1$
  private static final String ELEMENT_NAME_LIBRARY_DEPENDENCY = "libraryDependency"; //$NON-NLS-1$

  private static final String ATTRIBUTE_NAME_ID = "id"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_NAME = "name"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_SITE_URI = "siteUri"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_SOURCE_URI = "sourceUri"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_JAVADOC_URI = "javadocUri"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_PATTERN = "pattern"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_GROUP = "group"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_GROUP_ID = "groupId"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_REPOSITORY_URI = "repositoryUri"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_ARTIFACT_ID = "artifactId"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_VERSION = "version"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_TYPE = "type"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_TOOLTIP = "tooltip"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_CLASSIFIER = "classifier"; //$NON-NLS-1$
  private static final String ATTRIBUTE_NAME_EXPORT = "export"; //$NON-NLS-1$

  // prevent instantiation
  private LibraryFactory() {}

  static Library create(IConfigurationElement configurationElement) throws LibraryFactoryException {
    try {
      if (ELEMENT_NAME_LIBRARY.equals(configurationElement.getName())) {
        Library library = new Library(configurationElement.getAttribute(ATTRIBUTE_NAME_ID));
        library.setName(configurationElement.getAttribute(ATTRIBUTE_NAME_NAME));
        library.setSiteUri(new URI(configurationElement.getAttribute(ATTRIBUTE_NAME_SITE_URI)));
        library.setGroup(configurationElement.getAttribute(ATTRIBUTE_NAME_GROUP));
        library.setToolTip(configurationElement.getAttribute(ATTRIBUTE_NAME_TOOLTIP));
        library.setLibraryDependencies(getLibraryDependencies(
            configurationElement.getChildren(ELEMENT_NAME_LIBRARY_DEPENDENCY)));
        library.setLibraryFiles(
            getLibraryFiles(configurationElement.getChildren(ELEMENT_NAME_LIBRARY_FILE)));
        String exportString = configurationElement.getAttribute(ATTRIBUTE_NAME_EXPORT);
        if (exportString != null) {
          library.setExport(Boolean.parseBoolean(exportString));
        }
        String dependencies = configurationElement.getAttribute("dependencies"); //$NON-NLS-1$
        if (!"include".equals(dependencies)) { //$NON-NLS-1$
          library.setResolved();
        }
        String versionString = configurationElement.getAttribute("javaVersion"); //$NON-NLS-1$
        if (versionString != null) {
          library.setJavaVersion(versionString);
        }
        return library;
      } else {
        throw new LibraryFactoryException(
            Messages.getString("UnexpectedConfigurationElement",
                configurationElement.getName(), ELEMENT_NAME_LIBRARY));
      }
    } catch (InvalidRegistryObjectException | URISyntaxException | IllegalArgumentException ex) {
      throw new LibraryFactoryException(Messages.getString("CreateLibraryError"), ex);
    }
  }

  private static List<LibraryFile> getLibraryFiles(IConfigurationElement[] children)
      throws InvalidRegistryObjectException, URISyntaxException {
    List<LibraryFile> libraryFiles = new ArrayList<>();
    for (IConfigurationElement libraryFileElement : children) {
      if (ELEMENT_NAME_LIBRARY_FILE.equals(libraryFileElement.getName())) {
        MavenCoordinates mavenCoordinates = getMavenCoordinates(
            libraryFileElement.getChildren(ELEMENT_NAME_MAVEN_COORDINATES));
        LibraryFile libraryFile = loadSingleFile(libraryFileElement, mavenCoordinates);
        libraryFiles.add(libraryFile);
      }
    }
    return libraryFiles;
  }

  static Collection<LibraryFile> loadTransitiveDependencies(MavenCoordinates root)
      throws CoreException {
    Set<LibraryFile> dependencies = new HashSet<>();
    Collection<Artifact> artifacts = DependencyResolver.getTransitiveDependencies(
        root.getGroupId(), root.getArtifactId(), root.getVersion(), null);
    for (Artifact artifact : artifacts) {
      MavenCoordinates coordinates = new MavenCoordinates.Builder()
          .setGroupId(artifact.getGroupId())
          .setArtifactId(artifact.getArtifactId())
          .setVersion(artifact.getVersion())
          .build();
      LibraryFile file = new LibraryFile(coordinates);
      dependencies.add(file);
    }
    return dependencies;
  }

  private static LibraryFile loadSingleFile(IConfigurationElement libraryFileElement,
      MavenCoordinates mavenCoordinates) throws URISyntaxException {
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setFilters(getFilters(libraryFileElement.getChildren()));
    // todo do we really want these next two to be required?
    libraryFile.setSourceUri(
        getUri(libraryFileElement.getAttribute(ATTRIBUTE_NAME_SOURCE_URI)));
    libraryFile.setJavadocUri(
        getUri(libraryFileElement.getAttribute(ATTRIBUTE_NAME_JAVADOC_URI)));
    String exportString = libraryFileElement.getAttribute(ATTRIBUTE_NAME_EXPORT);
    if (exportString != null) {
      libraryFile.setExport(Boolean.parseBoolean(exportString));
    }
    return libraryFile;
  }

  private static URI getUri(String uriString) throws URISyntaxException {
    if (Strings.isNullOrEmpty(uriString)) {
      return null;
    } else {
      return new URI(uriString);
    }
  }

  private static MavenCoordinates getMavenCoordinates(IConfigurationElement[] children) {
    if (children.length != 1) {
      logger.warning(
          "Single configuration element for MavenCoordinates was expected, found: " //$NON-NLS-1$
          + children.length);
    }
    IConfigurationElement mavenCoordinatesElement = children[0];
    String groupId = mavenCoordinatesElement.getAttribute(ATTRIBUTE_NAME_GROUP_ID);
    String artifactId = mavenCoordinatesElement.getAttribute(ATTRIBUTE_NAME_ARTIFACT_ID);

    MavenCoordinates.Builder builder = new MavenCoordinates.Builder()
        .setGroupId(groupId)
        .setArtifactId(artifactId);

    String repository = mavenCoordinatesElement.getAttribute(ATTRIBUTE_NAME_REPOSITORY_URI);
    if (!Strings.isNullOrEmpty(repository)) {
      builder.setRepository(repository);
    }

    // Only look up latest version if version isn't specified in file.
    String version = mavenCoordinatesElement.getAttribute(ATTRIBUTE_NAME_VERSION);
    if (Strings.isNullOrEmpty(version) || "LATEST".equals(version)) {
      ArtifactVersion artifactVersion =
          ArtifactRetriever.DEFAULT.getLatestReleaseVersion(groupId, artifactId);
      if (artifactVersion != null) {
        version = artifactVersion.toString();
      }
    }

    if (!Strings.isNullOrEmpty(version)) {
      builder.setVersion(version);
    }
    String type = mavenCoordinatesElement.getAttribute(ATTRIBUTE_NAME_TYPE);
    if (!Strings.isNullOrEmpty(type)) {
      builder.setType(type);
    }
    String classifier = mavenCoordinatesElement.getAttribute(ATTRIBUTE_NAME_CLASSIFIER);
    if (!Strings.isNullOrEmpty(classifier)) {
      builder.setClassifier(classifier);
    }
    return builder.build();
  }

  private static List<Filter> getFilters(IConfigurationElement[] children) {
    List<Filter> filters = new ArrayList<>();
    for (IConfigurationElement childElement : children) {
      switch (childElement.getName()) {
        case ELEMENT_NAME_EXCLUSION_FILTER:
          filters.add(Filter.exclusionFilter(childElement.getAttribute(ATTRIBUTE_NAME_PATTERN)));
          break;
        case ELEMENT_NAME_INCLUSION_FILTER:
          filters.add(Filter.inclusionFilter(childElement.getAttribute(ATTRIBUTE_NAME_PATTERN)));
          break;
        default:
          // other child element of libraryFile, e.g.: mavenCoordinates
          break;
      }
    }
    return filters;
  }

  private static List<String> getLibraryDependencies(IConfigurationElement[] children) {
    List<String> libraryDependencies = new ArrayList<>();
    for (IConfigurationElement libraryDependencyElement : children) {
      String libraryId = libraryDependencyElement.getAttribute(ATTRIBUTE_NAME_ID);
      if (!Strings.isNullOrEmpty(libraryId)) {
        libraryDependencies.add(libraryId);
      }
    }
    return libraryDependencies;
  }
}
