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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.common.project.facet.core.JavaFacetInstallConfig;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.server.core.FacetUtil;
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

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;
import com.google.common.base.Preconditions;

public class AppEngineStandardFacet {

  public static final String ID = "com.google.cloud.tools.eclipse.appengine.facets.standard";

  static final String VERSION = "1";
  static final String DEFAULT_RUNTIME_ID = "com.google.cloud.tools.eclipse.appengine.standard.runtime";
  static final String DEFAULT_RUNTIME_NAME = "App Engine Standard";
  public static final String DEFAULT_APPENGINE_SDK_VERSION = "1.9.44";
  public static final String DEFAULT_GCLOUD_PLUGIN_VERSION = "2.0.9.130.v20161013";

  /**
   * Returns true if project has the App Engine Standard facet and false otherwise.
   *
   * @param project should not be null
   * @return true if project has the App Engine Standard facet and false otherwise
   */
  public static boolean hasAppEngineFacet(IFacetedProject project) {
    FacetedProjectHelper facetedProjectHelper = new FacetedProjectHelper();
    return facetedProjectHelper.projectHasFacet(project, ID);
  }

  /**
   * Returns true if <code>facetRuntime</code> is an App Engine Standard runtime and false otherwise
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
   * Returns true if <code>serverRuntime</code> is an App Engine Standard runtime and false otherwise
   *
   * @param serverRuntime the server runtime, runtime should not be null
   * @return true if <code>serverRuntime</code> is an App Engine Standard runtime and false otherwise
   */
  public static boolean isAppEngineStandardRuntime(org.eclipse.wst.server.core.IRuntime serverRuntime) {
    Preconditions.checkNotNull(serverRuntime, "runtime is null");
    IRuntimeType runtimeType = serverRuntime.getRuntimeType();
    if (runtimeType == null) {
      return false;
    }
    return DEFAULT_RUNTIME_ID.equals(runtimeType.getId());
  }

  /**
   * Checks to see if <code>facetedProject</code> has the App Engine facet installed. If not, it installs
   * the App Engine facet.
   *
   * @param facetedProject the faceted project receiving the App Engine facet
   * @param installDependentFacets true if the facets required by the App Engine facet should be installed,
   *   false otherwise
   * @param monitor the progress monitor
   * @throws CoreException if anything goes wrong during install
   */
  public static void installAppEngineFacet(IFacetedProject facetedProject, boolean installDependentFacets, IProgressMonitor monitor)
      throws CoreException {
    // Install required App Engine facets i.e. Java 1.7 and Dynamic Web Module 2.5
    if (installDependentFacets) {
      installJavaFacet(facetedProject, monitor);
      installWebFacet(facetedProject, monitor);
    }

    IProjectFacet appEngineFacet = ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID);
    IProjectFacetVersion appEngineFacetVersion = appEngineFacet.getVersion(AppEngineStandardFacet.VERSION);

