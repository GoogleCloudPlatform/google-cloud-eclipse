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
import com.google.cloud.tools.eclipse.appengine.facets.FacetUtil;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig;
import com.google.cloud.tools.eclipse.appengine.newproject.CodeTemplates;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Utility to create a new App Engine Flexible Eclipse project.
 */
public class CreateAppEngineFlexWtpProject extends CreateAppEngineWtpProject {
  private static final Logger logger =
      Logger.getLogger(CreateAppEngineFlexWtpProject.class.getName());

  private static final List<MavenCoordinates> PROJECT_DEPENDENCIES;

  static {
    MavenCoordinates servletApi = new MavenCoordinates("javax.servlet", "javax.servlet-api"); //$NON-NLS-1$ //$NON-NLS-2$
    servletApi.setVersion("3.1.0"); //$NON-NLS-1$
    PROJECT_DEPENDENCIES = Collections.singletonList(servletApi);
  }

  private ILibraryRepositoryService repositoryService;

  CreateAppEngineFlexWtpProject(AppEngineProjectConfig config, IAdaptable uiInfoAdapter,
      ILibraryRepositoryService repositoryService) {
    super(config, uiInfoAdapter);
    this.repositoryService = repositoryService;
  }

  @Override
  public void addAppEngineFacet(IProject newProject, IProgressMonitor monitor)
      throws CoreException {
    // added in configureFacets along with facets sample requires
  }

  @Override
  public String getDescription() {
    return Messages.getString("creating.app.engine.flex.project"); //$NON-NLS-1$
  }

  @Override
  public IFile createAndConfigureProjectContent(IProject newProject, AppEngineProjectConfig config,
      IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
    IFile mostImportantFile =  CodeTemplates.materializeAppEngineFlexFiles(newProject, config,
        subMonitor.newChild(30));
    configureFacets(newProject, subMonitor.newChild(20));
    addDependenciesToProject(newProject, subMonitor.newChild(50));
    return mostImportantFile;
  }

  /**
   * Add Java 8 and Dynamic Web Module facet
   */
  private void configureFacets(IProject project, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
    IFacetedProject facetedProject = ProjectFacetsManager.create(
        project, true /* convertIfNecessary */, subMonitor.newChild(50));
    FacetUtil facetUtil = new FacetUtil(facetedProject);
    facetUtil.addJavaFacetToBatch(JavaFacet.VERSION_1_8);
    facetUtil.addWebFacetToBatch(WebFacetUtils.WEB_31);

    IProjectFacet appEngineFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexFacet.ID);
    IProjectFacetVersion appEngineFacetVersion =
        appEngineFacet.getVersion(AppEngineFlexFacet.VERSION);
    facetUtil.addFacetToBatch(appEngineFacetVersion, null /* config */);
    facetUtil.install(subMonitor.newChild(50));
  }

  private void addDependenciesToProject(IProject project, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

    // Create a lib folder
    IFolder libFolder = project.getFolder("lib"); //$NON-NLS-1$
    if (!libFolder.exists()) {
      libFolder.create(true, true, subMonitor.newChild(10));
    }

    // Download the dependencies from maven
    int ticks = 50 / PROJECT_DEPENDENCIES.size();
    for (MavenCoordinates dependency : PROJECT_DEPENDENCIES) {
      LibraryFile libraryFile = new LibraryFile(dependency);
      File artifactFile = null;
      try {
        Artifact artifact = repositoryService.resolveArtifact(
            libraryFile, subMonitor.newChild(ticks));
        artifactFile = artifact.getFile();
        IFile destFile = libFolder.getFile(artifactFile.getName());
        destFile.create(Files.newInputStream(artifactFile.toPath()), true, subMonitor.newChild(30));
      } catch (CoreException ex) {
        logger.log(Level.WARNING, "Error downloading " + //$NON-NLS-1$
            libraryFile.getMavenCoordinates().toString() + " from maven", ex); //$NON-NLS-1$
      } catch (IOException ex) {
        logger.log(Level.WARNING, "Error copying over " + artifactFile.toString() + " to " + //$NON-NLS-1$ //$NON-NLS-2$
            libFolder.getFullPath().toPortableString(), ex);
      }
    }

    addDependenciesToClasspath(project, libFolder.getLocation().toString(),
        subMonitor.newChild(10));
  }

  private void addDependenciesToClasspath(IProject project, String libraryPath,
      IProgressMonitor monitor)  throws CoreException {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] entries = javaProject.getRawClasspath();
    List<IClasspathEntry> newEntries = new ArrayList<>();
    newEntries.addAll(Arrays.asList(entries));

    // Add all the jars under lib folder to the classpath
    File libFolder = new File(libraryPath);

    for(File file : libFolder.listFiles()) {
      IPath path = Path.fromOSString(file.toPath().toString());
      newEntries.add(JavaCore.newLibraryEntry(path, null, null));
    }

    javaProject.setRawClasspath(newEntries.toArray(new IClasspathEntry[0]), monitor);
  }

}
