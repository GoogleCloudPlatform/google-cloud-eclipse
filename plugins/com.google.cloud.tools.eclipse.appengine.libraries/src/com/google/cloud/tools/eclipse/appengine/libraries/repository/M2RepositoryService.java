package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;

/**
 * Implementation of {@link ILibraryRepositoryService} that relies on M2Eclipse to download the artifacts and store
 * them in the local Maven repository pointed to by M2Eclipse's M2_REPO variable.
 */
// FIXME For now this class is just a mock, to be implemented soon
public class M2RepositoryService implements ILibraryRepositoryService {

  @Override
  public IPath getJarLocation(MavenCoordinates mavenCoordinates) {
    return new Path("/path/to/jar/file/in/m2_repo/" + mavenCoordinates.getArtifactId() + "." + mavenCoordinates.getType());
  }

  @Override
  public IPath getSourceJarLocation(MavenCoordinates mavenCoordinates) {
    return new Path("/path/to/source/jar/file/in/m2_repo/" + mavenCoordinates.getArtifactId() + "." + mavenCoordinates.getType());
  }

}
