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

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexWarFacet;
import com.google.cloud.tools.eclipse.appengine.facets.FacetUtil;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig;
import com.google.cloud.tools.eclipse.appengine.newproject.CodeTemplates;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import com.google.cloud.tools.eclipse.util.ClasspathUtil;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
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

  private static final List<MavenCoordinates> SERVLET_DEPENDENCIES;
  private static final List<MavenCoordinates> PROJECT_DEPENDENCIES;

  static {
    // servlet-api and jsp-api are marked as not being included
    MavenCoordinates servletApi = new MavenCoordinates.Builder()
        .setGroupId("javax.servlet") //$NON-NLS-1$
        .setArtifactId("javax.servlet-api") //$NON-NLS-1$
        .setVersion("3.1.0") //$NON-NLS-1$
        .build();
    MavenCoordinates jsp = new MavenCoordinates.Builder().setGroupId("javax.servlet.jsp") //$NON-NLS-1$
        .setArtifactId("javax.servlet.jsp-api") //$NON-NLS-1$
        .setVersion("2.3.1") //$NON-NLS-1$
        .build();
    SERVLET_DEPENDENCIES = ImmutableList.of(servletApi, jsp);

    MavenCoordinates jstl = new MavenCoordinates.Builder().setGroupId("jstl") //$NON-NLS-1$
        .setArtifactId("jstl") //$NON-NLS-1$
        .setVersion("1.2") //$NON-NLS-1$
        .build();
    PROJECT_DEPENDENCIES = ImmutableList.of(jstl);

  }

  CreateAppEngineFlexWtpProject(AppEngineProjectConfig config, IAdaptable uiInfoAdapter,
      ILibraryRepositoryService repositoryService) {
    super(config, uiInfoAdapter, repositoryService);
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
    if (!config.getUseMaven()) {
      addDependenciesToProject(newProject, subMonitor.newChild(50));
    }
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

    IProjectFacet appEngineFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexWarFacet.ID);
    IProjectFacetVersion appEngineFacetVersion =
        appEngineFacet.getVersion(AppEngineFlexWarFacet.VERSION);
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
    subMonitor.setWorkRemaining(SERVLET_DEPENDENCIES.size() + 10);
    for (MavenCoordinates dependency : SERVLET_DEPENDENCIES) {
      installArtifact(dependency, libFolder, subMonitor.newChild(1));
    }

    addDependenciesToClasspath(project, libFolder, subMonitor.newChild(10));
  }

  private void addDependenciesToClasspath(IProject project, IFolder folder,
      IProgressMonitor monitor)  throws CoreException {
    List<IClasspathEntry> newEntries = new ArrayList<>();

    IClasspathAttribute[] nonDependencyAttribute =
        new IClasspathAttribute[] {UpdateClasspathAttributeUtil.createNonDependencyAttribute()};

    // Add all the jars under lib folder to the classpath
    File libFolder = folder.getLocation().toFile();
    for (File file : libFolder.listFiles()) {
      IPath path = Path.fromOSString(file.toPath().toString());
      newEntries.add(JavaCore.newLibraryEntry(path, null, null, new IAccessRule[0],
          nonDependencyAttribute, false /* isExported */));
    }

    ClasspathUtil.addClasspathEntries(project, newEntries, monitor);
  }

  @Override
  protected void addAdditionalDependencies(IProject newProject, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 20);
    super.addAdditionalDependencies(newProject, progress.newChild(10));

    // locate WEB-INF/lib
    IFolder webInfFolder = WebProjectUtil.getWebInfDirectory(newProject);
    IFolder libFolder = webInfFolder.getFolder("lib"); //$NON-NLS-1$
    if (!libFolder.exists()) {
      libFolder.create(true, true, progress.newChild(5));
    }

    progress.setWorkRemaining(PROJECT_DEPENDENCIES.size());
    for (MavenCoordinates dependency : PROJECT_DEPENDENCIES) {
      installArtifact(dependency, libFolder, progress.newChild(10));
    }
  }


}
