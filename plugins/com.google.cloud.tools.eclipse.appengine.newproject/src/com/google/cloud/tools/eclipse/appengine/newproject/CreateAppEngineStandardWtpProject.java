/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.newproject;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.Filter;
import com.google.cloud.tools.eclipse.appengine.libraries.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.LibraryRepositoryServiceException;
import com.google.cloud.tools.eclipse.preferences.PreferenceUtil;

/**
* Utility to make a new Eclipse project with the App Engine Standard facets in the workspace.  
*/
class CreateAppEngineStandardWtpProject extends WorkspaceModifyOperation {

  private final AppEngineStandardProjectConfig config;
  private final IAdaptable uiInfoAdapter;

  CreateAppEngineStandardWtpProject(AppEngineStandardProjectConfig config, IAdaptable uiInfoAdapter) {
    if (config == null) {
      throw new NullPointerException("Null App Engine configuration");
    }
    this.config = config;
    this.uiInfoAdapter = uiInfoAdapter;
  }

  @Override
  public void execute(IProgressMonitor monitor) throws InvocationTargetException, CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IProject newProject = config.getProject();
    URI location = config.getEclipseProjectLocationUri();

    String name = newProject.getName();
    final IProjectDescription description = workspace.newProjectDescription(name);
    description.setLocationURI(location);
    
    CreateProjectOperation operation = new CreateProjectOperation(
        description, "Creating new App Engine Project");
    try {
      operation.execute(monitor, uiInfoAdapter);
      CodeTemplates.materialize(newProject, config, monitor);
    } catch (ExecutionException ex) {
      throw new InvocationTargetException(ex, ex.getMessage());
    }

    IFacetedProject facetedProject = ProjectFacetsManager.create(
        newProject, true, monitor);
    AppEngineStandardFacet.installAppEngineFacet(
        facetedProject, true /* installDependentFacets */, monitor);
    AppEngineStandardFacet.installAllAppEngineRuntimes(facetedProject, true /* force */, monitor);
    
    PreferenceUtil.setProjectIdPreference(newProject, config.getAppEngineProjectId());

    addAppEngineLibrariesToBuildPath(newProject, config.getAppEngineLibraries(), monitor);

    addJunit4ToClasspath(monitor, newProject);
  }

  private void addAppEngineLibrariesToBuildPath(IProject newProject,
                                                final List<Library> libraries,
                                                IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, "Adding App Engine libraries", libraries.size());
    final IJavaProject javaProject = JavaCore.create(newProject);
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    final IClasspathEntry[] newRawClasspath = Arrays.copyOf(rawClasspath, rawClasspath.length + libraries.size());
    for (int i = 0; i < libraries.size(); i++) {
      Library library = libraries.get(i);
      IClasspathAttribute[] classpathAttributes;
      if (library.isExport()) {
        classpathAttributes =
            new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createDependencyAttribute(true /* isWebApp */) };
      } else {
        classpathAttributes =
            new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createNonDependencyAttribute() };
      }

      IClasspathEntry libraryContainer = JavaCore.newContainerEntry(library.getContainerPath(),
                                                                    new IAccessRule[0],
                                                                    classpathAttributes,
                                                                    false);
      newRawClasspath[rawClasspath.length + i] = libraryContainer;
      subMonitor.worked(1);
    }
    javaProject.setRawClasspath(newRawClasspath, monitor);
    
    Job job = new Job("Initialize libraries") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          IClasspathEntry[] updatedClasspath = new IClasspathEntry[newRawClasspath.length];
          System.arraycopy(newRawClasspath, 0, updatedClasspath, 0, newRawClasspath.length);
          ServiceReference<ILibraryRepositoryService> serviceReference = null;
          serviceReference = lookupRepositoryServiceReference();
          ILibraryRepositoryService repositoryService = getBundleContext().getService(serviceReference);
          for (int i = 0; i < newRawClasspath.length; i++) {
            IClasspathEntry classpathEntry = newRawClasspath[i];
            String libraryId = classpathEntry.getPath().segment(1);
            for (Library library : libraries) {
              if (library.getId().equals(libraryId)) {
                IClasspathAttribute[] classpathAttributes;
                if (library.isExport()) {
                  classpathAttributes =
                      new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createDependencyAttribute(true /* isWebApp */) };
                } else {
                  classpathAttributes =
                      new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createNonDependencyAttribute() };
                }
                List<LibraryFile> libraryFiles = library.getLibraryFiles();
                IClasspathEntry[] entries = new IClasspathEntry[libraryFiles.size()];
                int idx = 0;
                for (LibraryFile libraryFile : libraryFiles) {
                  IClasspathAttribute[] libraryFileClasspathAttributes;
                  if (libraryFile.isExport()) {
                    libraryFileClasspathAttributes =
                        new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createDependencyAttribute(true /* isWebApp */) };
                  } else {
                    libraryFileClasspathAttributes =
                        new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createNonDependencyAttribute() };
                  }
                  entries[idx++] =
                      JavaCore.newLibraryEntry(repositoryService.getJarLocation(libraryFile.getMavenCoordinates()),
                                               getSourceLocation(repositoryService, libraryFile),
                                               null,
                                               getAccessRules(libraryFile.getFilters()),
                                               libraryFileClasspathAttributes,
                                               true);
                }
                updatedClasspath[i] = JavaCore.newContainerEntry(library.getContainerPath(),
                                                                 new IAccessRule[0],
                                                                 classpathAttributes,
                                                                 false);
              }
            }
            updatedClasspath[i] = newRawClasspath[i];
          }
          javaProject.setRawClasspath(updatedClasspath, monitor);
        } catch (LibraryRepositoryServiceException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (CoreException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return null;
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

      private void releaseRepositoryService(ServiceReference<ILibraryRepositoryService> serviceReference) {
        BundleContext bundleContext = getBundleContext();
        bundleContext.ungetService(serviceReference);
      }

      private BundleContext getBundleContext() {
        //TODO change the class to obtain bundle context with
//        FrameworkUtil.getBundle(CreateAppEngineStandardWtpProject.class).start();
        BundleContext bundleContext = FrameworkUtil.getBundle(CreateAppEngineStandardWtpProject.class).getBundleContext();
        if (bundleContext == null) {
          throw new IllegalStateException("No bundle context was found for service lookup");
        } else {
          return bundleContext;
        }
      }

    };
    job.setRule(newProject);
    job.schedule();
  }

  private void addJunit4ToClasspath(IProgressMonitor monitor, final IProject newProject) throws CoreException,
                                                                                         JavaModelException {
    IJavaProject javaProject = JavaCore.create(newProject);
    IClasspathAttribute nonDependencyAttribute = UpdateClasspathAttributeUtil.createNonDependencyAttribute();
    IClasspathEntry junit4Container = JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH,
                                                                 new IAccessRule[0],
                                                                 new IClasspathAttribute[]{ nonDependencyAttribute },
                                                                 false);
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    IClasspathEntry[] newRawClasspath = Arrays.copyOf(rawClasspath, rawClasspath.length + 1);
    newRawClasspath[newRawClasspath.length - 1] = junit4Container;
    javaProject.setRawClasspath(newRawClasspath, monitor);
  }

}
