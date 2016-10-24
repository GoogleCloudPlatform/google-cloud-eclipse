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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFactory;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFactoryException;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer.LibraryContainerStateLocationProvider;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineLibraryContainerInitializerTest {

  private static final String TEST_LIBRARY_ID = "libraryId";
  private static final String TEST_CONTAINER_PATH = "test.appengine.libraries";
  private static final String TEST_LIBRARY_PATH = TEST_CONTAINER_PATH + "/" + TEST_LIBRARY_ID;

  @Mock private LibraryFactory libraryFactory;
  @Mock private IConfigurationElement configurationElement;
  @Mock private LibraryContainerStateLocationProvider containerStateProvider;

  private LibraryClasspathContainerSerializer serializer;

  @Rule
  public TestLibraryRepositoryServiceRegistrar libraryRepositoryServiceRegistrar =
      new TestLibraryRepositoryServiceRegistrar();
  @Rule
  public TestProject testProject = new TestProject().withClasspathContainerPath(TEST_LIBRARY_PATH);
  @Rule
  public TemporaryFolder stateLocationFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    when(libraryRepositoryServiceRegistrar.getRepositoryService().getLibraryClasspathEntry(any(LibraryFile.class)))
      .thenAnswer(fakeClasspathEntry());
    setupLibraryFactory();
    setupSerializer();
  }

  // TODO currently AppEngineLibraryContainerInitializer does not depend on ILibraryRepositoryService, but will
  // after https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/855 is resolved
  /**
   * This test relies on the {@link TestAppEngineLibraryContainerInitializer} defined in the fragment.xml for
   * <code>TEST_CONTAINER_PATH</code>. When the test is launched, the Platform will try to initialize the container
   * defined for the test project (field <code>testProject</code>), but due to the empty implementation of
   * {@link TestAppEngineLibraryContainerInitializer#initialize(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)}
   * the container will remain unresolved.
   * Then the {@link AppEngineLibraryContainerInitializer} instance created in this method will initialize the container
   * and the test will verify it.
   * This approach is required by the fact that the production {@link AppEngineLibraryContainerInitializer} is defined
   * in the host project's plugin.xml and it is not possible to remove/override it.
   */
  @Test
  public void testInitialize_resolvesContainerToJar() throws CoreException {
    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH,
                                                 serializer);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH), testProject.getJavaProject());

    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, is(2));
    IClasspathEntry libJar = resolvedClasspath[1];
    assertThat(libJar.getPath().toOSString(), is("/test/path/artifactId.jar"));
    assertThat(libJar.getSourceAttachmentPath().toOSString(), is("/test/path/artifactId-sources.jar"));
  }

  @Test(expected = CoreException.class)
  public void testInitialize_containerPathConsistsOfOneSegment() throws Exception {
    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH,
                                                 serializer);
    containerInitializer.initialize(new Path("single.segment.id"), testProject.getJavaProject());
  }

  @Test(expected = CoreException.class)
  public void testInitialize_containerPathConsistsOfThreeSegments() throws Exception {
    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH,
                                                 serializer);
    containerInitializer.initialize(new Path("first.segment/second.segment/third.segment"),
                                    testProject.getJavaProject());
  }

  @Test(expected = CoreException.class)
  public void testInitialize_containerPathHasWrongFirstSegment() throws Exception {
    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH,
                                                 serializer);
    containerInitializer.initialize(new Path("first.segment/second.segment"), testProject.getJavaProject());
  }

  @Test
  public void testInitialize_containerPathHasWrongLibraryId() throws Exception {
    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH,
                                                 serializer);
    containerInitializer.initialize(new Path(TEST_CONTAINER_PATH + "/second.segment"), testProject.getJavaProject());
    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, is(1));
  }

  @Test
  public void testInitialize_libraryFactoryErrorDoesNotPreventOtherLibraries() throws Exception {
    Library library = new Library(TEST_LIBRARY_ID);
    library.setLibraryFiles(Collections.singletonList(new LibraryFile(new MavenCoordinates("groupId",
                                                                                           "artifactId"))));
    // this will override what is set in setupLibraryFactory() when setUp() is executed
    doThrow(LibraryFactoryException.class)
      .doReturn(library)
      .when(libraryFactory).create(any(IConfigurationElement.class));

    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement,
                                                                              configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH,
                                                 serializer);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH), testProject.getJavaProject());

    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, is(2));
    IClasspathEntry libJar = resolvedClasspath[1];
    assertThat(libJar.getPath().toOSString(), is("/test/path/artifactId.jar"));
    assertThat(libJar.getSourceAttachmentPath().toOSString(), is("/test/path/artifactId-sources.jar"));
  }

  @Test(expected = CoreException.class)
  public void testInitialize_deserializingContainerThrowsError() throws Exception {
    LibraryClasspathContainerSerializer mockSerializer = mock(LibraryClasspathContainerSerializer.class);
    doThrow(new IOException("test exception"))
      .when(mockSerializer).loadContainer(any(IJavaProject.class), any(IPath.class));
    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH,
                                                 mockSerializer);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH), testProject.getJavaProject());
  }

  private void setupLibraryFactory() throws LibraryFactoryException {
    Library library = new Library(TEST_LIBRARY_ID);
    library.setLibraryFiles(Collections.singletonList(new LibraryFile(new MavenCoordinates("groupId", "artifactId"))));
    doReturn(library).when(libraryFactory).create(any(IConfigurationElement.class));
  }

  private void setupSerializer() throws IOException, CoreException {
    serializer = new LibraryClasspathContainerSerializer(containerStateProvider);
    File stateFile = stateLocationFolder.newFile();
    when(containerStateProvider.getContainerStateFile(any(IJavaProject.class),
                                                      eq(new Path(TEST_LIBRARY_PATH)),
                                                      anyBoolean()))
      .thenReturn(new Path(stateFile.getAbsolutePath()));
    IClasspathEntry[] classpathEntries =
        new IClasspathEntry[] { JavaCore.newLibraryEntry(new Path("/test/path/artifactId.jar"),
                                                         new Path("/test/path/artifactId-sources.jar"),
                                                         null /* sourceAttachmentRootPath */) };
    LibraryClasspathContainer container =
        new LibraryClasspathContainer(new Path(TEST_LIBRARY_PATH), "Test API", classpathEntries);
    serializer.saveContainer(null, container);
  }

  private Answer<IClasspathEntry> fakeClasspathEntry() {
    return new Answer<IClasspathEntry>() {
      @Override
      public IClasspathEntry answer(InvocationOnMock invocation) throws Throwable {
        MavenCoordinates mavenCoordinates = invocation.getArgumentAt(0, MavenCoordinates.class);
        IClasspathEntry classpathEntry = mock(IClasspathEntry.class);
        when(classpathEntry.getPath()).thenReturn(new Path("/test/path/" + mavenCoordinates.getArtifactId() + "." + mavenCoordinates.getType()));
        when(classpathEntry.getSourceAttachmentPath()).thenReturn(new Path("/test/path/" + mavenCoordinates.getArtifactId() + "-sources." + mavenCoordinates.getType()));
        return classpathEntry;
      }
    };
  }
}
