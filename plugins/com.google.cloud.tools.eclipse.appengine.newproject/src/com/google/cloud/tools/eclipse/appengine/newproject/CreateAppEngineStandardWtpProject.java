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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
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

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.Library;

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
    
    addAppEngineLibrariesToBuildPath(newProject, config.getAppEngineLibraries(), monitor);

    addJunit4ToClasspath(monitor, newProject);
  }

  private void addAppEngineLibrariesToBuildPath(IProject newProject,
                                                List<Library> libraries,
                                                IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, "Adding App Engine libraries", libraries.size());
    IJavaProject javaProject = JavaCore.create(newProject);
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    IClasspathEntry[] newRawClasspath = Arrays.copyOf(rawClasspath, rawClasspath.length + libraries.size());
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
