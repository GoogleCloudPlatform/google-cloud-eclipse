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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.xml.sax.SAXException;

/**
 * A set of utility methods for dealing with WTP {@link IModule}s.
 */
public class ModuleUtils {
  private static final Logger logger = Logger.getLogger(ModuleUtils.class.getName());

  /**
   * Retrieve the &lt;service&gt; or &lt;module&gt; identifier from <tt>appengine-web.xml</tt>.
   * If an identifier is not found, return "default".
   *
   * @return the identifier, defaulting to "default" if not found
   */
  public static String getServiceId(IModule module) {
    IFile descriptorFile =
        WebProjectUtil.findInWebInf(module.getProject(), new Path("appengine-web.xml"));
    if (descriptorFile != null) {
      try (InputStream contents = descriptorFile.getContents()) {
        AppEngineDescriptor descriptor = AppEngineDescriptor.parse(contents);
        String serviceId = descriptor.getServiceId();
        if (serviceId != null) {
          return serviceId;
        }
      } catch (SAXException ex) {
        // Parsing failed due to malformed XML; return "default".
      } catch (CoreException | IOException ex) {
        logger.log(Level.WARNING, "Unable to read " + descriptorFile.getFullPath(), ex);
      }
    }

    return "default";
  }

  /**
   * Returns the set of all referenced modules, including child modules. This returns the unique
   * modules, and doesn't return the module paths. Required as neither
   * {@code Server#getAllModules()} nor the module-visiting method {@code #visit()} are exposed on
   * {@link IServer}, and {@code ServerBehaviourDelegate#getAllModules()} is protected.
   */
  public static IModule[] getAllModules(IServer server) {
    Set<IModule> modules = new LinkedHashSet<>();
    for (IModule module : server.getModules()) {
      modules.add(module);
      addChildModules(server, new IModule[] {module}, modules);
    }
    return modules.toArray(new IModule[modules.size()]);
  }

  /** Recursively walk the children from {@code modulePath}. */
  private static void addChildModules(IServer server, IModule[] modulePath, Set<IModule> modules) {
    IModule[] newModulePath = Arrays.copyOf(modulePath, modulePath.length + 1);
    for (IModule child : server.getChildModules(modulePath, null)) {
      if (modules.add(child)) {
        newModulePath[newModulePath.length - 1] = child;
        addChildModules(server, newModulePath, modules);
      }
    }
  }
}
