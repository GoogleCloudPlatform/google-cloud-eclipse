package com.google.cloud.tools.eclipse.appengine.libraries;

import java.util.List;

public class LibraryFile {

  private List<ExclusionFilter> exclusionFilters;
  private List<InclusionFilter> inclusionFilters;
  private MavenCoordinates mavenCoordinates;

  public void setExclusionFilters(List<ExclusionFilter> exclusionFilters) {
    this.exclusionFilters = exclusionFilters;
  }

  public void setInclusionFilters(List<InclusionFilter> inclusionFilters) {
    this.inclusionFilters = inclusionFilters;
  }

  public void setMavenCoordinates(MavenCoordinates mavenCoordinates) {
    this.mavenCoordinates = mavenCoordinates;
  }

}
