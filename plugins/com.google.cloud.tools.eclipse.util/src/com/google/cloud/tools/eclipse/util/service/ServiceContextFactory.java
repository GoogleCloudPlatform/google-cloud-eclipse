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

package com.google.cloud.tools.eclipse.util.service;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;

public class ServiceContextFactory implements IExecutableExtensionFactory, IExecutableExtension {

  private Class<?> clazz;

  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
      throws CoreException {
      if (data == null || !(data instanceof String)) {
        throw new CoreException(StatusUtil.error(getClass(), "Data must be a class name"));
      }
      String className = (String) data;
      String bundleSymbolicName = config.getNamespaceIdentifier();
      Bundle bundle = Platform.getBundle(bundleSymbolicName);
      if (bundle == null) {
        throw new CoreException(StatusUtil.error(this, "Missing bundle " + bundleSymbolicName));
      }
      try {
        clazz = bundle.loadClass(className);
      } catch (ClassNotFoundException ex) {
        throw new CoreException(StatusUtil.error(this,
                                                 "Could not load class " + className
                                                 + " from bundle " + bundle.getSymbolicName(),
                                                 ex));
      }
  }

  @Override
  public Object create() throws CoreException {
    BundleContext bundleContext = FrameworkUtil.getBundle(clazz).getBundleContext();
    IEclipseContext serviceContext = EclipseContextFactory.getServiceContext(bundleContext);
    return ContextInjectionFactory.make(clazz, serviceContext);
  }
}
