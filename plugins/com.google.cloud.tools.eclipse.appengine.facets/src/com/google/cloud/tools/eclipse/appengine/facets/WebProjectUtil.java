/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.common.project.facet.core.JavaFacetInstallConfig;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.builder.DependencyGraphImpl;
import org.eclipse.wst.common.componentcore.internal.builder.IDependencyGraph;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Utility classes for processing WTP Web Projects (jst.web and jst.utility).
 */
@SuppressWarnings("restriction") // For IDependencyGraph
public class WebProjectUtil {
  private final static String DEFAULT_WEB_PATH = "src/main/webapp";

  private final static String WEB_INF = "WEB-INF/";

  /**
   * Return the project's <code>WEB-INF</code> directory. There is no guarantee that the contents
   * are actually published.
   * 
   * @return the <code>IFolder</code> or null if not present
   */
  public static IFolder getWebInfDirectory(IProject project) {
    // Try to obtain the directory as if it was a Dynamic Web Project
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null && component.exists()) {
      IVirtualFolder root = component.getRootFolder();
      // the root should exist, but the WEB-INF may not yet exist
      if (root.exists()) {
        return (IFolder) root.getFolder(WEB_INF).getUnderlyingFolder();
      }
    }
    // Otherwise it's seemingly fair game
    IFolder defaultLocation = project.getFolder(DEFAULT_WEB_PATH).getFolder(WEB_INF);
    if (defaultLocation.exists()) {
      return defaultLocation;
    }
    return null;
  }

  /**
   * Attempt to resolve the given file within the project's <code>WEB-INF</code>.
   * 
   * @return the file location or {@code null} if not found
   */
  public static IFile findInWebInf(IProject project, IPath filePath) {
    IFolder webInfFolder = getWebInfDirectory(project);
    if (webInfFolder == null) {
      return null;
    }
    IFile file = webInfFolder.getFile(filePath);
    return file.exists() ? file : null;
  }

  // TODO: Refactor
  /**
   * Installs Java 1.7 facet if it doesn't already exist in {@code facetedProject}.
   */
  public static void addJavaFacetToBatch(IProjectFacetVersion javaFacet, IFacetedProject facetedProject,
      Set<IFacetedProject.Action> facetInstallSet) {
    // TODO: verify javaFacet
    if (facetedProject.hasProjectFacet(javaFacet)) {
      return;
    }

    // TODO use "src/main/java" for only maven projects
    JavaFacetInstallConfig javaConfig = new JavaFacetInstallConfig();
    List<IPath> sourcePaths = new ArrayList<>();
 
    IProject project = facetedProject.getProject();
    if (project.getFolder("src/main/java").exists()) {
      sourcePaths.add(new Path("src/main/java"));
    }

    if (project.getFolder("src/test/java").exists()) {
      sourcePaths.add(new Path("src/test/java"));
    }

    javaConfig.setSourceFolders(sourcePaths);
    facetInstallSet.add(new IFacetedProject.Action(
        IFacetedProject.Action.Type.INSTALL, javaFacet, javaConfig));
  }

  /**
   * Installs Dynamic Web Module 2.5 facet if it doesn't already exist in {@code facetedProject}.
   */
  public static void addWebFacetToBatch(IProjectFacetVersion webFacet, IFacetedProject facetedProject,
      Set<IFacetedProject.Action> facetInstallSet) {
    if (facetedProject.hasProjectFacet(webFacet)) {
      return;
    }

    String webAppDirectory = "src/main/webapp";
    IPath webAppDirectoryFound = findMainWebAppDirectory(facetedProject.getProject());
    if (webAppDirectoryFound != null) {
      webAppDirectory = webAppDirectoryFound.toOSString();
    }

    IDataModel webModel = DataModelFactory.createDataModel(new WebFacetInstallDataModelProvider());
    webModel.setBooleanProperty(IJ2EEModuleFacetInstallDataModelProperties.ADD_TO_EAR, false);
    webModel.setBooleanProperty(IJ2EEFacetInstallDataModelProperties.GENERATE_DD, true);
    webModel.setBooleanProperty(IWebFacetInstallDataModelProperties.INSTALL_WEB_LIBRARY, false);
    webModel.setStringProperty(IWebFacetInstallDataModelProperties.CONFIG_FOLDER, webAppDirectory);
    facetInstallSet.add(new IFacetedProject.Action(
        IFacetedProject.Action.Type.INSTALL, webFacet, webModel));
  }

  public static void addFacetSetToProject(IFacetedProject facetedProject,
      Set<IFacetedProject.Action> facetInstallSet, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
    try {
      IDependencyGraph.INSTANCE.preUpdate();
      try {
        Job.getJobManager().join(DependencyGraphImpl.GRAPH_UPDATE_JOB_FAMILY,
            subMonitor.newChild(10));
      } catch (OperationCanceledException | InterruptedException ex) {
        //logger.log(Level.WARNING, "Exception waiting for WTP Graph Update job", ex);
      }

      facetedProject.modify(facetInstallSet, subMonitor.newChild(90));
    } finally {
      IDependencyGraph.INSTANCE.postUpdate();
    }
  }

  /**
   * Attempts to find a main web application directory, by the following logic:
   *
   * 1. If there is no {@code WEB-INF} folder in the {@code project}, returns {@code null}.
   * 2. Otherwise, if there is at least one {@code WEB-INF} folder that contains {@code web.xml},
   *     returns the parent directory of one of such {@code WEB-INF} folders.
   * 3. Otherwise, returns the parent directory of an arbitrary {@code WEB-INF}.
   *
   * @return path of the main web application directory, relative to {@code project}, if found;
   *     otherwise, {@code null}
   */
  @VisibleForTesting
  static IPath findMainWebAppDirectory(IProject project) {
    List<IFolder> webInfFolders = findAllWebInfFolders(project);
    if (webInfFolders.isEmpty()) {
      return null;
    }

    for (IFolder webInf : webInfFolders) {
      if (webInf.getFile("web.xml").exists()) {
        return webInf.getParent().getProjectRelativePath();
      }
    }
    return webInfFolders.get(0).getParent().getProjectRelativePath();
  }

  @VisibleForTesting
  static List<IFolder> findAllWebInfFolders(IContainer container) {
    final List<IFolder> webInfFolders = new ArrayList<>();

    try {
      IResourceVisitor webInfCollector = new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (resource.getType() == IResource.FOLDER && "WEB-INF".equals(resource.getName())) {
            webInfFolders.add((IFolder) resource);
            return false;  // No need to visit sub-directories.
          }
          return true;
        }
      };
      container.accept(webInfCollector);
    } catch (CoreException ex) {
      // Our attempt to find folders failed, but don't error out.
    }
    return webInfFolders;
  }

}
