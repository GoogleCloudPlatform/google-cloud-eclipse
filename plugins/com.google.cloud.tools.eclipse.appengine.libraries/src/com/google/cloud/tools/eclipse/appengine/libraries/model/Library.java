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

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.core.runtime.CoreException;

/**
 * A library that can be added to App Engine projects, e.g. App Engine Endpoints library.
 *
 */
public final class Library {

  private static final Logger logger = Logger.getLogger(Library.class.getName());

  private final String id;
  private String name;
  private String toolTip;
  private URI siteUri;
  private boolean export = true;
  private List<LibraryFile> transitiveDependencies = null;
  private List<LibraryFile> directDependencies = Collections.emptyList();
  private String group;
  private String stage = "GA";
  private String javaVersion="1.7";
  private String transport = "http";

  // IDs of other libraries that also need to be added to the build path with this library
  private List<String> libraryDependencies = new ArrayList<>();

  public Library(String id) {
    Preconditions.checkNotNull(id, "id null");
    Preconditions.checkArgument(!id.isEmpty(), "id empty");
    this.id = id;
  }
  
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  /**
   * @return minimum Java version required for this library
   */
  public String getJavaVersion() {
    return javaVersion;
  }

  void setJavaVersion(String version) {
    javaVersion = version;
  }
  
  public String getToolTip() {
    return toolTip;
  }

  void setToolTip(String toolTip) {
    this.toolTip = toolTip;
  }
  
  /**
   * @return typically GA, alpha, beta, or deprecated though other values are possible
   */
  public String getLaunchStage() {
    return stage;
  }
  
  void setLaunchStage(String stage) {
    this.stage = stage;
  }

  public URI getSiteUri() {
    return siteUri;
  }

  void setSiteUri(URI siteUri) {
    this.siteUri = siteUri;
  }

  /**
   * Returns the complete list of all transitive dependencies for this library.
   * This can generate large amounts of network traffic.
   */
  public synchronized List<LibraryFile> getAllDependencies() {
    if (transitiveDependencies == null) {
      transitiveDependencies = resolveDependencies();
    }
    return new ArrayList<>(transitiveDependencies);
  }

  /**
   * @param libraryFiles artifacts associated with this library, cannot be <code>null</code>
   */
  public synchronized void setLibraryFiles(List<LibraryFile> libraryFiles) {
    Preconditions.checkNotNull(libraryFiles);
    directDependencies = new ArrayList<>(libraryFiles);
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
  public void setLibraryDependencies(List<String> libraryDependencies) {
    Preconditions.checkNotNull(libraryDependencies);
    this.libraryDependencies = new ArrayList<>(libraryDependencies);
  }

  /**
   * @param group the collection to which this library belongs
   */
  void setGroup(String group) {
    this.group = group;
  }

  /**
   * @return the collections to which this library belongs
   */
  List<String> getGroups() {
    return Splitter.on(',').omitEmptyStrings().trimResults().splitToList(group);
  }

  public String getTransport() {
    return transport;
  }

  void setTransport(String transport) {
    this.transport = transport;
  }
  
  /**
   * Direct dependencies only. Do not attempt to load dependencies from Maven Central.
   */
  public synchronized void setResolved() {
    if (transitiveDependencies == null) {
      transitiveDependencies = directDependencies;
    }
  }

  /**
   * Returns the non-transitive dependencies of this library. Useful when a separate system such as
   * Maven will resolve the transitive dependencies later. 
   */
  public List<LibraryFile> getDirectDependencies() {
    return new ArrayList<>(directDependencies);
  }
    
  /**
   * A potentially long running operation that connects to the
   * local and remote Maven repos and returns a list of all library files in the
   * transitive dependency graph.
   */
  private synchronized List<LibraryFile> resolveDependencies() {
    List<LibraryFile> transitiveDependencies = new ArrayList<>();
        
    for (LibraryFile artifact : directDependencies) {
      artifact.updateVersion();
      MavenCoordinates coordinates = artifact.getMavenCoordinates();
      
      // include the artifact in its own list in case we can't find it in the repo
      transitiveDependencies.add(artifact);
      
      try {
        Collection<LibraryFile> dependencies =
            LibraryFactory.loadTransitiveDependencies(coordinates);
        transitiveDependencies.addAll(dependencies);
      } catch (CoreException ex) {
        logger.log(Level.SEVERE,
            "Could not load library " + artifact.getMavenCoordinates().toString(), ex);
      }
    }
    
    return resolveDuplicates(transitiveDependencies);
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
