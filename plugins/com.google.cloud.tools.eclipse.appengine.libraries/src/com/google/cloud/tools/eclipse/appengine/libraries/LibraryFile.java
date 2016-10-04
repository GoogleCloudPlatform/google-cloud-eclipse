package com.google.cloud.tools.eclipse.appengine.libraries;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * A jar file that is downloaded from the location defined by {@link MavenCoordinates}. It can have associated
 * filters to control visibility of classes and packages contained in the jar file.
 *
 */
public class LibraryFile {

  private List<Filter> exclusionFilters = Collections.emptyList();
  private List<Filter> inclusionFilters = Collections.emptyList();
  private MavenCoordinates mavenCoordinates;
  private URI javadocUri;
  private URI sourceUri;

  public LibraryFile(MavenCoordinates mavenCoordinates) {
    Preconditions.checkNotNull(mavenCoordinates, "mavenCoordinates is null");
    this.mavenCoordinates = mavenCoordinates;
  }

  public MavenCoordinates getMavenCoordinates() {
    return mavenCoordinates;
  }

  public List<Filter> getExclusionFilters() {
    return new ArrayList<>(exclusionFilters);
  }

  public void setExclusionFilters(List<Filter> exclusionFilters) {
    if (exclusionFilters != null) {
      this.exclusionFilters = new ArrayList<>(exclusionFilters);
    }
  }

  public List<Filter> getInclusionFilters() {
    return new ArrayList<>(inclusionFilters);
  }

  public void setInclusionFilters(List<Filter> inclusionFilters) {
    if (inclusionFilters != null) {
      this.inclusionFilters = new ArrayList<>(inclusionFilters);
    }
  }

  public URI getJavadocUri() {
    return javadocUri;
  }

  public void setJavadocUri(URI javadocUri) {
    this.javadocUri = javadocUri;
  }

  public URI getSourceUri() {
    return sourceUri;
  }

  public void setSourceUri(URI sourceUri) {
    this.sourceUri = sourceUri;
  }
}
