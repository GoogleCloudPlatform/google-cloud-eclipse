/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.localserver.server;

import com.google.cloud.tools.eclipse.appengine.localserver.Activator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jst.server.core.IEnterpriseApplication;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.facets.FacetUtil;
import org.eclipse.wst.server.core.model.ServerDelegate;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ServerDelegate} for Google Cloud SDK.
 */
@SuppressWarnings("restriction") // For FacetUtil
// TODO for https://github.com/GoogleCloudPlatform/gcloud-eclipse-tools/issues/242
// replace this class with one that relies on app-tools-lib-for-java.
// It will need most of the same operations, and probably require some
// additions and modifications to app-tools-lib-for-java.
public class CloudSdkServer extends ServerDelegate {

  private static final String SERVLET_MODULE_FACET = "jst.web";
  private static final String EAR_MODULE_FACET = "jst.ear";

  // must match the value used in plugin.xml in org.eclipse.wst.server.core.serverTypes
  public static final String SERVER_TYPE_ID = "com.google.cloud.tools.eclipse.server.id";
  public static final String SERVER_PROGRAM_FLAGS = Activator.PLUGIN_ID + ".SERVER_FLAGS";
  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_API_PORT = 8188;
  public static final int DEFAULT_ADMIN_PORT = 8000;

  private static final String ATTR_CLOUD_SDK_SERVER_MODULES = "cloudsdk-server-modules-list";

  private ILaunchConfigurationWorkingCopy remoteDebugLaunchConfig;

  /**
   * Returns a {@link CloudSdkServer} instance associated with the
   * {@code server} or a new {@link CloudSdkServer} instance if a
   * {@link CloudSdkServer} instance cannot be found for {@code server}.
   *
   * @param server the cloud SDK server
   * @return a new {@link CloudSdkServer} instance or the one associated with
   *         {@code server}
   */
  public static CloudSdkServer getCloudSdkServer(IServer server) {
    CloudSdkServer cloudSdkServer = server.getAdapter(CloudSdkServer.class);
    if (cloudSdkServer == null) {
      cloudSdkServer = (CloudSdkServer) server.loadAdapter(CloudSdkServer.class, null);
    }
    return cloudSdkServer;
  }

  @Override
  public IStatus canModifyModules(IModule[] add, IModule[] remove) {
    if (add != null) {
      for (IModule module : add) {
        if (module.getProject() != null) {
          IStatus status = FacetUtil.verifyFacets(module.getProject(), getServer());
          if (status != null && !status.isOK()) {
            return status;
          }
        }
      }
    }
    return Status.OK_STATUS;
  }

  @Override
  public IModule[] getChildModules(IModule[] module) {
    if (module[0] != null && module[0].getModuleType() != null) {
      IModule thisModule = module[module.length - 1];
      IModuleType moduleType = thisModule.getModuleType();
      if (moduleType != null && EAR_MODULE_FACET.equals(moduleType.getId())) {
        IEnterpriseApplication enterpriseApplication = (IEnterpriseApplication) thisModule.loadAdapter(IEnterpriseApplication.class,
                                                                                                       null);
        if (enterpriseApplication != null) {
          IModule[] earModules = enterpriseApplication.getModules();
          if (earModules != null) {
            return earModules;
          }
        }
      } else if (moduleType != null && SERVLET_MODULE_FACET.equals(moduleType.getId())) { //$NON-NLS-1$
        IWebModule webModule = (IWebModule) thisModule.loadAdapter(IWebModule.class, null);
        if (webModule != null) {
          IModule[] modules = webModule.getModules();
          return modules;
        }
      }
    }
    return new IModule[0];
  }

  @Override
  public IModule[] getRootModules(IModule module) throws CoreException {
    IStatus status = canModifyModules(new IModule[] { module }, null);
    if (status != null && !status.isOK()) {
      throw new CoreException(status);
    }
    IModule[] parents = doGetParentModules(module);
    if (parents.length > 0) {
      return parents;
    }
    return new IModule[] { module };
  }

  @SuppressWarnings("unchecked")
  @Override
  public void modifyModules(IModule[] add,
                            IModule[] remove,
                            IProgressMonitor monitor) throws CoreException {
    List<String> modules = this.getAttribute(ATTR_CLOUD_SDK_SERVER_MODULES, (List<String>) null);

    if (add != null && add.length > 0) {
      // TODO: ensure modules have same Project ID
      // throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
      // "This server instance cannot run more than one application", null));
      if (modules == null) {
        modules = new ArrayList<>();
      }
      for (int i = 0; i < add.length; i++) {
        if (!modules.contains(add[i].getId())) {
          modules.add(add[i].getId());
        }
      }
    }

    if (remove != null && remove.length > 0 && modules != null) {
      for (int i = 0; i < remove.length; i++) {
        modules.remove(remove[i].getId());
      }
      // schedule server stop as Cloud SDK server cannot run without modules.
      if (modules.isEmpty()) {
        getServer().stop(true);
      }
    }
    if (modules != null) {
      setAttribute(ATTR_CLOUD_SDK_SERVER_MODULES, modules);
    }
  }

  /**
   * @return the default host on which to start the API server
   */
  public String getApiHost() {
    return DEFAULT_HOST;
  }

  /**
   * @return the default host on which to start the admin server
   */
  public String getHostName() {
    return DEFAULT_HOST;
  }

  /**
   * @return the default port on which to start the API server
   */
  public int getApiPort() {
    return DEFAULT_API_PORT;
  }

  /**
   * @return the default port on which to start the admin server
   */
  public int getAdminPort() {
    return DEFAULT_ADMIN_PORT;
  }

  public void setRemoteDebugLaunchConfig(ILaunchConfigurationWorkingCopy wc) {
    remoteDebugLaunchConfig = wc;
  }

  public ILaunchConfigurationWorkingCopy getRemoteDebugLaunchConfig() {
    return remoteDebugLaunchConfig;
  }

  private IModule[] doGetParentModules(IModule module) {
    IModule[] earModules = ServerUtil.getModules(EAR_MODULE_FACET);
    ArrayList<IModule> list = new ArrayList<>();
    for (IModule earModule : earModules) {
      IEnterpriseApplication earApp = (IEnterpriseApplication) earModule.loadAdapter(IEnterpriseApplication.class,
                                                                                     null);
      for (IModule childModule : earApp.getModules()) {
        if (childModule.equals(module)) {
          list.add(earModule);
        }
      }
    }
    return list.toArray(new IModule[list.size()]);
  }
}
