package com.google.cloud.tools.eclipse.appengine.libraries;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class AppEngineLibraryContainerInitializer extends org.eclipse.jdt.core.ClasspathContainerInitializer {

  
  private static final String LIBRARIES_EXTENSION_POINT = "com.google.cloud.tools.eclipse.appengine.libraries";
  private static final String LIBRARY = "library";
  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String SITE_URI = "siteUri";
  private static final String LIBRARY_FILE = "libraryFile";
  private static final String EXCLUSION_FILTER = "exclusionFilter";
  private static final String INCLUSION_FILTER = "inclusionFilter";
  private static final String MAVEN_COORDINATES = "mavenCoordinates";
  private static final String PATTERN = "pattern";

  public AppEngineLibraryContainerInitializer() {
    super();
    IConfigurationElement[] configurationElements = Platform.getExtensionRegistry().getConfigurationElementsFor(LIBRARIES_EXTENSION_POINT);
    for (IConfigurationElement configurationElement : configurationElements) {
      Library library = getLibrary(configurationElement);
    }
  }

  @Override
  public void initialize(final IPath containerPath, IJavaProject project) throws CoreException {
    IClasspathContainer container = new IClasspathContainer() {
      
      @Override
      public IPath getPath() {
        return containerPath;
      }
      
      @Override
      public int getKind() {
        return IClasspathContainer.K_APPLICATION;
      }
      
      @Override
      public String getDescription() {
        return containerPath.lastSegment();
      }
      
      @Override
      public IClasspathEntry[] getClasspathEntries() {
        return new IClasspathEntry[]{ JavaCore.newLibraryEntry(new Path("/" + containerPath.toString()), null, null, null, null, true) };
      }
    };
    JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
                                   new IClasspathContainer[] {container}, null);
  }

  private Library getLibrary(IConfigurationElement configurationElement) {
    try {
    if (configurationElement.getName().equals(LIBRARY)) {
      Library library = new Library(configurationElement.getAttribute(ID));
      library.setName(configurationElement.getAttribute(NAME));
      library.setSiteUri(new URI(configurationElement.getAttribute(SITE_URI)));
      library.setLibraryFiles(getLibraryFiles(configurationElement.getChildren(LIBRARY_FILE)));
    }
    } catch (InvalidRegistryObjectException | URISyntaxException e) {
      // TODO: handle exception
    }
    return null;
  }

  private List<LibraryFile> getLibraryFiles(IConfigurationElement[] children) {
    for (IConfigurationElement libraryFileElement : children) {
      if (libraryFileElement.getName().equals(LIBRARY_FILE)) {
        LibraryFile libraryFile = new LibraryFile();
        libraryFile.setExclusionFilters(getExclusionFilters(libraryFileElement.getChildren(EXCLUSION_FILTER)));
        libraryFile.setInclusionFilters(getInclusionFilters(libraryFileElement.getChildren(INCLUSION_FILTER)));
        libraryFile.setMavenCoordinates(getMavenCoordinates(libraryFileElement.getChildren(MAVEN_COORDINATES)));
      }
    }
    return null;
  }

  private MavenCoordinates getMavenCoordinates(IConfigurationElement[] children) {
    if (children.length > 1) {
      // error
    }
    for (IConfigurationElement mavenCoordinatesElement : children) {
      MavenCoordinates mavenCoordinates = new MavenCoordinates();
      
    }
    return null;
  }

  private List<InclusionFilter> getInclusionFilters(IConfigurationElement[] children) {
    LinkedList<InclusionFilter> inclusionFilters = new LinkedList<>();
    for (IConfigurationElement inclusionFilterElement : children) {
      inclusionFilters.add(new InclusionFilter(inclusionFilterElement.getAttribute(PATTERN)));
    }
    return inclusionFilters;
  }

  private List<ExclusionFilter> getExclusionFilters(IConfigurationElement[] children) {
    LinkedList<ExclusionFilter> exclusionFilters = new LinkedList<>();
    for (IConfigurationElement exclusionFilterElement : children) {
      exclusionFilters.add(new ExclusionFilter(exclusionFilterElement.getAttribute(PATTERN)));
    }
    return exclusionFilters;
  }

}
