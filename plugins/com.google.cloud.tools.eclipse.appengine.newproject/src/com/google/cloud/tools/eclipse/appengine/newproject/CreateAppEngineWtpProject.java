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
import com.google.cloud.tools.eclipse.util.ClasspathUtil;
import com.google.common.annotations.VisibleForTesting;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.wst.common.componentcore.internal.builder.DependencyGraphImpl;
import org.eclipse.wst.common.componentcore.internal.builder.IDependencyGraph;

/**
* Utility to make a new Eclipse App Engine project in the workspace.
*/
public abstract class CreateAppEngineWtpProject extends WorkspaceModifyOperation {

  private static final Logger logger = Logger.getLogger(CreateAppEngineWtpProject.class.getName());

  private final AppEngineProjectConfig config;
  private final IAdaptable uiInfoAdapter;
  private IFile mostImportant = null;

  @VisibleForTesting
  Job deployAssemblyEntryRemoveJob;

  public abstract void addAppEngineFacet(IProject newProject, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Returns a user visible name for the resource operation that generates the files
   * for the App Engine WTP project.
   */
  public abstract String getDescription();

  /**
   * Returns the most important file created that the IDE will open in the editor.
   */
  public abstract IFile createAndConfigureProjectContent(IProject newProject,
      AppEngineProjectConfig config, IProgressMonitor monitor) throws CoreException;

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

    if (config.getUseMaven()) {
      enableMavenNature(newProject, subMonitor.newChild(2));
    } else {
      addJunit4ToClasspath(newProject, subMonitor.newChild(2));
    }

    BuildPath.addLibraries(newProject, config.getAppEngineLibraries(), subMonitor.newChild(2));

    fixTestSourceDirectorySettings(newProject, subMonitor.newChild(2));
  }

  private void fixTestSourceDirectorySettings(IProject newProject, IProgressMonitor monitor)
      throws CoreException {
    // 1. Fix the output folder of "src/test/java".
    IPath testSourcePath = newProject.getFolder("src/test/java").getFullPath();

    IJavaProject javaProject = JavaCore.create(newProject);
    IClasspathEntry[] entries = javaProject.getRawClasspath();
    for (int i = 0; i < entries.length; i++) {
      IClasspathEntry entry = entries[i];
      if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE
          && entry.getPath().equals(testSourcePath)
          && entry.getOutputLocation() == null) {  // Default output location?
        IPath oldOutputPath = javaProject.getOutputLocation();
        IPath newOutputPath = oldOutputPath.removeLastSegments(1).append("test-classes");

        entries[i] = JavaCore.newSourceEntry(testSourcePath, ClasspathEntry.INCLUDE_ALL,
            ClasspathEntry.EXCLUDE_NONE, newOutputPath);
        javaProject.setRawClasspath(entries, monitor);
        break;
      }
    }

    // 2. Remove "src/test/java" from the Web Deployment Assembly sources.
    deployAssemblyEntryRemoveJob =
        new DeployAssemblyEntryRemoveJob(newProject, new Path("src/test/java"));
    // Not to be affected by "NonSystemJobSuspender". Not necessary in production, but necessary
    // for tests to work properly. (As a side note, setting this to system would have not worked
    // if the deploy assembly update jobs triggered by the above classpath change were non-system
    // jobs; the update jobs should be visible by "DeployAssemblyEntryRemoveJob" because it joins
    // the update jobs before removing entries.)
    deployAssemblyEntryRemoveJob.setSystem(true);
    deployAssemblyEntryRemoveJob.schedule();
  }

  private static void enableMavenNature(IProject newProject, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 30);

    // Workaround deadlock bug described in Eclipse bug (https://bugs.eclipse.org/511793).
    try {
      IDependencyGraph.INSTANCE.preUpdate();
      try {
        Job.getJobManager().join(DependencyGraphImpl.GRAPH_UPDATE_JOB_FAMILY,
            subMonitor.newChild(8));
      } catch (OperationCanceledException | InterruptedException ex) {
        logger.log(Level.WARNING, "Exception waiting for WTP Graph Update job", ex);
      }

      ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
      MavenPlugin.getProjectConfigurationManager().enableMavenNature(newProject,
          resolverConfiguration, subMonitor.newChild(20));
    } finally {
      IDependencyGraph.INSTANCE.postUpdate();
    }

    // M2E will cleverly set "target/<artifact ID>-<version>/WEB-INF/classes" as a new Java output
    // folder; delete the default old folder.
    newProject.getFolder("build").delete(true /* force */, subMonitor.newChild(2));
  }

  private static void addJunit4ToClasspath(IProject newProject, IProgressMonitor monitor)
      throws CoreException {
    IClasspathAttribute nonDependencyAttribute =
        UpdateClasspathAttributeUtil.createNonDependencyAttribute();
    IClasspathEntry junit4Container = JavaCore.newContainerEntry(
        JUnitCore.JUNIT4_CONTAINER_PATH,
        new IAccessRule[0],
        new IClasspathAttribute[] {nonDependencyAttribute},
        false);
    ClasspathUtil.addClasspathEntry(newProject, junit4Container, monitor);
  }

}
