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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.jobs.JobUtil;
import com.google.cloud.tools.eclipse.util.jobs.JobUtil.ContextParameterSupplier;
import java.io.File;
import java.io.IOException;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class M2SourceAttachmentDownloaderJobTest {

  private static final String CONTAINER_PATH = "/containerPath";
  private static final String LIBRARY_PATH = "/libraryPath/library.jar";

  @Rule
  public TestProjectCreator testProjectCreator =
    new TestProjectCreator().withClasspathContainerPath(CONTAINER_PATH);
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Mock
  private MavenHelper mavenHelper;
  @Mock
  private Artifact sourceArtifact;
  @Mock
  private Artifact binaryArtifact;
  @Mock
  private LibraryClasspathContainerSerializer serializer;
  private IClasspathEntry library;
  private LibraryClasspathContainer container;

  @Before
  public void setUp() {
    library = JavaCore.newLibraryEntry(new Path(LIBRARY_PATH), null, null);
    container = new LibraryClasspathContainer(new Path(CONTAINER_PATH),
                                              "Test Container",
                                              new IClasspathEntry[]{ library });
  }
  
  @Test
  public void testRun_attachesSources() throws InterruptedException, CoreException, IOException {
    File sourceArtifactFile = temporaryFolder.newFile("testSourceArtifact");
    when(sourceArtifact.getFile()).thenReturn(sourceArtifactFile);
    when(mavenHelper.getMavenSourceJarLocation(any(Artifact.class), any(IProgressMonitor.class)))
      .thenReturn(new Path(sourceArtifactFile.getAbsolutePath()));
    when(binaryArtifact.getGroupId()).thenReturn("groupId");
    when(binaryArtifact.getArtifactId()).thenReturn("artifactId");
    IJavaProject javaProject = testProjectCreator.getJavaProject();
    JavaCore.setClasspathContainer(new Path(CONTAINER_PATH),
                                   new IJavaProject[]{ javaProject },
                                   new IClasspathContainer[]{ container },
                                   new NullProgressMonitor());
    M2SourceAttachmentDownloaderJob job = createJob(binaryArtifact, javaProject);
    job.schedule();
    job.join();
    assertThat(job.getResult(), is(Status.OK_STATUS));
    boolean libraryFound = false;
    for (IClasspathEntry iClasspathEntry : javaProject.getResolvedClasspath(false)) {
      if (iClasspathEntry.getPath().equals(new Path(LIBRARY_PATH))) {
        libraryFound = true;
        assertThat(iClasspathEntry.getSourceAttachmentPath().toFile().getAbsolutePath(),
                   is(sourceArtifactFile.getAbsolutePath()));
      }
    }
    assertTrue(libraryFound);
  }

  private M2SourceAttachmentDownloaderJob createJob(final Artifact artifact,
      final IJavaProject javaProject) {
    return JobUtil.createJob(M2SourceAttachmentDownloaderJob.class, new ContextParameterSupplier() {
      @Override
      public void setParameters(IEclipseContext context) {
        context.set(IJavaProject.class, javaProject);
        context.set(Artifact.class, sourceArtifact);
        context.set(MavenHelper.class, mavenHelper);
        context.set(LibraryClasspathContainerSerializer.class, serializer);
        context.set(M2SourceAttachmentDownloaderJob.PARAM_CLASSPATHENTRY_PATH,
                    new Path(LIBRARY_PATH));
      }
    });
  }
}
