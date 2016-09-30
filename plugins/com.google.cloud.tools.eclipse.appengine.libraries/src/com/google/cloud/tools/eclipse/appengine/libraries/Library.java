package com.google.cloud.tools.eclipse.appengine.libraries;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.google.common.base.Preconditions;

/**
 * Represents a library that can be added to App Engine projects, e.g. AppEngine Endpoints library.
 *
 */
public class Library {
  private static final String CONTAINER_PATH_PREFIX = "com.google.cloud.tools.eclipse.appengine.libraries";
  
  private String id;

  private String name;

  private URI siteUri;

  private List<LibraryFile> libraryFiles = Collections.emptyList();

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
    return libraryFiles;
  }

  public void setLibraryFiles(List<LibraryFile> libraryFiles) {
    if (libraryFiles != null) {
      this.libraryFiles = new LinkedList<>(libraryFiles);
    }
  }

  public String getDescription() {
    if (getName() == null || getName().isEmpty()) {
      return getId();
    } else {
      return getName();
    }
  }
}
