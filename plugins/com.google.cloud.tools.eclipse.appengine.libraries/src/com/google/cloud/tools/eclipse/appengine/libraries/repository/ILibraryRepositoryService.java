package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import org.eclipse.core.runtime.IPath;

import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;

/**
 * Service interface for obtaining local paths for artifacts described by {@link MavenCoordinates}
 */
public interface ILibraryRepositoryService {

  /**
   * @return a path that points to a local file corresponding to the artifact described by <code>mavenCoordinates</code>
   */
  public IPath getJarLocation(MavenCoordinates mavenCoordinates);

  /**
   * @return a path that points to a local file corresponding to the source artifact described
   * by <code>mavenCoordinates</code>
   */
  public IPath getSourceJarLocation(MavenCoordinates mavenCoordinates);

}
