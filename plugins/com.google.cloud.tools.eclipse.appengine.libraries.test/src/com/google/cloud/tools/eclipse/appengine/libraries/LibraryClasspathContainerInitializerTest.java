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

package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * This test relies on the {@link TestAppEngineLibraryContainerInitializer} defined in the
 * fragment.xml for <code>TEST_CONTAINER_PATH</code>. When the test is launched, the Platform will
 * try to initialize the container defined for the test project (field <code>testProject</code>),
 * but due to the empty implementation of
 * {@link TestAppEngineLibraryContainerInitializer#initialize(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)}
 * the container will remain unresolved.
 * Then the {@link LibraryClasspathContainerInitializer} instance created in the test methods will
 * initialize the container and then it will be verified.
 * <p>
 * This approach is required by the fact that the production
 * {@link LibraryClasspathContainerInitializer} is defined in the host project's plugin.xml and it
 * is not possible to remove/override it. Thus if we used the same container path prefix as in
 * production, the initializer defined in the plugin.xml would be called on the test containers
 * interfering with the test code.
 */
@RunWith(MockitoJUnitRunner.class)
public class LibraryClasspathContainerInitializerTest {

  private static final String NON_EXISTENT_FILE = "/non/existent/file";
  private static final String TEST_CONTAINER_PATH = "test.appengine.libraries";
  private static final String TEST_LIBRARY_ID = "libraryId";
  private static final String TEST_LIBRARY_PATH = TEST_CONTAINER_PATH + "/" + TEST_LIBRARY_ID;

  @Mock private ILibraryClasspathContainerResolverService resolverService;
  @Mock private LibraryClasspathContainerSerializer serializer;

  @Rule
  public ThreadDumpingWatchdog watchdog = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Rule
  public TestProjectCreator testProject = new TestProjectCreator()
      .withFacetVersions(JavaFacet.VERSION_1_7).withClasspathContainerPath(TEST_LIBRARY_PATH);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test(expected = CoreException.class)
  public void testInitialize_shouldFailIfContainerPathConsistsOfOneSegment() throws CoreException {
    LibraryClasspathContainerInitializer containerInitializer =
        new LibraryClasspathContainerInitializer(TEST_CONTAINER_PATH, serializer, resolverService);
    containerInitializer.initialize(new Path("single.segment.id"), testProject.getJavaProject());
  }

  @Test(expected = CoreException.class)
  public void testInitialize_shouldFailIfContainerPathConsistsOfThreeSegments() throws CoreException {
    LibraryClasspathContainerInitializer containerInitializer =
        new LibraryClasspathContainerInitializer(TEST_CONTAINER_PATH, serializer, resolverService);
    containerInitializer.initialize(new Path("first.segment/second.segment/third.segment"),
                                    testProject.getJavaProject());
  }

  @Test(expected = CoreException.class)
  public void testInitialize_shouldFailIfContainerPathHasWrongFirstSegment() throws CoreException {
    LibraryClasspathContainerInitializer containerInitializer =
        new LibraryClasspathContainerInitializer(TEST_CONTAINER_PATH, serializer, resolverService);
    containerInitializer.initialize(new Path("first.segment/second.segment"), testProject.getJavaProject());
  }

  @Test
  public void testInitialize_ifSerializerReturnsNullResolverServiceIsCalled() throws IOException,
                                                                                     CoreException {
    when(serializer.loadContainer(any(IJavaProject.class), any(IPath.class))).thenReturn(null);
    LibraryClasspathContainerInitializer containerInitializer =
        new LibraryClasspathContainerInitializer(TEST_CONTAINER_PATH, serializer, resolverService);
    containerInitializer.initialize(new Path(TEST_CONTAINER_PATH + "/second.segment"),
                                    testProject.getJavaProject());
    verifyContainerResolvedFromScratch();
  }

  @Test(expected = CoreException.class)
  public void testInitialize_deserializingContainerThrowsError() throws IOException, CoreException {
    doThrow(new IOException("test exception"))
      .when(serializer).loadContainer(any(IJavaProject.class), any(IPath.class));
    LibraryClasspathContainerInitializer containerInitializer =
        new LibraryClasspathContainerInitializer(TEST_CONTAINER_PATH, serializer, resolverService);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH), testProject.getJavaProject());
  }

  @Test
  public void testInitialize_ifArtifactJarPathIsInvalidContainerResolvedFromScratch() throws CoreException,
                                                                                             IOException {
    assertFalse(new File(NON_EXISTENT_FILE).exists());

    IClasspathEntry entry = mock(IClasspathEntry.class);
    when(entry.getPath()).thenReturn(new Path(NON_EXISTENT_FILE));
    IClasspathEntry[] entries = new IClasspathEntry[]{ entry };
    LibraryClasspathContainer container = mock(LibraryClasspathContainer.class);
    when(container.getClasspathEntries()).thenReturn(entries);
    when(serializer.loadContainer(any(IJavaProject.class), any(IPath.class))).thenReturn(container);

    LibraryClasspathContainerInitializer containerInitializer =
        new LibraryClasspathContainerInitializer(TEST_CONTAINER_PATH, serializer, resolverService);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH), testProject.getJavaProject());

    verifyContainerResolvedFromScratch();
  }

  @Test
  public void testInitialize_ifSourceArtifactJarPathInvalidContainerResolvedFromScratch() throws CoreException,
                                                                                                 IOException {
    File artifactFile = temporaryFolder.newFile();
    assertFalse(new File(NON_EXISTENT_FILE).exists());

    IClasspathEntry entry = mock(IClasspathEntry.class);
    when(entry.getPath()).thenReturn(new Path(artifactFile.getAbsolutePath()));
    when(entry.getSourceAttachmentPath()).thenReturn(new Path(NON_EXISTENT_FILE));
    IClasspathEntry[] entries = new IClasspathEntry[]{ entry };
    LibraryClasspathContainer container = mock(LibraryClasspathContainer.class);
    when(container.getClasspathEntries()).thenReturn(entries);
    when(serializer.loadContainer(any(IJavaProject.class), any(IPath.class))).thenReturn(container);

    LibraryClasspathContainerInitializer containerInitializer =
        new LibraryClasspathContainerInitializer(TEST_CONTAINER_PATH, serializer, resolverService);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH), testProject.getJavaProject());

    verifyContainerResolvedFromScratch();
  }

  @Test
  public void testInitialize_ifSourcePathIsNullContainerIsNotResolvedAgain() throws CoreException, IOException {
    File artifactFile = temporaryFolder.newFile();

    IClasspathEntry entry = JavaCore.newLibraryEntry(new Path(artifactFile.getAbsolutePath()), null, null);
    IClasspathEntry[] entries = new IClasspathEntry[]{ entry };
    LibraryClasspathContainer container = mock(LibraryClasspathContainer.class);
    when(container.getClasspathEntries()).thenReturn(entries);
    when(serializer.loadContainer(any(IJavaProject.class), any(IPath.class))).thenReturn(container);

    LibraryClasspathContainerInitializer containerInitializer =
        new LibraryClasspathContainerInitializer(TEST_CONTAINER_PATH, serializer, resolverService);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH), testProject.getJavaProject());
    testProject.getJavaProject().getRawClasspath();
    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, Matchers.greaterThan(2));
    for (IClasspathEntry resolvedEntry : resolvedClasspath) {
      if (resolvedEntry.getPath().toOSString().equals(artifactFile.getAbsolutePath())) {
        verifyContainerWasNotResolvedFromScratch();
        return;
      }
    }
    fail("classpath entry not found");
  }

  @Test
  public void testInitialize_ifSourcePathIsValidContainerIsNotResolvedAgain() throws CoreException, IOException {
    File artifactFile = temporaryFolder.newFile();
    File sourceArtifactFile = temporaryFolder.newFile();

    IClasspathEntry entry = JavaCore.newLibraryEntry(new Path(artifactFile.getAbsolutePath()),
                                                     new Path(sourceArtifactFile.getAbsolutePath()),
                                                     null);
    IClasspathEntry[] entries = new IClasspathEntry[]{ entry };
    LibraryClasspathContainer container = mock(LibraryClasspathContainer.class);
    when(container.getClasspathEntries()).thenReturn(entries);
    when(serializer.loadContainer(any(IJavaProject.class), any(IPath.class))).thenReturn(container);

    LibraryClasspathContainerInitializer containerInitializer =
        new LibraryClasspathContainerInitializer(TEST_CONTAINER_PATH, serializer, resolverService);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH), testProject.getJavaProject());
    testProject.getJavaProject().getRawClasspath();
    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, Matchers.greaterThan(2));
    for (IClasspathEntry resolvedEntry : resolvedClasspath) {
      if (resolvedEntry.getPath().toOSString().equals(artifactFile.getAbsolutePath())) {
        assertThat(resolvedEntry.getSourceAttachmentPath().toOSString(),
            is(sourceArtifactFile.getAbsolutePath()));
        verifyContainerWasNotResolvedFromScratch();
        return;
      }
    }
    fail("classpath entry not found");
  }

  private IStatus verifyContainerResolvedFromScratch() {
    return verify(resolverService).resolveContainer(any(IJavaProject.class), any(IPath.class),
                                                    any(IProgressMonitor.class));
  }

  private IStatus verifyContainerWasNotResolvedFromScratch() {
    return verify(resolverService, never()).resolveContainer(any(IJavaProject.class), any(IPath.class),
                                                      any(IProgressMonitor.class));
  }
}
