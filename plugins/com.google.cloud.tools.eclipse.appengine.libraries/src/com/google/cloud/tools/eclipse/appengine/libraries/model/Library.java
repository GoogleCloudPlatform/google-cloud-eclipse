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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * A library that can be added to App Engine projects, e.g. App Engine Endpoints library.
 *
 */
public final class Library {
  public static final String CONTAINER_PATH_PREFIX =
      "com.google.cloud.tools.eclipse.appengine.libraries";

  private final String id;
  private String name;
  private String toolTip;
  private URI siteUri;
  private boolean export = true;
  private List<LibraryFile> libraryFiles = Collections.emptyList();
  private LibraryRecommendation recommendation = LibraryRecommendation.OPTIONAL;
  private String group;
  private String javaVersion="1.7";
  
  // true if the dependencies for this library have been loaded
  private boolean resolved = true;

  // IDs of other libraries that also need to be added to the build path with this library
  private List<String> libraryDependencies = new ArrayList<>();

  public Library(String id) {
    Preconditions.checkNotNull(id, "id null");
    Preconditions.checkArgument(!id.isEmpty(), "id empty");
    this.id = id;
  }

  @VisibleForTesting
  public Library(String id, List<LibraryFile> libraryFiles) {
    this.id = id;
    this.libraryFiles = libraryFiles;
  }
  
  public String getId() {
    return id;
  }

  public IPath getContainerPath() {
    return new Path(CONTAINER_PATH_PREFIX + "/" + id);
  }

  public String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }
  
  /**
   * @return minimum Java version required for this library
   */
  public String getJavaVersion() {
    return javaVersion;
  }

  void setJavaVersion(String version) {
    this.javaVersion = version;
  }
  
  public String getToolTip() {
    return toolTip;
  }

  void setToolTip(String toolTip) {
    this.toolTip = toolTip;
  }

  public URI getSiteUri() {
    return siteUri;
  }

  void setSiteUri(URI siteUri) {
    this.siteUri = siteUri;
  }

  public synchronized List<LibraryFile> getLibraryFiles() {
    return new ArrayList<>(libraryFiles);
  }

  /**
   * @param libraryFiles artifacts associated with this library, cannot be <code>null</code>
   */
  public synchronized void setLibraryFiles(List<LibraryFile> libraryFiles) {
    Preconditions.checkNotNull(libraryFiles);
    this.libraryFiles = new ArrayList<>(libraryFiles);
  }

  public boolean isExport() {
    return export;
  }

  void setExport(boolean export) {
    this.export = export;
  }

  /**
   * @return list of library IDs that are dependencies of this library
   *     and should be added to the classpath, cannot be <code>null</code>
   */
  public List<String> getLibraryDependencies() {
    return new ArrayList<>(libraryDependencies);
  }

  /**
   * @param libraryDependencies list of library IDs that are dependencies of this library
   *     and should be added to the classpath, cannot be <code>null</code>
   */
   void setLibraryDependencies(List<String> libraryDependencies) {
    Preconditions.checkNotNull(libraryDependencies);
    this.libraryDependencies = new ArrayList<>(libraryDependencies);
  }

  /**
   * @param recommendation the level of recommendation for this library, cannot be <code>null</code>
   */
  void setRecommendation(LibraryRecommendation recommendation) {
    Preconditions.checkNotNull(recommendation);
    this.recommendation = recommendation;
  }

  /**
   * @return the level of recommendation for this library
   */
  public LibraryRecommendation getRecommendation() {
    return recommendation;
  }

  /**
   * @param group the collection to which this library belongs
   */
  void setGroup(String group) {
    this.group = group;
  }

  /**
   * @return the collection to which this library belongs
   */
  public String getGroup() {
    return group;
  }
  
  public synchronized boolean isResolved() {
    return this.resolved;
  }
  
  /**
   * @param resolved true iff this library contains its complete dependency graph
   */
  synchronized void setResolved(boolean resolved) {
    this.resolved = resolved;
  }
  
  /**
   * A potentially long running operation that connects to the
   * local and remote Maven repos and adds all library files in the
   * transitive dependency graph to the list.
   *  
   * @throws CoreException error loading transitive dependencies
   */
  public synchronized void resolveDependencies() throws CoreException {
    if (!resolved) {
      List<LibraryFile> transitiveDependencies = new ArrayList<>();
      for (LibraryFile artifact : this.libraryFiles) {
        MavenCoordinates coordinates = artifact.getMavenCoordinates();
        Collection<LibraryFile> dependencies = LibraryFactory.loadTransitiveDependencies(coordinates);
        transitiveDependencies.addAll(dependencies);
      }
      
      this.libraryFiles = resolveDuplicates(transitiveDependencies);
      this.resolved = true;
    }
  }  
  
  /**
   * Strip out different versions of the same library, retaining only the most recent.
   *
   * @return a new list containing the most recent version of each dependency
   */
  public static List<LibraryFile> resolveDuplicates(List<LibraryFile> dependencies) {
    TreeMap<String, LibraryFile> map = new TreeMap<>();
    for (LibraryFile file : dependencies) {
      MavenCoordinates coordinates = file.getMavenCoordinates();
      String key = coordinates.getGroupId() + ":" + coordinates.getArtifactId();
      if (map.containsKey(key)) {
        MavenCoordinates previousCoordinates = map.get(key).getMavenCoordinates();
        if (newer(coordinates, previousCoordinates)) {
          map.put(key, file);
        }
      } else {
        map.put(key, file);
      }
      
    }
    return new ArrayList<>(map.values());
  }

  private static boolean newer(MavenCoordinates coordinates, MavenCoordinates previousCoordinates) {
    try {
      ComparableVersion version1 = new ComparableVersion(coordinates.getVersion());
      ComparableVersion version2 = new ComparableVersion(previousCoordinates.getVersion());
      
      return version1.compareTo(version2) > 0;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  /**
   * @return a string suitable for debugging
   */
  @Override
  public String toString() {
    return "Library: id=" + id + "; name=" + name;
  }

}
