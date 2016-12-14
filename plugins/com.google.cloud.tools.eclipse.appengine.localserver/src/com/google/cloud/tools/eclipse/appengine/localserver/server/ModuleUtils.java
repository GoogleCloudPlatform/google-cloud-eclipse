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

import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.util.AppEngineDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;

/**
 * A set of utility methods for dealing with WTP {@link IModule}s.
 */
public class ModuleUtils {
  private static final Logger logger = Logger.getLogger(ModuleUtils.class.getName());

  /**
   * Retrieve the &lt;service&gt; or &lt;module&gt; identifier from <tt>appengine-web.xml</tt>.
   * If an identifier is not found, then returns "default".
   * 
   * @return the identifier, defaulting to "default" if not found
   */
  public static String getServiceId(IModule module) {
    IFile descriptorFile =
        WebProjectUtil.findInWebInf(module.getProject(), new Path("appengine-web.xml"));
    if (descriptorFile == null) {
      return "default";
    }
    
    String serviceId = null;
    try (InputStream contents = descriptorFile.getContents()) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(contents);
      serviceId = descriptor.getServiceId();
    } catch (CoreException | IOException ex) {
      logger.log(Level.WARNING, "Unable to read " + descriptorFile.getFullPath(), ex);
    }
    return serviceId != null ? serviceId : "default";
  }
}
