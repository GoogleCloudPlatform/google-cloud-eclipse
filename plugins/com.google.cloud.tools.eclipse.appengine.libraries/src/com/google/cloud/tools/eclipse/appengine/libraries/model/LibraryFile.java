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

import com.google.cloud.tools.eclipse.util.ArtifactRetriever;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * A jar file that is downloaded from the location defined by {@link MavenCoordinates}. It can have
 * associated filters to control visibility of classes and packages contained in the jar file.
 */
public class LibraryFile implements Comparable<LibraryFile> {

  private List<Filter> filters = Collections.emptyList();
  private MavenCoordinates mavenCoordinates;
  private URI javadocUri;
  private URI sourceUri;
  private boolean export = true;
  
  /** Version has been checked against Maven Central **/
  private boolean fixedVersion = false;
  
  /** whether to stick to specified version even if there's a later version in Maven central **/
  private boolean pinned = false; 

  public LibraryFile(MavenCoordinates mavenCoordinates) {
    Preconditions.checkNotNull(mavenCoordinates, "mavenCoordinates is null");
    this.mavenCoordinates = mavenCoordinates;
  }

  public MavenCoordinates getMavenCoordinates() {
    return mavenCoordinates;
  }

  public List<Filter> getFilters() {
    return new ArrayList<>(filters);
  }

  void setFilters(List<Filter> filters) {
    if (filters != null) {
      this.filters = new ArrayList<>(filters);
    }
  }

  public URI getJavadocUri() {
    return javadocUri;
  }

  void setJavadocUri(URI javadocUri) {
    this.javadocUri = javadocUri;
  }

  public URI getSourceUri() {
    return sourceUri;
  }

  @VisibleForTesting
  public void setSourceUri(URI sourceUri) {
    this.sourceUri = sourceUri;
  }

  public boolean isExport() {
    return export;
  }

  void setExport(boolean export) {
    this.export = export;
  }
  
  @Override
  public String toString() {
    return mavenCoordinates.toStringCoordinates();
  }

  @Override
  public int compareTo(LibraryFile other) {
    return this.mavenCoordinates.toStringCoordinates()
        .compareTo(other.mavenCoordinates.toStringCoordinates());
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof LibraryFile)) {
      return false;
    }
    return this.mavenCoordinates.toStringCoordinates()
        .equals((((LibraryFile) other).mavenCoordinates.toStringCoordinates()));
  }
  
  @Override
  public int hashCode() {
    return this.mavenCoordinates.toStringCoordinates().hashCode();
  }

  /**
   * Check Maven Central to find the latest release version of this artifact.
   * This check is made at most once. Subsequent checks are no-ops.
   */
  void updateVersion() {
    if (!fixedVersion && !isPinned()) {
      ArtifactVersion remoteVersion = ArtifactRetriever.DEFAULT.getBestVersion(
          mavenCoordinates.getGroupId(), mavenCoordinates.getArtifactId());
      if (remoteVersion != null) {
        DefaultArtifactVersion localVersion = new DefaultArtifactVersion(mavenCoordinates.getVersion());
        if (remoteVersion.compareTo(localVersion) > 0) {
          String updatedVersion = remoteVersion.toString(); 
          mavenCoordinates = mavenCoordinates.toBuilder().setVersion(updatedVersion).build();
        }
      }
      fixedVersion = true;
    }
  }

  public boolean isPinned() {
    return pinned;
  }

  @VisibleForTesting
  public void setPinned(boolean pinned) {
    this.pinned = pinned;
  }

}
