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

import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.util.ClasspathUtil;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
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
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXException;

public class BuildPath {

  public static void addMavenLibraries(IProject project, List<Library> libraries,
      IProgressMonitor monitor) throws CoreException {
    
    if (libraries.isEmpty()) {
      return;
    }
    
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
   * Returns the entries added to the classpath.
   */
  public static IClasspathEntry[] addNativeLibrary(IJavaProject javaProject,
      List<Library> libraries, IProgressMonitor monitor) throws CoreException {
    
    SubMonitor subMonitor = SubMonitor.convert(monitor,
        Messages.getString("adding.app.engine.libraries"), //$NON-NLS-1$
        3);
    
    Library masterLibrary = collectLibraryFiles(javaProject, libraries);
    subMonitor.worked(1);
    List<IClasspathEntry> newEntries = computeEntries(javaProject, masterLibrary);
    subMonitor.worked(1);

    List<String> libraryIds = new ArrayList<>();
    for (Library library : libraries) {
      libraryIds.add(library.getId());
    }
    try {
      LibraryClasspathContainerSerializer serializer = new LibraryClasspathContainerSerializer();
      // We could create the LibraryClasspathContainer and serialize out the classpath information,
      // but our LibraryClasspathContainerInitializer requires source paths to be resolved too,
      // or it tosses the serialized classpath information
      serializer.saveLibraryIds(javaProject, libraryIds);
    } catch (IOException ex) {
      throw new CoreException(
          StatusUtil.error(BuildPath.class, "Error saving master library list", ex));
    }

    ClasspathUtil.addClasspathEntries(javaProject.getProject(), newEntries, subMonitor);
    runContainerResolverJob(javaProject);

    return newEntries.toArray(new IClasspathEntry[0]);
  }

  /**
   * Adds all jars and dependencies from <code>libraries</code> to the master library.
   * 
   * @return the master library
   */
  public static Library collectLibraryFiles(IJavaProject javaProject, List<Library> libraries)
      throws CoreException {
    SortedSet<LibraryFile> masterFiles = new TreeSet<>();
    List<String> dependentIds = new ArrayList<>();
    for (Library library : libraries) {
      if (!library.isResolved()) {
        library.resolveDependencies();
      }
      dependentIds.add(library.getId());
      masterFiles.addAll(library.getLibraryFiles());
    }

    Library masterLibrary = new Library(CloudLibraries.MASTER_CONTAINER_ID);
    masterLibrary.setName("Google APIs"); //$NON-NLS-1$
    masterLibrary.setLibraryDependencies(dependentIds);
    
    List<LibraryFile> resolved = Library.resolveDuplicates(new ArrayList<LibraryFile>(masterFiles));
    masterLibrary.setLibraryFiles(resolved);
    return masterLibrary;
  }

  private static List<IClasspathEntry> computeEntries(IJavaProject javaProject, Library library)
      throws CoreException {
    List<IClasspathEntry> rawClasspath = Lists.newArrayList(javaProject.getRawClasspath());
    List<IClasspathEntry> newEntries = new ArrayList<>();
    IClasspathEntry libraryContainer = makeClasspathEntry(library);
    if (CloudLibraries.MASTER_CONTAINER_ID.equals(library.getId())) {
      try {
        LibraryClasspathContainerSerializer serializer = new LibraryClasspathContainerSerializer();
        serializer.saveLibraryIds(javaProject, library.getLibraryDependencies());
      } catch (IOException ex) {
        throw new CoreException(
            StatusUtil.error(BuildPath.class, "Error saving master library list", ex));
      }
    }
    if (!rawClasspath.contains(libraryContainer)) {
      newEntries.add(libraryContainer);
    }
    return newEntries;
  }
  
  /**
   * Returns the entries to be added to the classpath. Does not add them to the classpath.
   */
  public static IClasspathEntry[] listNativeLibrary(IJavaProject javaProject, Library library)
      throws CoreException {
    List<IClasspathEntry> newEntries = computeEntries(javaProject, library);
    runContainerResolverJob(javaProject);
    return newEntries.toArray(new IClasspathEntry[0]);
  }

  private static IClasspathEntry makeClasspathEntry(Library library) throws CoreException {
    IClasspathAttribute[] classpathAttributes = new IClasspathAttribute[1];
    if (library.isExport()) {
      boolean isWebApp = true;
      classpathAttributes[0] = UpdateClasspathAttributeUtil.createDependencyAttribute(isWebApp);
    } else {
      classpathAttributes[0] = UpdateClasspathAttributeUtil.createNonDependencyAttribute();
    }

    return JavaCore.newContainerEntry(library.getContainerPath(), new IAccessRule[0],
        classpathAttributes, false);
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
