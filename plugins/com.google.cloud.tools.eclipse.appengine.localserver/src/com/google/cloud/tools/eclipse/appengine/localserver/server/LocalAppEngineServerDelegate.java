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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.facets.FacetUtil;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;

@SuppressWarnings("restriction") // For FacetUtil
public class LocalAppEngineServerDelegate extends ServerDelegate implements IURLProvider {
  private static final Logger logger =
      Logger.getLogger(LocalAppEngineServerDelegate.class.getName());
  public static final String RUNTIME_TYPE_ID =
      "com.google.cloud.tools.eclipse.appengine.standard.runtime"; //$NON-NLS-1$
  public static final String SERVER_TYPE_ID =
      "com.google.cloud.tools.eclipse.appengine.standard.server"; //$NON-NLS-1$

  private static final IModule[] EMPTY_MODULES = new IModule[0];
  private static final String SERVLET_MODULE_FACET = "jst.web"; //$NON-NLS-1$
  private static final String ATTR_APP_ENGINE_SERVER_MODULES = "app-engine-server-modules-list"; //$NON-NLS-1$

  /**
   * Returns a {@link LocalAppEngineServerDelegate} instance associated with the
   * {@code server} or a new {@link LocalAppEngineServerDelegate} instance if a
   * {@link LocalAppEngineServerDelegate} instance cannot be found for {@code server}.
   *
   * @param server the App Engine server
   * @return a new {@link LocalAppEngineServerDelegate} instance or the one associated with
   *         {@code server}
   */
  public static LocalAppEngineServerDelegate getAppEngineServer(IServer server) {
    LocalAppEngineServerDelegate serverDelegate =
        server.getAdapter(LocalAppEngineServerDelegate.class);
    if (serverDelegate == null) {
      serverDelegate = (LocalAppEngineServerDelegate) server
          .loadAdapter(LocalAppEngineServerDelegate.class, null);
    }
    return serverDelegate;
  }

  /**
   * Returns OK status if the projects associated with modules to be added support the App Engine
   * runtime, otherwise returns an ERROR status.
   */
  @Override
  public IStatus canModifyModules(IModule[] add, IModule[] remove) {
    IStatus result = checkProjectFacets(add);
    if (!result.isOK()) {
      return result;
    }
    return checkConflictingServiceIds(getServer().getModules(), add, remove);
  }

  /**
   * Check that the associated projects support the App Engine runtime.
   */
  @VisibleForTesting
  IStatus checkProjectFacets(IModule[] toBeAdded) {
    if (toBeAdded != null) {
      for (IModule module : toBeAdded) {
        if (module.getProject() != null) {
          IStatus supportedFacets = FacetUtil.verifyFacets(module.getProject(), getServer());
          if (supportedFacets != null && !supportedFacets.isOK()) {
            return supportedFacets;
          }
          IStatus appEngineStandardFacetPresent = hasAppEngineStandardFacet(module);
          if (appEngineStandardFacetPresent != null && !appEngineStandardFacetPresent.isOK()) {
            return appEngineStandardFacetPresent;
          }
        }
      }
    }
    return Status.OK_STATUS;
  }

  /**
   * Check that the associated projects have unique App Engine Service IDs.
   */
  @VisibleForTesting
  IStatus checkConflictingServiceIds(IModule[] current, IModule[] toBeAdded,
      IModule[] toBeRemoved) {

    // verify that we do not have conflicting modules by service id
    Map<String, IModule> currentServiceIds = new HashMap<>();
    for (IModule module : current) {
      String moduleServiceId = getServiceId(module);
      if (currentServiceIds.containsKey(moduleServiceId)) {
        // should never happen: we have a conflict within the already-defined modules
        return StatusUtil.error(this, Messages.getString("SERVICE_CONFLICTS", //$NON-NLS-1$
            module.getName(), currentServiceIds.get(moduleServiceId).getName(), moduleServiceId));
      }
      currentServiceIds.put(moduleServiceId, module);
    }
    if (toBeRemoved != null) {
      for (IModule module : toBeRemoved) {
        String moduleServiceId = getServiceId(module);
        // could verify that: serviceIds.containsKey(moduleServiceId)
        currentServiceIds.remove(moduleServiceId);
      }
    }
    if (toBeAdded != null) {
      for (IModule module : toBeAdded) {
        if (currentServiceIds.containsValue(module)) {
          // skip modules that are already present
          continue;
        }
        String moduleServiceId = getServiceId(module);
        if (currentServiceIds.containsKey(moduleServiceId)) {
          return StatusUtil.error(this, Messages.getString("SERVICE_CONFLICTS", //$NON-NLS-1$
              module.getName(), currentServiceIds.get(moduleServiceId).getName(), moduleServiceId));
        }
        currentServiceIds.put(moduleServiceId, module);
      }
    }
    return Status.OK_STATUS;
  }

