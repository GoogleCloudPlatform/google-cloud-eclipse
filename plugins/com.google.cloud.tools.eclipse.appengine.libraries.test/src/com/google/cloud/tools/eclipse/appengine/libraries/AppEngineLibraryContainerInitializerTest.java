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
  private static final String TEST_CONTAINER_PATH = Library.CONTAINER_PATH_PREFIX + "/" + TEST_LIBRARY_ID;

  @Mock private LibraryBuilder libraryBuilder;
  @Mock private IConfigurationElement configurationElement;

  @Rule
  public TestProject testProject = new TestProject().withClasspathContainerPath(TEST_CONTAINER_PATH);

  @Test
  public void testInitialize_resolvesContainerToJar() throws CoreException, LibraryBuilderException {
    Library library = new Library(TEST_LIBRARY_ID);
    library.setLibraryFiles(Collections.singletonList(new LibraryFile(new MavenCoordinates("groupId", "artifactId"))));
    when(libraryBuilder.build(any(IConfigurationElement.class))).thenReturn(library);

    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement }, libraryBuilder);
    containerInitializer.initialize(new Path(TEST_CONTAINER_PATH),
                                    testProject.getJavaProject());

    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, is(2));
    IClasspathEntry libJar = resolvedClasspath[1];
    assertTrue(libJar.getPath().toOSString().endsWith("artifactId.jar"));
    assertTrue(libJar.getSourceAttachmentPath().toOSString().endsWith("artifactId.jar"));
  }
}
