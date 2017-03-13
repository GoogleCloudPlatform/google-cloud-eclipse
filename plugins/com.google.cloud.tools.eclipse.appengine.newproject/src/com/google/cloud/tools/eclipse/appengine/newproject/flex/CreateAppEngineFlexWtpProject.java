/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject.flex;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.M2RepositoryService;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig;
import com.google.cloud.tools.eclipse.appengine.newproject.CodeTemplates;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Utility to make a new Eclipse project with the App Engine Flexible facets in
 * the workspace.
 */
public class CreateAppEngineFlexWtpProject extends CreateAppEngineWtpProject {
  // TODO: is map the best collection type?
  private static final Map<String, String> PROJECT_DEPENDENCIES;
  static {
    Map<String, String> projectDependencies = new HashMap<String, String>();
    projectDependencies.put("javax.servlet", "servlet-api");
    projectDependencies.put("javax.servlet.jsp", "javax.servlet.jsp-api");
    projectDependencies.put("jstl", "jstl");
    PROJECT_DEPENDENCIES = Collections.unmodifiableMap(projectDependencies);
  }

  CreateAppEngineFlexWtpProject(AppEngineProjectConfig config, IAdaptable uiInfoAdapter) {
    super(config, uiInfoAdapter);
  }

  @Override
  public void addAppEngineFacet(IProject newProject, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor,
        Messages.getString("add.appengine.flex.facet"), 100);

    IFacetedProject facetedProject = ProjectFacetsManager.create(
        newProject, true /* convertIfNecessary */, subMonitor.newChild(50));
    AppEngineFlexFacet.installAppEngineFacet(
        facetedProject, true /* installDependentFacets */, subMonitor.newChild(50));
  }

  @Override
  public String getDescription() {
    return Messages.getString("creating.app.engine.flex.project"); //$NON-NLS-1$
  }

  @Override
  public IFile createProjectFiles(IProject newProject, AppEngineProjectConfig config, IProgressMonitor monitor)
      throws CoreException {
    addDependenciesToProject(newProject, monitor);
    return CodeTemplates.materializeAppEngineFlexFiles(newProject, config, monitor);
  }

  // TODO: should this be wrapped around a workspace operation?
  private void addDependenciesToProject(IProject newProject, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

    // Create a lib folder
    IFolder libFolder = newProject.getFolder("lib");
    if (!libFolder.exists()) {
      libFolder.create(true, true, subMonitor.newChild(10));
    }

    // Download the dependencies from maven
    M2RepositoryService repoService = new M2RepositoryService();
    repoService.activate();
    int ticks = 90 / PROJECT_DEPENDENCIES.size();
    for (Map.Entry<String, String> entry : PROJECT_DEPENDENCIES.entrySet()) {
      LibraryFile libraryFile = new LibraryFile(new MavenCoordinates(entry.getKey(), entry.getValue()));
      Artifact artifact = null;
      try {
        artifact = repoService.resolveArtifact(libraryFile, subMonitor.newChild(ticks));
      } catch (CoreException e1) {
        // log and continue
      }

      // Copy into lib folder
      // TODO: could artifact be null?
      if (artifact != null) {
        File artifactFile = artifact.getFile();
        IFile destFile = libFolder.getFile(artifactFile.getName());
        try {
          destFile.create(new FileInputStream(artifactFile), true, subMonitor);
        } catch (FileNotFoundException e) {
          // TODO log and continue
        }
      }

    }
  }

}
