package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.eclipse.core.internal.registry.RegistryProviderFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.IRegistryProvider;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.After;
import org.junit.Before;
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
  @Mock private IRegistryProvider registryProvider;
  @Mock private IExtensionRegistry registry;

  @Rule
  public TestProject testProject = new TestProject().withClasspathContainerPath(TEST_CONTAINER_PATH);

  // save the original default to be restored after test
  private IRegistryProvider defaultRegistryProvider;

  @Before
  public void setUp() throws CoreException {
    defaultRegistryProvider = RegistryProviderFactory.getDefault();
    RegistryProviderFactory.releaseDefault();
    RegistryFactory.setDefaultRegistryProvider(registryProvider);
    when(registryProvider.getRegistry()).thenReturn(registry);
    when(registry.getConfigurationElementsFor(AppEngineLibraryContainerInitializer.LIBRARIES_EXTENSION_POINT))
      .thenReturn(new IConfigurationElement[0]);
  }

  @After
  public void tearDown() throws CoreException {
    RegistryProviderFactory.releaseDefault();
    RegistryFactory.setDefaultRegistryProvider(defaultRegistryProvider);
  }

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
