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

import com.google.cloud.tools.eclipse.appengine.libraries.Messages;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Filter;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.io.FileDownloader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of {@link ILibraryRepositoryService} that relies on M2Eclipse to download the artifacts and store
 * them in the local Maven repository pointed to by M2Eclipse's M2_REPO variable.
 */
@Component
public class M2RepositoryService implements ILibraryRepositoryService {

  private static final String CLASSPATH_ATTRIBUTE_SOURCE_URL =
      "com.google.cloud.tools.eclipse.appengine.libraries.sourceUrl";

  private MavenHelper mavenHelper;
  private MavenCoordinatesClasspathAttributesTransformer transformer;

  @Override
  public IClasspathEntry getLibraryClasspathEntry(LibraryFile libraryFile) throws LibraryRepositoryServiceException {
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    Artifact artifact = resolveArtifact(mavenCoordinates);
    IClasspathAttribute[] libraryFileClasspathAttributes = getClasspathAttributes(libraryFile, artifact);
    URL sourceUrl = getSourceUrlFromUri(libraryFile.getSourceUri());
    return JavaCore.newLibraryEntry(new Path(artifact.getFile().getAbsolutePath()),
                                    getSourceLocation(mavenCoordinates, sourceUrl),
                                    null /*  sourceAttachmentRootPath */,
                                    getAccessRules(libraryFile.getFilters()),
                                    libraryFileClasspathAttributes,
                                    true /* isExported */);
  }

  private URL getSourceUrlFromUri(URI sourceUri) {
    try {
      if (sourceUri == null) {
        return null;
      } else {
        return sourceUri.toURL();
      }
    } catch (MalformedURLException | IllegalArgumentException e) {
      // should not cause error in the resolution process, we'll disregard it
      return null;
    }
  }

  @Override
  public IClasspathEntry rebuildClasspathEntry(IClasspathEntry classpathEntry) throws LibraryRepositoryServiceException {
    MavenCoordinates mavenCoordinates = transformer.createMavenCoordinates(classpathEntry.getExtraAttributes());
    Artifact artifact = resolveArtifact(mavenCoordinates);
    URL sourceUrl = getSourceUrlFromAttribute(classpathEntry.getExtraAttributes());
    return JavaCore.newLibraryEntry(new Path(artifact.getFile().getAbsolutePath()),
                                    getSourceLocation(mavenCoordinates, sourceUrl),
                                    null /*  sourceAttachmentRootPath */,
                                    classpathEntry.getAccessRules(),
                                    classpathEntry.getExtraAttributes(),
                                    true /* isExported */);
  }

  private URL getSourceUrlFromAttribute(IClasspathAttribute[] extraAttributes) {
    try {
      for (IClasspathAttribute iClasspathAttribute : extraAttributes) {
        if (CLASSPATH_ATTRIBUTE_SOURCE_URL.equals(iClasspathAttribute.getName())) {
          return new URL(iClasspathAttribute.getValue());
        }
      }
    } catch (MalformedURLException e) {
      // should not cause error in the resolution process, we'll disregard it
    }
    return null;
  }

  private Artifact resolveArtifact(MavenCoordinates mavenCoordinates) throws LibraryRepositoryServiceException {
    Preconditions.checkState(mavenHelper != null, "mavenHelper is null"); //$NON-NLS-1$
    try {
      List<ArtifactRepository> repository = getRepository(mavenCoordinates);

      return mavenHelper.resolveArtifact(null, mavenCoordinates, repository);
    } catch (CoreException ex) {
      throw new LibraryRepositoryServiceException(NLS.bind(Messages.ResolveArtifactError, mavenCoordinates), ex);
    }
  }

  private IClasspathAttribute[] getClasspathAttributes(LibraryFile libraryFile, Artifact artifact)
                                                                              throws LibraryRepositoryServiceException {
    try {
      List<IClasspathAttribute> attributes =
          transformer.createClasspathAttributes(artifact, libraryFile.getMavenCoordinates());
      if (libraryFile.isExport()) {
        attributes.add(UpdateClasspathAttributeUtil.createDependencyAttribute(true /* isWebApp */));
      } else {
        attributes.add(UpdateClasspathAttributeUtil.createNonDependencyAttribute());
      }
      if (libraryFile.getSourceUri() != null) {
        addUriAttribute(attributes, CLASSPATH_ATTRIBUTE_SOURCE_URL, libraryFile.getSourceUri());
      }
      if (libraryFile.getJavadocUri() != null) {
        addUriAttribute(attributes, IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, libraryFile.getJavadocUri());
      }
      return attributes.toArray(new IClasspathAttribute[0]);
    } catch (CoreException ex) {
      throw new LibraryRepositoryServiceException("Could not create classpath attributes", ex);
    }
  }

  private void addUriAttribute(List<IClasspathAttribute> attributes, String attributeName, URI uri) {
    try {
      attributes.add(JavaCore.newClasspathAttribute(attributeName, uri.toURL().toString()));
    } catch (MalformedURLException | IllegalArgumentException ex) {
      // disregard invalid URL
    }
  }

