/*
 * Copyright 2017 Google Inc.
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

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IContainer;
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
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.common.project.facet.core.JavaFacetInstallConfig;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.internal.builder.DependencyGraphImpl;
import org.eclipse.wst.common.componentcore.internal.builder.IDependencyGraph;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Utility class for processing facets.
 */
@SuppressWarnings("restriction") // For IDependencyGraph
public class FacetUtil {
  public static final Logger logger = Logger.getLogger(FacetUtil.class.getName());
  

  /**
   * Adds an install action for the {@code javaFacet} to the {@code facetInstallSet} if the
   * {@code javaFacet} does not already exist in {@code facetedProject}.
   *
   * @return true is the facet action was added to {@code facetInstallSet} and false if {@code javaFacet}
   *   already exists in {@code facetedProject}
   * @throws CoreException if {@code javaFacet} is not a Java facet
   */
 public static boolean addJavaFacetToBatch(IProjectFacetVersion javaFacet, IFacetedProject facetedProject,
     Set<IFacetedProject.Action> facetInstallSet) throws CoreException {
   if (!JavaFacet.FACET.getId().equals(javaFacet.getProjectFacet().getId())) {
     throw new CoreException(StatusUtil.error(FacetUtil.class, javaFacet.toString() + " is not a Java facet"));
   }
   
   if (facetedProject.hasProjectFacet(javaFacet)) {
     return false;
   }

   JavaFacetInstallConfig javaConfig = new JavaFacetInstallConfig();
   List<IPath> sourcePaths = new ArrayList<>();

   IProject project = facetedProject.getProject();
   // TODO: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/490
   if (project.getFolder("src/main/java").exists()) {
     sourcePaths.add(new Path("src/main/java"));
   }

   if (project.getFolder("src/test/java").exists()) {
     sourcePaths.add(new Path("src/test/java"));
   }

   javaConfig.setSourceFolders(sourcePaths);
   facetInstallSet.add(new IFacetedProject.Action(
       IFacetedProject.Action.Type.INSTALL, javaFacet, javaConfig));
   return true;
 }

 /**
  * Adds an install action for the {@code webFacet} to the {@code facetInstallSet} if the
  * {@code webFacet} does not already exist in {@code facetedProject}.
  * 
  * @return true is the facet action was added to {@code facetInstallSet} and false if {@code webFacet}
  *   already exists in {@code facetedProject}
  * @throws CoreException if {@code webFacet} is not a Web facet
  */
 public static boolean addWebFacetToBatch(IProjectFacetVersion webFacet, IFacetedProject facetedProject,
     Set<IFacetedProject.Action> facetInstallSet) throws CoreException {
   if (!WebFacetUtils.WEB_FACET.getId().equals(webFacet.getProjectFacet().getId())) {
     throw new CoreException(StatusUtil.error(FacetUtil.class, webFacet.toString() + " is not a Web facet"));
   }

   if (facetedProject.hasProjectFacet(webFacet)) {
     return false;
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
   return true;
 }

 /**
  * Modifies the set of project facets in {@code facetedProject} by performing the series of
  * actions in {@code facetActionSet}.
  *
  * @throws CoreException
  */
 public static void addFacetSetToProject(IFacetedProject facetedProject,
     Set<IFacetedProject.Action> facetActionSet, IProgressMonitor monitor) throws CoreException {
   SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

   // Workaround deadlock bug described in Eclipse bug (https://bugs.eclipse.org/511793).
   // There are graph update jobs triggered by the completion of the CreateProjectOperation
   // above (from resource notifications) and from other resource changes from modifying the
   // project facets. So we force the dependency graph to defer updates
   try {
     IDependencyGraph.INSTANCE.preUpdate();
     try {
       Job.getJobManager().join(DependencyGraphImpl.GRAPH_UPDATE_JOB_FAMILY,
           subMonitor.newChild(10));
     } catch (OperationCanceledException | InterruptedException ex) {
       logger.log(Level.WARNING, "Exception waiting for WTP Graph Update job", ex);
     }

     facetedProject.modify(facetActionSet, subMonitor.newChild(90));
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

 /**
  * Returns a list of WEB-INF folders in {@code container}.
  */
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
