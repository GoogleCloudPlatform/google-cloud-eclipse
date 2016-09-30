package com.google.cloud.tools.eclipse.appengine.libraries;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryBuilder;
import com.google.common.annotations.VisibleForTesting;

public class AppEngineLibraryContainerInitializer extends org.eclipse.jdt.core.ClasspathContainerInitializer {

  private static final String LIBRARIES_EXTENSION_POINT = "com.google.cloud.tools.eclipse.appengine.libraries";

  private Map<String, Library> libraries;

  public AppEngineLibraryContainerInitializer() {
    super();
    IConfigurationElement[] configurationElements = Platform.getExtensionRegistry().getConfigurationElementsFor(LIBRARIES_EXTENSION_POINT);
    initializeLibraries(configurationElements, new LibraryBuilder());
  }

  private void initializeLibraries(IConfigurationElement[] configurationElements, LibraryBuilder libraryBuilder) {
    libraries = new HashMap<>(configurationElements.length);
    for (IConfigurationElement configurationElement : configurationElements) {
      Library library = libraryBuilder.build(configurationElement);
      libraries.put(library.getId(), library);
    }
  }

  @VisibleForTesting
  AppEngineLibraryContainerInitializer(IConfigurationElement[] configurationElements, LibraryBuilder libraryBuilder) {
    initializeLibraries(configurationElements, libraryBuilder);
  }

  @Override
  public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
    if (containerPath.segmentCount() == 2) {
      String libraryId = containerPath.lastSegment();
      Library library = libraries.get(libraryId);
      if (library != null) {
        LibraryClasspathContainer container = new LibraryClasspathContainer(containerPath, library);
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
                                       new IClasspathContainer[] {container}, null);
      }
    }
  }
}
