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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  private static final Logger logger = Logger.getLogger(BuildPath.class.getName());
  
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
  public static void addNativeLibrary(IJavaProject javaProject,
      List<Library> libraries, IProgressMonitor monitor) throws CoreException {
    
    SubMonitor subMonitor = SubMonitor.convert(monitor,
        Messages.getString("adding.app.engine.libraries"), //$NON-NLS-1$
        18);
    
    Library masterLibrary = collectLibraryFiles(javaProject, libraries, subMonitor.newChild(8));
    subMonitor.worked(1);
    IClasspathEntry masterEntry = computeEntry(javaProject, masterLibrary, subMonitor.newChild(8));
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

    if (masterEntry != null) {
      ClasspathUtil.addClasspathEntry(javaProject.getProject(), masterEntry, subMonitor);
    }
    runContainerResolverJob(javaProject);
  }

  /**
   * Adds all jars and dependencies from <code>libraries</code> to the master library.
   * 
   * @return the master library
   */
  public static Library collectLibraryFiles(IJavaProject javaProject, List<Library> libraries,
      IProgressMonitor monitor) {
    
    SubMonitor subMonitor = SubMonitor.convert(monitor,
        Messages.getString("calculating.dependencies"), //$NON-NLS-1$
        10 + libraries.size());
    
    SortedSet<LibraryFile> masterFiles = new TreeSet<>();
    List<String> dependentIds = new ArrayList<>();
    for (Library library : libraries) {
      dependentIds.add(library.getId());
      masterFiles.addAll(library.getAllDependencies());
      subMonitor.worked(1);
    }

    // need to get old master library entries first if they exist
    try {
      LibraryClasspathContainerSerializer serializer = new LibraryClasspathContainerSerializer();
      List<String> previouslyAddedLibraries = serializer.loadLibraryIds(javaProject, null);
      for (String id : previouslyAddedLibraries) {
        Library library = CloudLibraries.getLibrary(id);
        // null happens mostly in tests but could also be null
        // if someone edited the serialized data behind Eclipse's back
        if (library != null && !dependentIds.contains(id)) { 
          dependentIds.add(library.getId());
          masterFiles.addAll(library.getAllDependencies());
          subMonitor.worked(1);
        }
      }
    } catch (IOException | CoreException ex) {
      logger.log(Level.WARNING, "Error loading previous libraries", ex);
    }
    
    Library masterLibrary = new Library(CloudLibraries.MASTER_CONTAINER_ID);
    masterLibrary.setName("Google APIs"); //$NON-NLS-1$
    masterLibrary.setLibraryDependencies(dependentIds);
    subMonitor.worked(1);
    
    List<LibraryFile> resolved = Library.resolveDuplicates(new ArrayList<>(masterFiles));
    subMonitor.worked(8);
    
    masterLibrary.setLibraryFiles(resolved);
    
    masterLibrary.setResolved();
    
    subMonitor.worked(1);

    return masterLibrary;
  }

  /**
   * @return an {@link IClasspathEntry} created from {@code library} if {@code javaProject} does not
   *     already have the entry; otherwise, {@code null}
   */
  private static IClasspathEntry computeEntry(IJavaProject javaProject, Library library,
      IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor,
        Messages.getString("computing.entries"), //$NON-NLS-1$
        11);
    
    if (CloudLibraries.MASTER_CONTAINER_ID.equals(library.getId())) {
      try {
        LibraryClasspathContainerSerializer serializer = new LibraryClasspathContainerSerializer();
        serializer.saveLibraryIds(javaProject, library.getLibraryDependencies());
      } catch (IOException ex) {
        throw new CoreException(
            StatusUtil.error(BuildPath.class, "Error saving master library list", ex)); //$NON-NLS-1$
      }
    }
    subMonitor.worked(10);

    IClasspathEntry libraryContainer = makeClasspathEntry(library);
    subMonitor.worked(1);

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    boolean alreadyExists = Arrays.asList(rawClasspath).contains(libraryContainer);
    return alreadyExists ? null : libraryContainer;
  }
  
  /**
   * Returns the entry of {@code library} to be added to the classpath of {@code javaProject}, if
   * the project does not already have it. (Note that this does not add it to the classpath.)
   * Returns {@code null} otherwise.
   */
  public static IClasspathEntry listNativeLibrary(IJavaProject javaProject, Library library,
      IProgressMonitor monitor) throws CoreException {
    IClasspathEntry libraryEntry = computeEntry(javaProject, library, monitor);
    runContainerResolverJob(javaProject);
    return libraryEntry;
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
