package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryBuilder;
import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryBuilder.LibraryBuilderException;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineLibraryContainerInitializerTest {

  private static final String TEST_LIBRARY_ID = "libraryId";
  private static final String TEST_CONTAINER_PATH = "test.appengine.libraries";
  private static final String TEST_LIBRARY_PATH = TEST_CONTAINER_PATH + "/" + TEST_LIBRARY_ID;

  @Mock private LibraryBuilder libraryBuilder;
  @Mock private IConfigurationElement configurationElement;

  @Rule
  public TestProject testProject = new TestProject().withClasspathContainerPath(TEST_LIBRARY_PATH);

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
  public void testInitialize_resolvesContainerToJar() throws CoreException, LibraryBuilderException {
    Library library = new Library(TEST_LIBRARY_ID);
    library.setLibraryFiles(Collections.singletonList(new LibraryFile(new MavenCoordinates("groupId", "artifactId"))));
    when(libraryBuilder.build(any(IConfigurationElement.class))).thenReturn(library);

    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement },
                                                 libraryBuilder,
                                                 TEST_CONTAINER_PATH);
    containerInitializer.initialize(new Path(TEST_LIBRARY_PATH),
                                    testProject.getJavaProject());

    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, is(2));
    IClasspathEntry libJar = resolvedClasspath[1];
    assertTrue(libJar.getPath().toOSString().endsWith("artifactId.jar"));
    assertTrue(libJar.getSourceAttachmentPath().toOSString().endsWith("artifactId.jar"));
  }
}
