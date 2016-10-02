package com.google.cloud.tools.eclipse.appengine.libraries;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.google.common.base.Preconditions;

/**
 * Represents a library that can be added to App Engine projects. E.g. AppEngine Endpoints library.
 */
public class Library {
  private static final String CONTAINER_PATH_PREFIX = "com.google.cloud.tools.eclipse.appengine.libraries";
  
  private String id;

  public Library(String id) {
    Preconditions.checkNotNull(id, "id cannot be null");
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public IPath getContainerPath() {
    return new Path(CONTAINER_PATH_PREFIX + "/" + id);
  }
}
