package com.google.cloud.tools.eclipse.appengine.libraries;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryBuilder;
import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryBuilder.LibraryBuilderException;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;

/**
 * {@link ClasspathContainerInitializer} implementation that resolves containers for App Engine libraries.
 * <p>
 * The container path is expected to be in the form of
 * &lt;value of {@link Library#CONTAINER_PATH_PREFIX}&gt;/&lt;library ID&gt;
 */
public class AppEngineLibraryContainerInitializer extends ClasspathContainerInitializer {

  public static final String LIBRARIES_EXTENSION_POINT = "com.google.cloud.tools.eclipse.appengine.libraries";

  private Map<String, Library> libraries;

  public AppEngineLibraryContainerInitializer() {
    super();
  }

  @VisibleForTesting
  AppEngineLibraryContainerInitializer(IConfigurationElement[] configurationElements,
                                       LibraryBuilder libraryBuilder) throws CoreException {
    initializeLibraries(configurationElements, libraryBuilder);
  }

  @Override
  public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
    if (libraries == null) {
      // in tests libraries will be initialized via the test constructor, this would override mocks/stubs.
      IConfigurationElement[] configurationElements = RegistryFactory.getRegistry().getConfigurationElementsFor(LIBRARIES_EXTENSION_POINT);
      initializeLibraries(configurationElements, new LibraryBuilder());
    }
    if (containerPath.segmentCount() == 2) {
      if (!containerPath.segment(0).equals(Library.CONTAINER_PATH_PREFIX)) {
        throw new CoreException(StatusUtil.error(this,
                                                 MessageFormat.format("Unexpected first segment of container path, "
                                                                      + "expected: {0} was: {1}",
                                                                      Library.CONTAINER_PATH_PREFIX,
                                                                      containerPath.segment(0))));
      }
      String libraryId = containerPath.lastSegment();
      Library library = libraries.get(libraryId);
      if (library != null) {
        LibraryClasspathContainer container = new LibraryClasspathContainer(containerPath, library);
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
                                       new IClasspathContainer[] {container}, null);
      } else {
        throw new CoreException(StatusUtil.error(this, "library not found for ID: " + libraryId));
      }
    } else {
      throw new CoreException(StatusUtil.error(this,
                                               "containerPath does not have exactly 2 segments: "
                                               + containerPath.toString()));
    }
  }

  private void initializeLibraries(IConfigurationElement[] configurationElements,
                                   LibraryBuilder libraryBuilder) throws CoreException {
    try {
      libraries = new HashMap<>(configurationElements.length);
      for (IConfigurationElement configurationElement : configurationElements) {
        Library library = libraryBuilder.build(configurationElement);
        libraries.put(library.getId(), library);
      }
    } catch (LibraryBuilderException exception) {
      throw new CoreException(StatusUtil.error(this, "Failed to initialize libraries", exception));
    }
  }
}
