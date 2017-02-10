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

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainerResolverJob;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;

public class BuildPath {

  public static void addLibraries(IProject project, List<Library> libraries, IProgressMonitor monitor)
      throws CoreException {
    
    if (libraries.isEmpty()) {
      return;
    }
    SubMonitor subMonitor = SubMonitor.convert(monitor,
        Messages.getString("adding.app.engine.libraries"), libraries.size()); //$NON-NLS-1$
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    IClasspathEntry[] newRawClasspath =
        Arrays.copyOf(rawClasspath, rawClasspath.length + libraries.size());
    for (int i = 0; i < libraries.size(); i++) {
      Library library = libraries.get(i);
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
      newRawClasspath[rawClasspath.length + i] = libraryContainer;
      subMonitor.worked(1);
    }
    javaProject.setRawClasspath(newRawClasspath, monitor);
  
    IEclipseContext context = EclipseContextFactory.getServiceContext(
        FrameworkUtil.getBundle(BuildPath.class).getBundleContext());
    
    runContainerResolverJob(javaProject, context);
  }
  
  private static void runContainerResolverJob(IJavaProject javaProject, IEclipseContext context) {
    final IEclipseContext childContext =
        context.createChild(LibraryClasspathContainerResolverJob.class.getName());
    childContext.set(IJavaProject.class, javaProject);
    Job job = ContextInjectionFactory.make(LibraryClasspathContainerResolverJob.class, childContext);
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        childContext.dispose();
      }
    });
    job.schedule();
  }

}
