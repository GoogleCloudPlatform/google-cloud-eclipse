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

package com.google.cloud.tools.eclipse.appengine.newproject;

import com.google.cloud.tools.eclipse.appengine.libraries.BuildPath;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
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
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;

/**
* Utility to make a new Eclipse App Engine project in the workspace.
*/
public abstract class CreateAppEngineWtpProject extends WorkspaceModifyOperation {

  private final AppEngineProjectConfig config;
  private final IAdaptable uiInfoAdapter;
  private IFile mostImportant = null;

  public abstract void addAppEngineFacet(IProject newProject, IProgressMonitor monitor) throws CoreException;

  /**
   * Returns a user visible name for the resource operation that generates the files
   * for the App Engine WTP project.
   */
  public abstract String getDescription();

  /**
   * Returns the most important file created that the IDE will open in the editor.
   */
  public abstract IFile createAndConfigureProjectContent(IProject newProject, AppEngineProjectConfig config,
      IProgressMonitor monitor) throws CoreException;

  /**
   * @return the file in the project that should be opened in an editor when the wizard finishes;
   *     may be null
   */
  public IFile getMostImportant() {
    return mostImportant;
  }

  protected CreateAppEngineWtpProject(AppEngineProjectConfig config,
      IAdaptable uiInfoAdapter) {
    if (config == null) {
      throw new NullPointerException("Null App Engine configuration"); //$NON-NLS-1$
    }
    this.config = config;
    this.uiInfoAdapter = uiInfoAdapter;
  }

  @Override
  public void execute(IProgressMonitor monitor) throws InvocationTargetException, CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject newProject = config.getProject();
    URI location = config.getEclipseProjectLocationUri();

    String name = newProject.getName();
    IProjectDescription description = workspace.newProjectDescription(name);
    description.setLocationURI(location);

    String operationLabel = getDescription();
    SubMonitor subMonitor = SubMonitor.convert(monitor, operationLabel, 100);
    CreateProjectOperation operation = new CreateProjectOperation(description, operationLabel); 
    try {
      operation.execute(subMonitor.newChild(10), uiInfoAdapter);
      mostImportant = createAndConfigureProjectContent(newProject, config, subMonitor.newChild(80));
    } catch (ExecutionException ex) {
      throw new InvocationTargetException(ex);
    }

    addAppEngineFacet(newProject, subMonitor.newChild(4));

    BuildPath.addLibraries(newProject, config.getAppEngineLibraries(), subMonitor.newChild(2));

    addJunit4ToClasspath(subMonitor.newChild(2), newProject);
  }

  private static void addJunit4ToClasspath(IProgressMonitor monitor, IProject newProject)
      throws CoreException {
    IJavaProject javaProject = JavaCore.create(newProject);
    IClasspathAttribute nonDependencyAttribute =
        UpdateClasspathAttributeUtil.createNonDependencyAttribute();
    IClasspathEntry junit4Container = JavaCore.newContainerEntry(
        JUnitCore.JUNIT4_CONTAINER_PATH,
        new IAccessRule[0],
        new IClasspathAttribute[] {nonDependencyAttribute},
        false);
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    IClasspathEntry[] newRawClasspath = Arrays.copyOf(rawClasspath, rawClasspath.length + 1);
    newRawClasspath[newRawClasspath.length - 1] = junit4Container;
    javaProject.setRawClasspath(newRawClasspath, monitor);
  }

}
