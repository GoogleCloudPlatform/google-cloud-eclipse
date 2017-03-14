/*
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
 */

package com.google.cloud.tools.eclipse.appengine.libraries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXException;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainerResolverJob;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;

public class BuildPath {

  public static void addLibraries(IProject project, List<Library> libraries,
      IProgressMonitor monitor) throws CoreException {
    
    if (libraries.isEmpty()) {
      return;
    }
    if (MavenUtils.hasMavenNature(project)) {
      addMavenLibraries(project, libraries, monitor);
    } else {
      IJavaProject javaProject = JavaCore.create(project);
      addLibraries(javaProject, libraries, monitor);
    }
  }

  public static void addMavenLibraries(IProject project, List<Library> libraries,
      IProgressMonitor monitor) throws CoreException {
    // see m2e-core/org.eclipse.m2e.core.ui/src/org/eclipse/m2e/core/ui/internal/actions/AddDependencyAction.java
    // m2e-core/org.eclipse.m2e.core.ui/src/org/eclipse/m2e/core/ui/internal/editing/AddDependencyOperation.java
    
    IFile pomFile = project.getFile("pom.xml");
    
    try {
      Pom pom = Pom.parse(pomFile);
      pom.addDependencies(libraries);
    } catch (SAXException | IOException ex) {
      IStatus status = StatusUtil.error(BuildPath.class, ex.getMessage(), ex);
      throw new CoreException(status);
    }
  }

  /**
   * @return the entries added to the classpath. 
   *     Does not include entries previously present in classpath.
   */
  public static IClasspathEntry[] addLibraries(
      IJavaProject javaProject, List<Library> libraries, IProgressMonitor monitor)
          throws JavaModelException, CoreException {
    
    return prepareLibraries(javaProject, libraries, monitor, true);
  }

  private static IClasspathEntry[] prepareLibraries(IJavaProject javaProject,
      List<Library> libraries, IProgressMonitor monitor, boolean addToClasspath)
          throws JavaModelException, CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor,
        Messages.getString("adding.app.engine.libraries"), //$NON-NLS-1$
        libraries.size() + 1); // + 1 because we pass the submonitor along below

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    
    List<IClasspathEntry> newRawClasspath = new ArrayList<>(rawClasspath.length + libraries.size());
    newRawClasspath.addAll(Arrays.asList(rawClasspath));
    List<IClasspathEntry> newEntries = new ArrayList<>();
    for (Library library : libraries) {
      IClasspathEntry libraryContainer = makeClasspathEntry(library);
      if (!newRawClasspath.contains(libraryContainer)) {
        newEntries.add(libraryContainer);
        newRawClasspath.add(libraryContainer);
      }
      subMonitor.worked(1);
    }
    
    if (addToClasspath) {
      javaProject.setRawClasspath(newRawClasspath.toArray(new IClasspathEntry[0]), subMonitor);
      runContainerResolverJob(javaProject);
    }
    
    return newEntries.toArray(new IClasspathEntry[0]);
  }
  
  /**
   * @return the entries to be added to the classpath. Does not add them to the classpath.
   */
  public static IClasspathEntry[] listAdditionalLibraries(
      IJavaProject javaProject, List<Library> libraries, IProgressMonitor monitor)
          throws JavaModelException, CoreException {
    return prepareLibraries(javaProject, libraries, monitor, false);
  }

  @VisibleForTesting
  static IClasspathEntry makeClasspathEntry(Library library) throws CoreException {
    IClasspathAttribute[] classpathAttributes = new IClasspathAttribute[1];
    if (library.isExport()) {
      boolean isWebApp = true;
      classpathAttributes[0] = UpdateClasspathAttributeUtil.createDependencyAttribute(isWebApp);
    } else {
      classpathAttributes[0] = UpdateClasspathAttributeUtil.createNonDependencyAttribute();
    }
 
    IClasspathEntry libraryContainer = JavaCore.newContainerEntry(library.getContainerPath(),
                                                                  new IAccessRule[0],
                                                                  classpathAttributes,
                                                                  false);
    return libraryContainer;
  }
  
  private static void runContainerResolverJob(IJavaProject javaProject) {
    IEclipseContext context = EclipseContextFactory.getServiceContext(
        FrameworkUtil.getBundle(BuildPath.class).getBundleContext());
    final IEclipseContext childContext =
        context.createChild(LibraryClasspathContainerResolverJob.class.getName());
    childContext.set(IJavaProject.class, javaProject);
    Job job =
        ContextInjectionFactory.make(LibraryClasspathContainerResolverJob.class, childContext);
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        childContext.dispose();
      }
    });
    job.schedule();
  }

}
