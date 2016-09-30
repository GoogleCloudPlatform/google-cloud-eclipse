package com.google.cloud.tools.eclipse.appengine.libraries;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;

public class LibraryClasspathContainer implements IClasspathContainer {
  private final IPath containerPath;
  private Library library;

  LibraryClasspathContainer(IPath containerPath, Library library) {
    this.containerPath = containerPath;
    this.library = library;
  }

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
    return library.getDescription();
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    List<LibraryFile> libraryFiles = library.getLibraryFiles();
    IClasspathEntry[] entries = new IClasspathEntry[libraryFiles.size()];
    int idx = 0;
    ILibraryRepositoryService repositoryService = lookupRepositoryService();
    for (LibraryFile libraryFile : libraryFiles) {
      entries[idx++] = JavaCore.newLibraryEntry(repositoryService.getJarLocation(libraryFile.getMavenCoordinates()),
                                                repositoryService.getSourceJarLocation(libraryFile.getMavenCoordinates()),
                                                null,
                                                getAccessRules(libraryFile.getExclusionFilters(), libraryFile.getInclusionFilters()),
                                                null,
                                                true);
    }
    return entries;
  }

  private IAccessRule[] getAccessRules(List<ExclusionFilter> exclusionFilters, List<InclusionFilter> inclusionFilters) {
    IAccessRule[] accessRules = new IAccessRule[exclusionFilters.size() + inclusionFilters.size()];
    int idx = 0;
    for (final ExclusionFilter exclusionFilter : exclusionFilters) {
      IAccessRule accessRule = JavaCore.newAccessRule(new Path(exclusionFilter.getPattern()), IAccessRule.K_NON_ACCESSIBLE);
      accessRules[idx++] = accessRule;
    }
    for (final InclusionFilter inclusionFilter : inclusionFilters) {
      IAccessRule accessRule = JavaCore.newAccessRule(new Path(inclusionFilter.getPattern()), IAccessRule.K_ACCESSIBLE);
      accessRules[idx++] = accessRule;
    }
    return accessRules;
  }

  private ILibraryRepositoryService lookupRepositoryService() {
    BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
    ServiceReference<ILibraryRepositoryService> serviceReference =
        bundleContext.getServiceReference(ILibraryRepositoryService.class);
    ILibraryRepositoryService repositoryService = bundleContext.getService(serviceReference);
    return repositoryService;
  }
}