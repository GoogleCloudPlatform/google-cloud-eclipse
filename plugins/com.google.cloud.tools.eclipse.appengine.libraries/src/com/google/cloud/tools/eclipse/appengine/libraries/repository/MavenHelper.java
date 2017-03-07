/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;

/**
 * Wrapper class around {@link MavenUtils} to enable mocking in unit tests.
 */
public class MavenHelper {

  public Artifact resolveArtifact(MavenCoordinates mavenCoordinates,
                                  IProgressMonitor monitor) throws CoreException {
    List<ArtifactRepository> repository = getRepository(mavenCoordinates);
    return MavenUtils.resolveArtifact(monitor,
                                      mavenCoordinates.getGroupId(),
                                      mavenCoordinates.getArtifactId(),
                                      mavenCoordinates.getType(),
                                      mavenCoordinates.getVersion(),
                                      mavenCoordinates.getClassifier(),
                                      repository);
  }

  private List<ArtifactRepository> getRepository(MavenCoordinates mavenCoordinates)
                                                                              throws CoreException {
    if (MavenCoordinates.MAVEN_CENTRAL_REPO.equals(mavenCoordinates.getRepository())) {
      // M2Eclipse will use the Maven Central repo in case null is used
      return null;
    } else {
      return Collections.singletonList(getCustomRepository(mavenCoordinates.getRepository()));
    }
  }

  private ArtifactRepository getCustomRepository(String repository) throws CoreException {
    try {
      URI repoUri = new URI(repository);
      if (!repoUri.isAbsolute()) {
        throw new CoreException(StatusUtil.error(this, Messages.getString("RepositoryUriNotAbsolute",
                                                                          repository)));
      }
      return MavenUtils.createRepository(repoUri.getHost(), repoUri.toString());
    } catch (URISyntaxException exception) {
      throw new CoreException(StatusUtil.error(this,
                                               Messages.getString("RepositoryUriInvalid",
                                                                  repository),
                                               exception));
    }
  }

  /**
   * Returns the folder to which the file described by <code>artifact</code> should be
   * downloaded.
   * <p>
   * The folder is created as follows:
   * <code>&lt;bundle_state_location&gt;/downloads/&lt;groupId&gt;/&lt;artifactId&gt;/&lt;version&gt;</code>
   * <p>
   * The <code>&lt;bundle_state_location&gt;</code> is determined by using the bundle containing
   * {@link MavenHelper}.
   * 
   * @return the location of the download folder, may not exist
   */
  public static IPath bundleStateBasedMavenFolder(MavenCoordinates mavenCoordinates) {
    Preconditions.checkArgument(
        !mavenCoordinates.getVersion().equals(MavenCoordinates.LATEST_VERSION));
    File downloadedSources =
        Platform.getStateLocation(FrameworkUtil.getBundle(MavenHelper.class))
        .append("downloads")
        .append(mavenCoordinates.getGroupId())
        .append(mavenCoordinates.getArtifactId())
        .append(mavenCoordinates.getVersion())
        .toFile();
    return new Path(downloadedSources.getAbsolutePath());
  }

  public boolean isArtifactLocallyAvailable(MavenCoordinates mavenCoordinates) {
    return MavenUtils.isArtifactAvailableLocally(mavenCoordinates.getGroupId(),
                                                 mavenCoordinates.getArtifactId(),
                                                 mavenCoordinates.getVersion(),
                                                 mavenCoordinates.getType(),
                                                 mavenCoordinates.getClassifier());
  }

}