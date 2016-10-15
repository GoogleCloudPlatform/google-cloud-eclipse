/*******************************************************************************
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
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryFactory;
import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryFactoryException;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineLibraryContainerInitializerTest {

  private static final String TEST_LIBRARY_ID = "libraryId";
  private static final String TEST_CONTAINER_PATH = "test.appengine.libraries";
  private static final String TEST_LIBRARY_PATH = TEST_CONTAINER_PATH + "/" + TEST_LIBRARY_ID;

  @Mock private LibraryFactory libraryFactory;
  @Mock private IConfigurationElement configurationElement;

  @Rule
  public TestLibraryRepositoryServiceRegistrar libraryRepositoryServiceRegistrar =
      new TestLibraryRepositoryServiceRegistrar();
  @Rule
  public TestProject testProject = new TestProject().withClasspathContainerPath(TEST_LIBRARY_PATH);

  @Before
  public void setUp() throws Exception {
    when(libraryRepositoryServiceRegistrar.getRepositoryService().getJarLocation(any(MavenCoordinates.class)))
      .thenAnswer(fakePathFromArtifactId(""));
    when(libraryRepositoryServiceRegistrar.getRepositoryService().getSourceJarLocation(any(MavenCoordinates.class)))
      .thenAnswer(fakePathFromArtifactId("-sources"));
    setupLibraryFactory();
  }

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
                                                 TEST_CONTAINER_PATH);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH),
                                    testProject.getJavaProject());

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
                                                 TEST_CONTAINER_PATH);
    containerInitializer.initialize(new Path("single.segment.id"),
                                    testProject.getJavaProject());
  }

  @Test(expected = CoreException.class)
  public void testInitialize_containerPathConsistsOfThreeSegments() throws Exception {
    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH);
    containerInitializer.initialize(new Path("first.segment/second.segment/third.segment"),
                                    testProject.getJavaProject());
  }

  @Test(expected = CoreException.class)
  public void testInitialize_containerPathHasWrongFirstSegment() throws Exception {
    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH);
    containerInitializer.initialize(new Path("first.segment/second.segment"),
                                    testProject.getJavaProject());
  }

  @Test(expected = CoreException.class)
  public void testInitialize_containerPathHasWrongLibraryId() throws Exception {
    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH);
    containerInitializer.initialize(new Path(TEST_CONTAINER_PATH + "/second.segment"),
                                    testProject.getJavaProject());
  }

  @Test
  public void testInitialize_libraryFactoryErrorDoesNotPreventOtherLibraries() throws Exception {
    Library library = new Library(TEST_LIBRARY_ID);
    library.setLibraryFiles(Collections.singletonList(new LibraryFile(new MavenCoordinates("groupId",
                                                                                           "artifactId"))));
    // this will override what is set in setupLibraryFactory() when setUp() is executed
    doThrow(LibraryFactoryException.class).doReturn(library).when(libraryFactory).create(any(IConfigurationElement.class));

    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement,
                                                                              configurationElement },
                                                 libraryFactory,
                                                 TEST_CONTAINER_PATH);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH),
                                    testProject.getJavaProject());

    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, is(2));
    IClasspathEntry libJar = resolvedClasspath[1];
    assertThat(libJar.getPath().toOSString(), is("/test/path/artifactId.jar"));
    assertThat(libJar.getSourceAttachmentPath().toOSString(), is("/test/path/artifactId-sources.jar"));
  }

  private void setupLibraryFactory() throws LibraryFactoryException {
    Library library = new Library(TEST_LIBRARY_ID);
    library.setLibraryFiles(Collections.singletonList(new LibraryFile(new MavenCoordinates("groupId", "artifactId"))));
    doReturn(library).when(libraryFactory).create(any(IConfigurationElement.class));
  }

  private Answer<IPath> fakePathFromArtifactId(final String postfix) {
    return new Answer<IPath>() {
      @Override
      public IPath answer(InvocationOnMock invocation) throws Throwable {
        String postfixToAdd = postfix;
        if (postfixToAdd ==  null) {
          postfixToAdd = "";
        }
        MavenCoordinates argument = invocation.getArgumentAt(0, MavenCoordinates.class);
        return new Path("/test/path/" + argument.getArtifactId() + postfixToAdd + "." + argument.getType());
      }
    };
  }

}
