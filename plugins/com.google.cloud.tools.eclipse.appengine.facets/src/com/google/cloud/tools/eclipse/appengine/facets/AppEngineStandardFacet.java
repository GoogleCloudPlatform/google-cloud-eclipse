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

package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.wst.common.componentcore.internal.builder.DependencyGraphImpl;
import org.eclipse.wst.common.componentcore.internal.builder.IDependencyGraph;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

public class AppEngineStandardFacet {
  private static final Logger logger = Logger.getLogger(AppEngineStandardFacet.class.getName());

  public static final String ID = "com.google.cloud.tools.eclipse.appengine.facets.standard";

  public static final String VERSION = "1";
  static final String DEFAULT_RUNTIME_ID =
      "com.google.cloud.tools.eclipse.appengine.standard.runtime";
  static final String DEFAULT_RUNTIME_NAME = "App Engine Standard";
  public static final String DEFAULT_APPENGINE_SDK_VERSION = "1.9.49";
  public static final String DEFAULT_GCLOUD_PLUGIN_VERSION = "2.0.9.133.v201611104";

  /**
   * Returns true if project has the App Engine Standard facet and false otherwise.
   *
   * @param project should not be null
   * @return true if project has the App Engine Standard facet and false otherwise
   */
  public static boolean hasAppEngineFacet(IFacetedProject project) {
    return FacetedProjectHelper.projectHasFacet(project, ID);
  }

  /**
   * Returns true if {@code facetRuntime} is an App Engine Standard runtime and false otherwise.
   *
   * @param facetRuntime the facet runtime; runtime should not be null
   * @return true if <code>facetRuntime</code> is an App Engine Standard runtime and false otherwise
   */
  public static boolean isAppEngineStandardRuntime(IRuntime facetRuntime) {
    Preconditions.checkNotNull(facetRuntime, "runtime is null");

    org.eclipse.wst.server.core.IRuntime serverRuntime = FacetUtil.getRuntime(facetRuntime);
    if (serverRuntime != null) {
      IRuntimeType runtimeType = serverRuntime.getRuntimeType();
      if (runtimeType == null) {
        return false;
      }
      return DEFAULT_RUNTIME_ID.equals(runtimeType.getId());
    } else {
      return false;
    }
  }

  /**
   * Returns true if {@code serverRuntime} is an App Engine Standard runtime and false otherwise.
   *
   * @param serverRuntime the server runtime, runtime should not be null
   * @return true if <code>serverRuntime</code> is an App Engine Standard runtime and false
   *         otherwise
   */
  public static boolean isAppEngineStandardRuntime(
      org.eclipse.wst.server.core.IRuntime serverRuntime) {
    Preconditions.checkNotNull(serverRuntime, "runtime is null");
    IRuntimeType runtimeType = serverRuntime.getRuntimeType();
    if (runtimeType == null) {
      return false;
    }
    return DEFAULT_RUNTIME_ID.equals(runtimeType.getId());
  }

  /**
   * Checks to see if <code>facetedProject</code> has the App Engine standard facet.
   * If not, it installs the App Engine standard facet.
   *
   * @param facetedProject the faceted project receiving the App Engine facet
   * @param installDependentFacets true if the facets required by the App Engine facet should be
   *        installed, false otherwise
   * @param monitor the progress monitor
   * @throws CoreException if anything goes wrong during install
   */
  public static void installAppEngineFacet(IFacetedProject facetedProject,
      boolean installDependentFacets, IProgressMonitor monitor) throws CoreException {

    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1155
    // Instead of calling "IFacetedProject.installProjectFacet()" multiple times, we install facets
    // in a batch using "IFacetedProject.modify()" so that we hold the lock until we finish
    // installing all the facets. This ensures that the first ConvertJob starts installing the JSDT
    // facet only after the batch is complete, which in turn prevents the first ConvertJob from
    // scheduling the second ConvertJob (triggered by installing the JSDT facet.)
    Set<IFacetedProject.Action> facetInstallSet = new HashSet<>();
    // Install required App Engine facets i.e. Java 1.7 and Dynamic Web Module 2.5
    if (installDependentFacets) {
      addJavaFacetToBatch(facetedProject, facetInstallSet);
      addWebFacetToBatch(facetedProject, facetInstallSet);
    }

    IProjectFacet appEngineFacet = ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID);
    IProjectFacetVersion appEngineFacetVersion =
        appEngineFacet.getVersion(AppEngineStandardFacet.VERSION);

