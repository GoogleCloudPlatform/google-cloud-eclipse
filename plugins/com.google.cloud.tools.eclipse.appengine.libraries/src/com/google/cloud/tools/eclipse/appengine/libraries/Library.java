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

package com.google.cloud.tools.eclipse.appengine.libraries;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.google.common.base.Preconditions;

/**
 * A library that can be added to App Engine projects, e.g. App Engine Endpoints library.
 *
 */
public final class Library {
  public static final String CONTAINER_PATH_PREFIX = "com.google.cloud.tools.eclipse.appengine.libraries";

  private String id;

  private String name;

  private URI siteUri;

  private boolean export = true;

  private List<LibraryFile> libraryFiles = Collections.emptyList();

  // library IDs of dependencies that are also need to be added to the build path along this library
  private List<String> libraryDependencies = new ArrayList<>();

  private LibraryRecommendation recommendation = LibraryRecommendation.OPTIONAL;

  public Library(String id) {
    Preconditions.checkNotNull(id, "id null");
    Preconditions.checkArgument(!id.isEmpty(), "id empty");
    this.id = id;
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

  public void setName(String name) {
    this.name = name;
  }

  public URI getSiteUri() {
    return siteUri;
  }

  public void setSiteUri(URI siteUri) {
    this.siteUri = siteUri;
  }

  public List<LibraryFile> getLibraryFiles() {
    return new ArrayList<>(libraryFiles);
  }

  /**
   * @param libraryFiles artifacts associated with this library, cannot be <code>null</code>
   */
  public void setLibraryFiles(List<LibraryFile> libraryFiles) {
    Preconditions.checkNotNull(libraryFiles);
    this.libraryFiles = new ArrayList<>(libraryFiles);
  }

  public boolean isExport() {
    return export;
  }

  public void setExport(boolean export) {
    this.export = export;
  }

  public List<String> getLibraryDependencies() {
    return new ArrayList<>(libraryDependencies);
  }

  /**
   * @param libraryDependencies list of libraryIds that are dependencies of this library and should be added to the 
   * classpath, cannot be <code>null</code>
   */
  public void setLibraryDependencies(List<String> libraryDependencies) {
    Preconditions.checkNotNull(libraryDependencies);
    this.libraryDependencies = new ArrayList<>(libraryDependencies);
  }

  /**
   * @param recommendation the level of recommendation for this library, cannot be <code>null</code>
   */
  public void setRecommendation(LibraryRecommendation recommendation) {
    Preconditions.checkNotNull(recommendation);
    this.recommendation = recommendation;
  }

  public LibraryRecommendation getRecommendation() {
    return recommendation;
  }
}
