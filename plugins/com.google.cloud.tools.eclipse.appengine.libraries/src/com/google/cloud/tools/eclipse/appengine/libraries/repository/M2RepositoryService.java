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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of {@link ILibraryRepositoryService} that relies on M2Eclipse to download the artifacts and store
 * them in the local Maven repository pointed to by M2Eclipse's M2_REPO variable.
 */
@Component
public class M2RepositoryService implements ILibraryRepositoryService {

  private static final String CLASSPATH_ATTRIBUTE_REPOSITORY =
      "com.google.cloud.tools.eclipse.appengine.libraries.repository";
  private static final String CLASSPATH_ATTRIBUTE_GROUP_ID =
      "com.google.cloud.tools.eclipse.appengine.libraries.groupid";
  private static final String CLASSPATH_ATTRIBUTE_ARTIFACT_ID =
      "com.google.cloud.tools.eclipse.appengine.libraries.artifactId";
  private static final String CLASSPATH_ATTRIBUTE_TYPE =
      "com.google.cloud.tools.eclipse.appengine.libraries.type";
  private static final String CLASSPATH_ATTRIBUTE_VERSION =
      "com.google.cloud.tools.eclipse.appengine.libraries.version";
  private static final String CLASSPATH_ATTRIBUTE_CLASSIFIER =
      "com.google.cloud.tools.eclipse.appengine.libraries.classifier";

  private MavenHelper mavenHelper;

  @Override
  public IClasspathEntry getLibraryClasspathEntry(LibraryFile libraryFile) throws LibraryRepositoryServiceException {
    Artifact artifact = resolveArtifact(libraryFile.getMavenCoordinates());
    IClasspathAttribute[] libraryFileClasspathAttributes = getClasspathAttributes(libraryFile, artifact);
    return JavaCore.newLibraryEntry(new Path(artifact.getFile().getAbsolutePath()),
                                    getSourceLocation(libraryFile),
                                    null /*  sourceAttachmentRootPath */,
                                    getAccessRules(libraryFile.getFilters()),
                                    libraryFileClasspathAttributes,
                                    true /* isExported */);
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

  private static IClasspathAttribute[] getClasspathAttributes(LibraryFile libraryFile, Artifact artifact)
                                                                              throws LibraryRepositoryServiceException {
    try {
      List<IClasspathAttribute> attributes = getAttributesForMavenCoordinates(artifact,
                                                                              libraryFile.getMavenCoordinates());
      if (libraryFile.isExport()) {
        attributes.add(UpdateClasspathAttributeUtil.createDependencyAttribute(true /* isWebApp */));
      } else {
        attributes.add(UpdateClasspathAttributeUtil.createNonDependencyAttribute());
      }
      return attributes.toArray(new IClasspathAttribute[0]);
    } catch (CoreException ex) {
      throw new LibraryRepositoryServiceException("Could not create classpath attributes", ex);
    }
  }

  private static List<IClasspathAttribute> getAttributesForMavenCoordinates(Artifact artifact,
                                                                            MavenCoordinates mavenCoordinates) {
    List<IClasspathAttribute> attributes = Lists.newArrayList(
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_REPOSITORY, mavenCoordinates.getRepository()),
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_GROUP_ID, artifact.getGroupId()),
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_ARTIFACT_ID, artifact.getArtifactId()),
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_TYPE, artifact.getType()),
        JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_VERSION, artifact.getVersion())
        );
    if (artifact.getClassifier() != null) {
      attributes.add(JavaCore.newClasspathAttribute(CLASSPATH_ATTRIBUTE_CLASSIFIER, artifact.getClassifier()));
    }
    return attributes;
  }

  private IPath getSourceLocation(LibraryFile libraryFile) {
    if (libraryFile.getSourceUri() == null) {
      return getSourceJarLocation(libraryFile.getMavenCoordinates());
    } else {
      // download the file and return path to it
      // TODO https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/800
      return new Path("/downloaded/source/file"); //$NON-NLS-1$
    }
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

  private IPath getSourceJarLocation(MavenCoordinates mavenCoordinates) {
    return new Path("/path/to/source/jar/file/in/m2_repo/" + mavenCoordinates.getArtifactId() + "." + mavenCoordinates.getType()); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Activate
  protected void activate() {
    mavenHelper = new M2EclipseMavenHelper();
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