    if (!facetedProject.hasProjectFacet(appEngineFacet)) {
      Object config = null;
      facetInstallSet.add(new IFacetedProject.Action(
          IFacetedProject.Action.Type.INSTALL, appEngineFacetVersion, config));

      // Workaround deadlock bug described in Eclipse bug (https://bugs.eclipse.org/511793).
      // There are graph update jobs triggered by the completion of the CreateProjectOperation
      // above (from resource notifications) and from other resource changes from modifying the
      // project facets. So we force the dependency graph to defer updates.
      try {
        IDependencyGraph.INSTANCE.preUpdate();
        try {
          Job.getJobManager().join(DependencyGraphImpl.GRAPH_UPDATE_JOB_FAMILY,
              subMonitor.newChild(10));
        } catch (OperationCanceledException | InterruptedException ex) {
          logger.log(Level.WARNING, "Exception waiting for WTP Graph Update job", ex);
        }

        facetedProject.modify(facetInstallSet, subMonitor.newChild(90));
      } finally {
        IDependencyGraph.INSTANCE.postUpdate();
      }
    }
  }

  /**
   * If App Engine runtimes exist in the workspace, add them to the list of targeted runtimes
   * of <code>project</code>. Otherwise create a new App Engine runtime and add it to the list
   * of targeted runtimes.
   *
   * @param project the faceted project receiving the App Engine runtime(s)
   * @param monitor the progress monitor
   * @throws CoreException if the project contains one or more facets that are not supported by
   *     this runtime; if failed for any other reason
   */
  public static void installAllAppEngineRuntimes(IFacetedProject project, IProgressMonitor monitor)
      throws CoreException {
    // If the project already has an App Engine runtime instance
    // do not add any other App Engine runtime instances to the list of targeted runtimes
    for (IRuntime existingTargetedRuntime : project.getTargetedRuntimes()) {
      if (AppEngineStandardFacet.isAppEngineStandardRuntime(existingTargetedRuntime)) {
        return;
      }
    }

    SubMonitor progress = SubMonitor.convert(monitor, 100);

    // Workaround deadlock bug described in Eclipse bug (https://bugs.eclipse.org/511793).
    // There are graph update jobs triggered by the completion of the CreateProjectOperation
    // above (from resource notifications) and from other resource changes from modifying the
    // project facets. So we force the dependency graph to defer updates.
    try {
      IDependencyGraph.INSTANCE.preUpdate();
      try {
        Job.getJobManager().join(DependencyGraphImpl.GRAPH_UPDATE_JOB_FAMILY,
            progress.newChild(10));
      } catch (OperationCanceledException | InterruptedException ex) {
        logger.log(Level.WARNING, "Exception waiting for WTP Graph Update job", ex);
      }

      org.eclipse.wst.server.core.IRuntime[] appEngineRuntimes = getAppEngineRuntimes();
      if (appEngineRuntimes.length > 0) {
        IRuntime appEngineFacetRuntime = null;
        progress.setWorkRemaining(appEngineRuntimes.length);
        for (org.eclipse.wst.server.core.IRuntime appEngineRuntime : appEngineRuntimes) {
          appEngineFacetRuntime = FacetUtil.getRuntime(appEngineRuntime);
          project.addTargetedRuntime(appEngineFacetRuntime, progress.newChild(1));
        }
        project.setPrimaryRuntime(appEngineFacetRuntime, monitor);
      } else { // Create a new App Engine runtime
        IRuntime appEngineFacetRuntime = createAppEngineFacetRuntime(progress.newChild(10));
        if (appEngineFacetRuntime == null) {
          throw new NullPointerException("Could not locate App Engine facet runtime");
        }

        project.addTargetedRuntime(appEngineFacetRuntime, progress.newChild(10));
        project.setPrimaryRuntime(appEngineFacetRuntime, progress.newChild(10));
      }
    } finally {
      IDependencyGraph.INSTANCE.postUpdate();
    }
  }

  public static org.eclipse.wst.server.core.IRuntime createAppEngineServerRuntime(
      IProgressMonitor monitor) throws CoreException {
    IRuntimeType appEngineRuntimeType =
        ServerCore.findRuntimeType(AppEngineStandardFacet.DEFAULT_RUNTIME_ID);
    if (appEngineRuntimeType == null) {
      throw new NullPointerException(
          "Could not find " + AppEngineStandardFacet.DEFAULT_RUNTIME_NAME + " runtime type");
    }

    IRuntimeWorkingCopy appEngineRuntimeWorkingCopy
        = appEngineRuntimeType.createRuntime(null /* id */, monitor);
    return appEngineRuntimeWorkingCopy.save(true, monitor);
  }

  public static IRuntime createAppEngineFacetRuntime(IProgressMonitor monitor)
      throws CoreException {
    org.eclipse.wst.server.core.IRuntime appEngineServerRuntime =
        createAppEngineServerRuntime(monitor);
    return FacetUtil.getRuntime(appEngineServerRuntime);
  }

  /**
   * Installs Java 1.7 facet if it doesn't already exist in {@code facetedProject}.
   */
  private static void addJavaFacetToBatch(IFacetedProject facetedProject,
      Set<IFacetedProject.Action> facetInstallSet) {
    if (facetedProject.hasProjectFacet(JavaFacet.VERSION_1_7)) {
      return;
    }

    // TODO use "src/main/java" for only maven projects
    JavaFacetInstallConfig javaConfig = new JavaFacetInstallConfig();
    List<IPath> sourcePaths = new ArrayList<>();
    sourcePaths.add(new Path("src/main/java"));
    sourcePaths.add(new Path("src/test/java"));
    javaConfig.setSourceFolders(sourcePaths);
    facetInstallSet.add(new IFacetedProject.Action(
        IFacetedProject.Action.Type.INSTALL, JavaFacet.VERSION_1_7, javaConfig));
  }

  /**
   * Installs Dynamic Web Module 2.5 facet if it doesn't already exist in {@code facetedProject}.
   */
  private static void addWebFacetToBatch(IFacetedProject facetedProject,
      Set<IFacetedProject.Action> facetInstallSet) {
    if (facetedProject.hasProjectFacet(WebFacetUtils.WEB_25)) {
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
        IFacetedProject.Action.Type.INSTALL, WebFacetUtils.WEB_25, webModel));
  }

  private static org.eclipse.wst.server.core.IRuntime[] getAppEngineRuntimes() {
    org.eclipse.wst.server.core.IRuntime[] allRuntimes = ServerCore.getRuntimes();
    List<org.eclipse.wst.server.core.IRuntime> appEngineRuntimes = new ArrayList<>();

    for (int i = 0; i < allRuntimes.length; i++) {
      if (isAppEngineStandardRuntime(allRuntimes[i])) {
        appEngineRuntimes.add(allRuntimes[i]);
      }
    }

    org.eclipse.wst.server.core.IRuntime[] appEngineRuntimesArray =
        new org.eclipse.wst.server.core.IRuntime[appEngineRuntimes.size()];
    return appEngineRuntimes.toArray(appEngineRuntimesArray);
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
