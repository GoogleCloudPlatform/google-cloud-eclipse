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

@RunWith(MockitoJUnitRunner.class)
public class AppEngineLibraryContainerInitializerTest {

  @Mock private LibraryBuilder libraryBuilder;
  @Mock private IConfigurationElement configurationElement;

  @Rule
  public TestProject testProject = new TestProject();

  @Test
  public void testInitialize_resolvesContainerToJar() throws CoreException {
    Library library = new Library("bbb");
    library.setLibraryFiles(Collections.singletonList(new LibraryFile(new MavenCoordinates("a", "b", "c"))));
    when(libraryBuilder.build(any(IConfigurationElement.class))).thenReturn(library);

    AppEngineLibraryContainerInitializer containerInitializer =
        new AppEngineLibraryContainerInitializer(new IConfigurationElement[]{ configurationElement }, libraryBuilder);
    containerInitializer.initialize(new Path("aaa/bbb"), testProject.getJavaProject());

    IClasspathEntry[] resolvedClasspath = testProject.getJavaProject().getResolvedClasspath(false);
    assertThat(resolvedClasspath.length, is(2));
    IClasspathEntry libJar = resolvedClasspath[1];
    assertTrue(libJar.getPath().toOSString().endsWith("c.jar"));
    assertTrue(libJar.getSourceAttachmentPath().toOSString().endsWith("c.jar"));
  }
}
