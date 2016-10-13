/*******************************************************************************
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.libraries;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.LibraryRepositoryServiceException;
import com.google.common.base.Strings;

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
    if (!Strings.isNullOrEmpty(library.getName())) {
      return library.getName();
    } else {
      return library.getId();
    }
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    ServiceReference<ILibraryRepositoryService> serviceReference = null;
    try {
      List<LibraryFile> libraryFiles = library.getLibraryFiles();
      IClasspathEntry[] entries = new IClasspathEntry[libraryFiles.size()];
      int idx = 0;
      serviceReference = lookupRepositoryServiceReference();
      ILibraryRepositoryService repositoryService = getBundleContext().getService(serviceReference);
      for (LibraryFile libraryFile : libraryFiles) {
        IClasspathAttribute[] classpathAttributes;
        if (libraryFile.isExport()) {
          classpathAttributes = new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createDependencyAttribute() };
        } else {
          classpathAttributes = new IClasspathAttribute[] { UpdateClasspathAttributeUtil.createNonDependencyAttribute() };
        }
        entries[idx++] =
            JavaCore.newLibraryEntry(repositoryService.getJarLocation(libraryFile.getMavenCoordinates()),
                                     getSourceLocation(repositoryService, libraryFile),
                                     null,
                                     getAccessRules(libraryFile.getFilters()),
                                     classpathAttributes,
                                     true);
      }
      return entries;
    } catch (CoreException | LibraryRepositoryServiceException ex) {
      // declared on UpdateClasspathAttributeUtil.create(Non)DependencyAttribute(), but its current implementation does
      // not throw this exception.
      return new IClasspathEntry[0];
    } finally {
      if (serviceReference != null) {
        releaseRepositoryService(serviceReference);
      }
    }
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
    BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
    if (bundleContext == null) {
      throw new IllegalStateException("No bundle context was found for service lookup");
    } else {
      return bundleContext;
    }
  }
}