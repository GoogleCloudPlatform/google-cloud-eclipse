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

package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.util.io.FileDownloader;
import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of {@link ILibraryRepositoryService} that relies on M2Eclipse to download the
 * artifacts and store them in the local Maven repository pointed to by M2Eclipse's M2_REPO
 * variable.
 * <p>
 * In case <code>libraryFile.getSourceUri()</code> is null, M2Eclipse resolves the source
 * artifact by using the "sources" classifier with the binary artifact's {@link MavenCoordinates}.
 */
@Component
public class M2RepositoryService implements ILibraryRepositoryService {

  @Override
  public Artifact resolveArtifact(LibraryFile libraryFile, IProgressMonitor monitor)
                                                                              throws CoreException {
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    return MavenHelper.resolveArtifact(mavenCoordinates, monitor);
  }

  @Override
  public IPath resolveSourceArtifact(LibraryFile libraryFile, String versionHint,
      IProgressMonitor monitor) {
    
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    MavenCoordinates.Builder sourceCoordinates = mavenCoordinates.toBuilder();
    if (!Strings.isNullOrEmpty(versionHint)) {
      sourceCoordinates.setVersion(versionHint);
    }
    sourceCoordinates.setClassifier("sources");
    if (libraryFile.getSourceUri() == null) {
      try {
        File artifactFile = MavenHelper.resolveArtifact(sourceCoordinates.build(), monitor).getFile();
        return new Path(artifactFile.getAbsolutePath());
      } catch (CoreException ex) {
        // not all artifacts have sources. E.g. com.google.appengine:appengine does not.
        return null;
      }
    } else {
      try {
        URL sourceUrl = libraryFile.getSourceUri().toURL();
        return getDownloadedSourceLocation(sourceCoordinates.build(), sourceUrl, monitor);
      } catch (MalformedURLException | IllegalArgumentException ex) {
        return null;
      }
    }
  }

  private static IPath getDownloadedSourceLocation(MavenCoordinates mavenCoordinates, URL sourceUrl,
                                                   IProgressMonitor monitor) {
    try {
      IPath downloadFolder = MavenHelper.bundleStateBasedMavenFolder(mavenCoordinates);
      return new FileDownloader(downloadFolder).download(sourceUrl, monitor);
    } catch (IOException ex) {
      // source file failed to download; this is not an error
      return null;
    }
  }

  @Activate
  protected void activate() {  // Necessary to instantiate the class.
  }
}