  private IPath getSourceLocation(MavenCoordinates mavenCoordinates, URL sourceUrl) {
    if (sourceUrl == null) {
      return getMavenSourceJarLocation(mavenCoordinates);
    } else {
      return getDownloadedSourceLocation(mavenCoordinates, sourceUrl);
    }
  }

  private IPath getDownloadedSourceLocation(MavenCoordinates mavenCoordinates, URL sourceUrl) {
    
    try {
      IPath downloadFolder = getDownloadedFilesFolder(mavenCoordinates);
      IPath path = new FileDownloader(downloadFolder).download(sourceUrl);
      return path;
    } catch (IOException e) {
      // source file is failed to download, this is not an error
      return null;
    }
  }

  /**
   * Returns the folder to which the a file corresponding to <code>mavenCoordinates</code> should be downloaded.
   * <p>
   * The folder is created as follows:
   * <code>&lt;bundle_state_location&gt;/downloads/&lt;groupId&gt;/&lt;artifactId&gt;/&lt;version&gt;</code>
   * @return the location of the download folder, may not exist
   */
  private IPath getDownloadedFilesFolder(MavenCoordinates mavenCoordinates) {
    File downloadedSources =
        Platform.getStateLocation(FrameworkUtil.getBundle(getClass()))
          .append("downloads")
          .append(mavenCoordinates.getGroupId())
          .append(mavenCoordinates.getArtifactId())
          .append(mavenCoordinates.getVersion()).toFile();
    return new Path(downloadedSources.getAbsolutePath());
  }

  private static IAccessRule[] getAccessRules(List<Filter> filters) {
    IAccessRule[] accessRules = new IAccessRule[filters.size()];
    int idx = 0;
    for (Filter filter : filters) {
      int accessRuleKind = filter.isExclude() ? IAccessRule.K_NON_ACCESSIBLE : IAccessRule.K_ACCESSIBLE;
      accessRules[idx++] = JavaCore.newAccessRule(new Path(filter.getPattern()), accessRuleKind);
    }
    return accessRules;
  }

  private ArtifactRepository getCustomRepository(String repository) throws LibraryRepositoryServiceException {
    try {
      URI repoUri = new URI(repository);
      if (!repoUri.isAbsolute()) {
        throw new LibraryRepositoryServiceException(NLS.bind(Messages.RepositoryUriNotAbsolute, repository));
      }
      return mavenHelper.createArtifactRepository(repoUri.getHost(), repoUri.toString());
    } catch (URISyntaxException exception) {
      throw new LibraryRepositoryServiceException(NLS.bind(Messages.RepositoryUriInvalid, repository), exception);
    } catch (CoreException exception) {
      throw new LibraryRepositoryServiceException(NLS.bind(Messages.RepositoryCannotBeLocated, repository), exception);
    }
  }

  private List<ArtifactRepository> getRepository(MavenCoordinates mavenCoordinates) throws LibraryRepositoryServiceException {
    if (MavenCoordinates.MAVEN_CENTRAL_REPO.equals(mavenCoordinates.getRepository())) {
      // M2Eclipse will use the Maven Central repo in case null is used
      return null;
    } else {
      return Collections.singletonList(getCustomRepository(mavenCoordinates.getRepository()));
    }
  }

  private IPath getMavenSourceJarLocation(MavenCoordinates mavenCoordinates) {
    try {
      MavenCoordinates sourceMavenCoordinates = new MavenCoordinates(mavenCoordinates);
      sourceMavenCoordinates.setClassifier("sources");
      Artifact artifact = resolveArtifact(sourceMavenCoordinates);
      return new Path(artifact.getFile().getAbsolutePath());
    } catch (LibraryRepositoryServiceException exception) {
      // source file failed to download, this is not an error
      return null;
    }
  }

  @Activate
  protected void activate() {
    mavenHelper = new M2EclipseMavenHelper();
    transformer = new MavenCoordinatesClasspathAttributesTransformer();
  }

  @VisibleForTesting
  protected interface MavenHelper {
    Artifact resolveArtifact(IProgressMonitor monitor, MavenCoordinates coordinates,
                             List<ArtifactRepository> repositories) throws CoreException;

    ArtifactRepository createArtifactRepository(String host, String string) throws CoreException;
  }

  /*
   * To make sure that mavenHelper is not null in production, ensure that the activate() method is called.
   */
  @VisibleForTesting
  void setMavenHelper(MavenHelper mavenHelper) {
    this.mavenHelper = mavenHelper;
  }

  @VisibleForTesting
  void setTransformer(MavenCoordinatesClasspathAttributesTransformer transformer) {
    this.transformer = transformer;
  }

  private static class M2EclipseMavenHelper implements MavenHelper {

    @Override
    public Artifact resolveArtifact(IProgressMonitor monitor,
                                    MavenCoordinates mavenCoordinates,
                                    List<ArtifactRepository> repositories) throws CoreException {
      return MavenUtils.resolveArtifact(null, mavenCoordinates.getGroupId(), mavenCoordinates.getArtifactId(),
                                        mavenCoordinates.getType(), mavenCoordinates.getVersion(),
                                        mavenCoordinates.getClassifier(), repositories);
    }

    @Override
    public ArtifactRepository createArtifactRepository(String id, String url) throws CoreException {
      return MavenUtils.createRepository(id, url);
    }
  }
}