  @VisibleForTesting
  Function<IModule, String> serviceIdFunction;

  private String getServiceId(IModule module) {
    if (serviceIdFunction != null) {
      return serviceIdFunction.apply(module);
    }
    return ModuleUtils.getServiceId(module);
  }

  private static IStatus hasAppEngineStandardFacet(IModule module) {
    try {
      if (AppEngineStandardFacet.hasFacet(ProjectFacetsManager.create(module.getProject()))) {
        return Status.OK_STATUS;
      } else {
        String errorMessage = Messages.getString("GAE_STANDARD_FACET_MISSING", module.getName(), //$NON-NLS-1$
            module.getProject().getName());
        return StatusUtil.error(LocalAppEngineServerDelegate.class, errorMessage);
      }
    } catch (CoreException ex) {
      return StatusUtil.error(LocalAppEngineServerDelegate.class,
          Messages.getString("NOT_FACETED_PROJECT", module.getProject().getName()), ex); //$NON-NLS-1$
    }
  }

  /**
   * If the module is a web module returns the utility modules contained within its WAR, otherwise
   * returns an empty list.
   *
   * @param module the module path traversed to this point
   */
  @Override
  public IModule[] getChildModules(IModule[] module) {
    if (module == null || module.length == 0) {
      return EMPTY_MODULES;
    }
    IModule thisModule = module[module.length - 1];
    if (thisModule != null && thisModule.getModuleType() != null) {
      IModuleType moduleType = thisModule.getModuleType();
      if (moduleType != null && SERVLET_MODULE_FACET.equals(moduleType.getId())) {
        IWebModule webModule = (IWebModule) thisModule.loadAdapter(IWebModule.class, null);
        if (webModule != null) {
          IModule[] modules = webModule.getModules();
          return modules;
        }
      }
    }
    return EMPTY_MODULES;
  }

  @Override
  public IModule[] getRootModules(IModule module) throws CoreException {
    return new IModule[] { module };
  }

  @SuppressWarnings("unchecked")
  @Override
  public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor)
      throws CoreException {
    List<String> modules = this.getAttribute(ATTR_APP_ENGINE_SERVER_MODULES, (List<String>) null);

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
      // schedule server stop as App Engine server cannot run without modules.
      if (modules.isEmpty()) {
        getServer().stop(true);
      }
    }
    if (modules != null) {
      setAttribute(ATTR_APP_ENGINE_SERVER_MODULES, modules);
    }
  }

  @Override
  public URL getModuleRootURL(IModule module) {
    // use getAdapter() to avoid unnecessarily loading the class (e.g., not started yet)
    LocalAppEngineServerBehaviour serverBehaviour =
        (LocalAppEngineServerBehaviour) getServer().getAdapter(LocalAppEngineServerBehaviour.class);
    if (serverBehaviour == null) {
      return null;
    }
    try {
      String url;
      if (module == null) {
        url = "http://" + getServer().getHost() + ":" + serverBehaviour.getAdminPort(); //$NON-NLS-1$ //$NON-NLS-2$
      } else {
        String serviceId = getServiceId(module); // never null
        url = serverBehaviour.getServiceUrl(serviceId);
      }
      return new URL(url);
    } catch (MalformedURLException ex) {
      logger.log(Level.WARNING, "Generated invalid URL", ex); //$NON-NLS-1$
      return null;
    }
  }
}