    if (!facetedProject.hasProjectFacet(appEngineFacet)) {
      facetedProject.installProjectFacet(appEngineFacetVersion, null /* config */, monitor);
    }
  }

  /**
   * If App Engine runtimes exist in the workspace, add them to the list of targeted runtimes
   * of <code>project</code>. Otherwise create a new App Engine runtime and add it to the list
   * of targeted runtimes.
   *
   * @param project the faceted project receiving the App Engine runtime(s)
   * @param force true if all runtime instances should be added to the <code>project</code> even if targeted list of
   *  <code>project</code> already includes App Engine runtime instances and false otherwise
   * @param monitor the progress monitor
   * @throws CoreException if the project contains one or more facets that are not supported by this runtime; if
   *   failed for any other reason
   */
  public static void installAllAppEngineRuntimes(IFacetedProject project, boolean force, IProgressMonitor monitor)
      throws CoreException {
    // If the project already has an App Engine runtime instance and force is false
    // do not add any other App Engine runtime instances to the list of targeted runtimes
    Set<IRuntime> existingTargetedRuntimes = project.getTargetedRuntimes();
    if (!existingTargetedRuntimes.isEmpty()) {
      for (IRuntime existingTargetedRuntime : existingTargetedRuntimes) {
        if (AppEngineStandardFacet.isAppEngineStandardRuntime(existingTargetedRuntime) && !force) {
          return;
        }
      }
    }

    org.eclipse.wst.server.core.IRuntime[] appEngineRuntimes = getAppEngineRuntimes();
    if (appEngineRuntimes.length > 0) {
      IRuntime appEngineFacetRuntime = null;
      for(int index = 0; index < appEngineRuntimes.length; index++) {
        appEngineFacetRuntime = FacetUtil.getRuntime(appEngineRuntimes[index]);
        project.addTargetedRuntime(appEngineFacetRuntime, monitor);
      }
      project.setPrimaryRuntime(appEngineFacetRuntime, monitor);
    } else { // Create a new App Engine runtime
      IRuntime appEngineFacetRuntime = createAppEngineFacetRuntime(monitor);
      if (appEngineFacetRuntime == null) {
        throw new NullPointerException("Could not locate App Engine facet runtime");
      }

      project.addTargetedRuntime(appEngineFacetRuntime, monitor);
      project.setPrimaryRuntime(appEngineFacetRuntime, monitor);
    }
  }

  public static org.eclipse.wst.server.core.IRuntime createAppEngineServerRuntime(IProgressMonitor monitor)
      throws CoreException {
    IRuntimeType appEngineRuntimeType =
        ServerCore.findRuntimeType(AppEngineStandardFacet.DEFAULT_RUNTIME_ID);
    if (appEngineRuntimeType == null) {
      throw new NullPointerException("Could not find " + AppEngineStandardFacet.DEFAULT_RUNTIME_NAME + " runtime type");
    }

    IRuntimeWorkingCopy appEngineRuntimeWorkingCopy
        = appEngineRuntimeType.createRuntime(null /* id */, monitor);

    CloudSdk cloudSdk = new CloudSdk.Builder().build();
    if (cloudSdk != null) {
      java.nio.file.Path sdkLocation = cloudSdk.getJavaAppEngineSdkPath();
      if (sdkLocation != null) {
        IPath sdkPath = Path.fromOSString(sdkLocation.toAbsolutePath().toString());
        appEngineRuntimeWorkingCopy.setLocation(sdkPath);
      }
    }

    return appEngineRuntimeWorkingCopy.save(true, monitor);
  }

  public static IRuntime createAppEngineFacetRuntime(IProgressMonitor monitor)
      throws CoreException {
    org.eclipse.wst.server.core.IRuntime appEngineServerRuntime = createAppEngineServerRuntime(monitor);
    return FacetUtil.getRuntime(appEngineServerRuntime);
  }

  /**
   * Installs Java 1.7 facet if it doesn't already exist in <code>factedProject</code>
   */
  private static void installJavaFacet(IFacetedProject facetedProject, IProgressMonitor monitor)
      throws CoreException {
    if (facetedProject.hasProjectFacet(JavaFacet.VERSION_1_7)) {
      return;
    }

    // TODO use "src/main/java" for only maven projects
    JavaFacetInstallConfig javaConfig = new JavaFacetInstallConfig();
    List<IPath> sourcePaths = new ArrayList<>();
    sourcePaths.add(new Path("src/main/java"));
    sourcePaths.add(new Path("src/test/java"));
    javaConfig.setSourceFolders(sourcePaths);
    facetedProject.installProjectFacet(JavaFacet.VERSION_1_7, javaConfig, monitor);
  }

  /**
   * Installs Dynamic Web Module 2.5 facet if it doesn't already exits in <code>factedProject</code>
   */
  private static void installWebFacet(IFacetedProject facetedProject, IProgressMonitor monitor)
      throws CoreException {
    if (facetedProject.hasProjectFacet(WebFacetUtils.WEB_25)) {
      return;
    }

    IDataModel webModel = DataModelFactory.createDataModel(new WebFacetInstallDataModelProvider());
    webModel.setBooleanProperty(IJ2EEModuleFacetInstallDataModelProperties.ADD_TO_EAR, false);
    webModel.setBooleanProperty(IJ2EEFacetInstallDataModelProperties.GENERATE_DD, false);
    webModel.setBooleanProperty(IWebFacetInstallDataModelProperties.INSTALL_WEB_LIBRARY, false);
    webModel.setStringProperty(IWebFacetInstallDataModelProperties.CONFIG_FOLDER, "src/main/webapp");
    facetedProject.installProjectFacet(WebFacetUtils.WEB_25, webModel, monitor);
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

}
