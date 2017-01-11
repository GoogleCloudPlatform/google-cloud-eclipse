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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class M2RepositoryServiceTest {

  private static final String FAKE_PATH = "/fake/path";
  private static final String FAKE_SOURCE_PATH = "/fake/source/path";

  private TestJob testJob;
  private M2RepositoryService m2RepositoryService;
  @Mock private MavenHelper mavenHelper;
  @Mock private SourceDownloaderJobFactory sourceDownloaderJobFactory;
  @Rule public TestProjectCreator testProjectCreator = new TestProjectCreator();

  private MavenCoordinates mavenCoordinates;
  
  @Before
  public void setUp() {
    mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    testJob = new TestJob();
    when(sourceDownloaderJobFactory.createSourceDownloaderJob(any(IJavaProject.class), 
                                                              any(Artifact.class),
                                                              any(IPath.class),
                                                              any(URL.class))).thenReturn(testJob);
    
    m2RepositoryService = new M2RepositoryService();
    m2RepositoryService.setMavenHelper(mavenHelper);
    m2RepositoryService.setSourceDownloaderJobFactory(sourceDownloaderJobFactory);
  }
  
  @Test(expected = LibraryRepositoryServiceException.class)
  public void getLibraryClasspathEntry_errorInArtifactResolution() throws Exception {
    when(mavenHelper.resolveArtifact(any(MavenCoordinates.class), any(IProgressMonitor.class)))
      .thenThrow(testCoreException());

    m2RepositoryService.getLibraryClasspathEntry(testProjectCreator.getJavaProject(),
                                                 new LibraryFile(mavenCoordinates),
                                                 new NullProgressMonitor());
  }

  @Test
  public void getLibraryClasspathEntry_customRepositoryURI() throws Exception {
    Artifact artifact = getMockArtifactWithJarPath();
    when(mavenHelper.resolveArtifact(any(MavenCoordinates.class),
                                     any(IProgressMonitor.class))).thenReturn(artifact);
    mavenCoordinates.setRepository("http://example.com");
    IPath jarLocation =
        m2RepositoryService.getLibraryClasspathEntry(testProjectCreator.getJavaProject(),
                                                     new LibraryFile(mavenCoordinates),
                                                     new NullProgressMonitor())
                           .getPath();

    assertThat(jarLocation.toOSString(), is(FAKE_PATH));
  }

  @Test
  public void getLibraryClasspathEntry_withJavadoc() throws Exception {
    Artifact artifact = getMockArtifactWithJarPath();
    when(mavenHelper.resolveArtifact(any(MavenCoordinates.class),
                                     any(IProgressMonitor.class))).thenReturn(artifact);
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setJavadocUri(new URI("http://example.com/javadoc"));
    IClasspathAttribute[] attributes =
        m2RepositoryService.getLibraryClasspathEntry(testProjectCreator.getJavaProject(),
                                                     libraryFile,
                                                     new NullProgressMonitor()).getExtraAttributes();

    assertThat(attributes, hasItemInArray(allOf(hasProperty("name",
                                                            is(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME)),
                                                hasProperty("value",
                                                            is("http://example.com/javadoc")))));
  }

  @Test
  public void getLibraryClasspathEntry_withInvalidSourceUriSourceResolutionStillHappens()
                                                                                  throws Exception {
    Artifact artifact = getMockArtifactWithJarPath();
    when(mavenHelper.resolveArtifact(any(MavenCoordinates.class),
                                     any(IProgressMonitor.class))).thenReturn(artifact);
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setSourceUri(new URI("foo/bar"));
    m2RepositoryService.getLibraryClasspathEntry(testProjectCreator.getJavaProject(),
                                                 libraryFile,
                                                 new NullProgressMonitor());
    testJob.join();
    assertTrue(testJob.executed);
  }

  @Test
  public void getLibraryClasspathEntry_withoutJavaProjectNoBackgroundJobIsExecuted() throws Exception {
    Artifact artifact = getMockArtifactWithJarPath();
    when(mavenHelper.resolveArtifact(any(MavenCoordinates.class),
                                     any(IProgressMonitor.class))).thenReturn(artifact);
    when(mavenHelper.getMavenSourceJarLocation(any(Artifact.class),
                                               any(IProgressMonitor.class)))
                    .thenReturn(new Path(FAKE_SOURCE_PATH));
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    IClasspathEntry classpathEntry =
        m2RepositoryService.getLibraryClasspathEntry(null /* javaProject */,
                                                     libraryFile,
                                                     new NullProgressMonitor());
    testJob.join();
    assertFalse(testJob.executed);
    assertThat(classpathEntry.getSourceAttachmentPath().toFile().getAbsolutePath(),
               is(FAKE_SOURCE_PATH));
  }

  @Test
  public void rebuildClasspathEntry() throws Exception {
    Artifact artifact = getMockArtifactWithJarPath();
    when(mavenHelper.resolveArtifact(any(MavenCoordinates.class),
                                     any(IProgressMonitor.class))).thenReturn(artifact);
    IClasspathEntry classpathEntry = getClasspathEntry("groupId", "artifactId");
    m2RepositoryService.rebuildClasspathEntry(testProjectCreator.getJavaProject(),
                                              classpathEntry,
                                              new NullProgressMonitor());
    testJob.join();
    assertTrue(testJob.executed);
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void rebuildClasspathEntry_errorInArtifactResolution() throws Exception {
    when(mavenHelper.resolveArtifact(any(MavenCoordinates.class), any(IProgressMonitor.class)))
      .thenThrow(testCoreException());

    IClasspathEntry classpathEntry = getClasspathEntry("groupId", "artifactId");
    m2RepositoryService.rebuildClasspathEntry(testProjectCreator.getJavaProject(),
                                              classpathEntry,
                                              new NullProgressMonitor());
  }

  @Test(expected = IllegalStateException.class)
  public void testMavenHelperMustBeSet() throws LibraryRepositoryServiceException {
    new M2RepositoryService().getLibraryClasspathEntry(testProjectCreator.getJavaProject(),
                                                       new LibraryFile(mavenCoordinates),
                                                       new NullProgressMonitor());
  }

  @Test(expected = IllegalStateException.class)
  public void testTransformerMustBeSet() throws LibraryRepositoryServiceException {
    M2RepositoryService service = new M2RepositoryService();
    service.setMavenHelper(mavenHelper);
    service.getLibraryClasspathEntry(testProjectCreator.getJavaProject(),
                                     new LibraryFile(mavenCoordinates),
                                     new NullProgressMonitor());
  }

  @Test(expected = IllegalStateException.class)
  public void testSourceDownloadJobFactoryMustBeSet() throws LibraryRepositoryServiceException {
    M2RepositoryService service = new M2RepositoryService();
    service.setMavenHelper(mavenHelper);
    service.getLibraryClasspathEntry(testProjectCreator.getJavaProject(),
                                     new LibraryFile(mavenCoordinates),
                                     new NullProgressMonitor());
  }

  @Test(expected = TestRuntimeException.class)
  public void testActivateSetsMavenHelper() throws LibraryRepositoryServiceException {
    M2RepositoryService m2RepositoryService = new M2RepositoryService();
    m2RepositoryService.activate();
    MavenCoordinates mavenCoordinates = mock(MavenCoordinates.class);
    // TestRuntimeException is thrown to verify that it was thrown because of the mock setup
    when(mavenCoordinates.getRepository()).thenThrow(new TestRuntimeException());
    m2RepositoryService.getLibraryClasspathEntry(testProjectCreator.getJavaProject(),
                                                 new LibraryFile(mavenCoordinates),
                                                 new NullProgressMonitor());
  }

  private Artifact getMockArtifactWithJarPath() {
    Artifact artifact = mock(Artifact.class);
    File file = new File(FAKE_PATH);
    when(artifact.getFile()).thenReturn(file );
    return artifact;
  }

  private IClasspathEntry getClasspathEntry(String groupId, String artifactId) {
    List<IClasspathAttribute> attributes =
        MavenCoordinatesHelper.createClasspathAttributes(new MavenCoordinates(groupId, artifactId),
                                                         "1.0.0");
    return JavaCore.newLibraryEntry(new Path(FAKE_PATH), null, null, new IAccessRule[0],
                                    attributes.toArray(new IClasspathAttribute[0]), true);
  }

  private CoreException testCoreException() {
    return new CoreException(StatusUtil.error(this, "Test exception"));
  }

  @SuppressWarnings("serial")
  private static class TestRuntimeException extends RuntimeException {
  }

  private static class TestJob extends Job {
    private boolean executed = false;
    private TestJob() {
      super("testJob");
    }
    @Override
    protected IStatus run(IProgressMonitor monitor) {
      executed = true;
      return Status.OK_STATUS;
    }    
  }
}
