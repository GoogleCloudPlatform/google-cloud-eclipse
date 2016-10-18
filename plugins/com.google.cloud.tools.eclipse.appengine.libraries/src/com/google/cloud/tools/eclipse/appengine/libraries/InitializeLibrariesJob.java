package com.google.cloud.tools.eclipse.appengine.libraries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryFactory;
import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryFactory.LibraryFactoryException;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.LibraryRepositoryServiceException;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;

public class InitializeLibrariesJob extends Job {
  //TODO duplicate of com.google.cloud.tools.eclipse.appengine.libraries.AppEngineLibraryContainerInitializer.LIBRARIES_EXTENSION_POINT
  public static final String LIBRARIES_EXTENSION_POINT = "com.google.cloud.tools.eclipse.appengine.libraries";
  
  private static final Logger logger = Logger.getLogger(InitializeLibrariesJob.class.getName());

  private Map<String, Library> libraries;
  private final IJavaProject javaProject;
  private ServiceReference<ILibraryRepositoryService> serviceReference = null;

  public InitializeLibrariesJob(String name, IJavaProject javaProject) {
    super(name);
    Preconditions.checkNotNull(javaProject, "javaProject is null");
    this.javaProject = javaProject;
    setUser(true);
    setRule(javaProject.getSchedulingRule());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    // TODO use progress monitor
    // TODO replace resolution logic in LibraryClasspathContainer with load from disk logic in AppEngineLibraryContainerInitializer
    // TODO when to save the entries of LibraryClasspathContainer?
    // TODO parse library definition in ILibraryConfigService (or similar) started when the plugin/bundle starts
    try {
      if (libraries == null) {
        // in tests libraries will be initialized via the test constructor, this would override mocks/stubs.
        IConfigurationElement[] configurationElements =
            RegistryFactory.getRegistry().getConfigurationElementsFor(LIBRARIES_EXTENSION_POINT);
        initializeLibraries(configurationElements, new LibraryFactory());
      }
      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      serviceReference = lookupRepositoryServiceReference();
      ILibraryRepositoryService repositoryService = getBundleContext().getService(serviceReference);
      for (int i = 0; i < rawClasspath.length; i++) {
        IClasspathEntry classpathEntry = rawClasspath[i];
        String libraryId = classpathEntry.getPath().segment(1);
        Library library = libraries.get(libraryId);
        if (library != null) {
          List<LibraryFile> libraryFiles = library.getLibraryFiles();
          IClasspathEntry[] entries = new IClasspathEntry[libraryFiles.size()];
          int idx = 0;
          for (LibraryFile libraryFile : libraryFiles) {
            IClasspathAttribute[] libraryFileClasspathAttributes = getClasspathAttributes(libraryFile);
            entries[idx++] =
                JavaCore.newLibraryEntry(repositoryService.getJarLocation(libraryFile.getMavenCoordinates()),
                    getSourceLocation(repositoryService, libraryFile),
                    null,
                    getAccessRules(libraryFile.getFilters()),
                    libraryFileClasspathAttributes,
                    true);
          }
          LibraryClasspathContainer container = new LibraryClasspathContainer(classpathEntry.getPath(), library, entries);
          JavaCore.setClasspathContainer(classpathEntry.getPath(), new IJavaProject[] {javaProject},
              new IClasspathContainer[] {container}, null);
        }
      }
    } catch (LibraryRepositoryServiceException | CoreException ex) {
      return StatusUtil.error(this, "Could not resolve libraries", ex);
    } finally {
      releaseRepositoryService();
    }
    return Status.OK_STATUS;
  }

  private IClasspathAttribute[] getClasspathAttributes(LibraryFile libraryFile) throws CoreException {
    IClasspathAttribute[] libraryFileClasspathAttributes;
    if (libraryFile.isExport()) {
      libraryFileClasspathAttributes =
          new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createDependencyAttribute(true /* isWebApp */) };
    } else {
      libraryFileClasspathAttributes =
          new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createNonDependencyAttribute() };
    }
    return libraryFileClasspathAttributes;
  }

  private IPath getSourceLocation(ILibraryRepositoryService repositoryService, LibraryFile libraryFile) {
    if (libraryFile.getSourceUri() == null) {
      return repositoryService.getSourceJarLocation(libraryFile.getMavenCoordinates());
    } else {
      // download the file and return path to it
      // TODO https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/800
      return new Path("/downloaded/source/file");
    }
  }

  private void initializeLibraries(IConfigurationElement[] configurationElements, LibraryFactory libraryFactory) {
    libraries = new HashMap<>(configurationElements.length);
    for (IConfigurationElement configurationElement : configurationElements) {
      try {
        Library library = libraryFactory.create(configurationElement);
        libraries.put(library.getId(), library);
      } catch (LibraryFactoryException exception) {
        logger.log(Level.SEVERE, "Failed to initialize libraries", exception);
      }
    }
  }
  private IAccessRule[] getAccessRules(List<Filter> filters) {
    IAccessRule[] accessRules = new IAccessRule[filters.size()];
    int idx = 0;
    for (Filter filter : filters) {
      if (filter.isExclude()) {
        IAccessRule accessRule = JavaCore.newAccessRule(new Path(filter.getPattern()), IAccessRule.K_NON_ACCESSIBLE);
        accessRules[idx++] = accessRule;
      } else {
        IAccessRule accessRule = JavaCore.newAccessRule(new Path(filter.getPattern()), IAccessRule.K_ACCESSIBLE);
        accessRules[idx++] = accessRule;
      }
    }
    return accessRules;
  }

  private ServiceReference<ILibraryRepositoryService> lookupRepositoryServiceReference() {
    BundleContext bundleContext = getBundleContext();
    ServiceReference<ILibraryRepositoryService> serviceReference =
        bundleContext.getServiceReference(ILibraryRepositoryService.class);
    return serviceReference;
  }

  private void releaseRepositoryService() {
    BundleContext bundleContext = getBundleContext();
    bundleContext.ungetService(serviceReference);
  }

  private BundleContext getBundleContext() {
    BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
    if (bundleContext == null) {
      throw new IllegalStateException("No bundle context was found for service lookup");
    } else {
      return bundleContext;
    }
  }
}