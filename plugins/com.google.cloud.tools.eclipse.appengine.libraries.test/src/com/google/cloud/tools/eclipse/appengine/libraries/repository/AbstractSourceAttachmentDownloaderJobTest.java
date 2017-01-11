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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

public abstract class AbstractSourceAttachmentDownloaderJobTest {

  private static final String CONTAINER_PATH = "/containerPath";
  protected static final String LIBRARY_PATH = "/libraryPath/library.jar";

  @Rule
  public TestProjectCreator testProjectCreator =
      new TestProjectCreator().withClasspathContainerPath(CONTAINER_PATH);
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Mock
  private LibraryClasspathContainerSerializer serializer;
  private IClasspathEntry library;
  private LibraryClasspathContainer container;

  protected void setUp() throws Exception {
    library = JavaCore.newLibraryEntry(new Path(LIBRARY_PATH), null, null);
    container = new LibraryClasspathContainer(new Path(CONTAINER_PATH),
                                              "Test Container",
                                              new IClasspathEntry[]{ library });
  }

  @Test
  public void testRun() throws JavaModelException, InterruptedException {
    IJavaProject javaProject = testProjectCreator.getJavaProject();
    JavaCore.setClasspathContainer(new Path(CONTAINER_PATH),
                                   new IJavaProject[]{ javaProject },
                                   new IClasspathContainer[]{ container },
                                   new NullProgressMonitor());
    Job job = createJob(javaProject, serializer);
    job.schedule();
    job.join();
    assertThat(job.getResult(), is(Status.OK_STATUS));
    boolean libraryFound = false;
    for (IClasspathEntry iClasspathEntry : javaProject.getResolvedClasspath(false)) {
      if (iClasspathEntry.getPath().equals(new Path(LIBRARY_PATH))) {
        libraryFound = true;
        assertThat(iClasspathEntry.getSourceAttachmentPath().toFile().getAbsolutePath(),
                   is(getSourceArtifactFilePath()));
      }
    }
    assertTrue(libraryFound);

  }

  protected abstract String getSourceArtifactFilePath();

  protected abstract Job createJob(IJavaProject javaProject, LibraryClasspathContainerSerializer serializer);
}
