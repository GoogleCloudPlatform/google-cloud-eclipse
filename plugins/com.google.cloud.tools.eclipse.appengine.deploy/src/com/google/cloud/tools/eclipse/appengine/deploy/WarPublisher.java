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

package com.google.cloud.tools.eclipse.appengine.deploy;

import com.google.common.base.Preconditions;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jst.server.core.IWebFragmentModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ModuleFile;
import org.eclipse.wst.server.core.util.PublishHelper;

/**
 * Writes a WAR file of a project, or the exploded contents of it to a destination directory.
 */
public class WarPublisher {

  public static final Logger logger = Logger.getLogger(WarPublisher.class.getName());

  /**
   * It does a smart export, i.e. considers the resources to be copied and if the destination
   * directory already contains resources those will be deleted if they are not part of the exploded
   * WAR.
   */
  public static void publishExploded(IProject project, IPath destination, IProgressMonitor monitor)
      throws CoreException {
    publish(project, destination, true /* exploded */, monitor);
  }

  public static void publishWar(IProject project, IPath destination, IProgressMonitor monitor)
      throws CoreException {
    publish(project, destination, false /* exploded */, monitor);
  }

  private static void publish(IProject project, IPath destination, boolean exploded,
      IProgressMonitor monitor) throws CoreException {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
    Preconditions.checkNotNull(project, "project is null"); //$NON-NLS-1$
    Preconditions.checkNotNull(destination, "destination is null"); //$NON-NLS-1$
    Preconditions.checkArgument(!destination.isEmpty(), "destination is empty path"); //$NON-NLS-1$

    SubMonitor progress = SubMonitor.convert(monitor, 100);
    progress.setTaskName(Messages.getString("task.name.publish.war"));

    PublishHelper publishHelper = new PublishHelper(null);
    IModuleResource[] resources = flattenResources(project, progress);

    if (exploded) {
      publishHelper.publishFull(resources, destination, progress.newChild(100));
    } else {
      publishHelper.publishZip(resources, destination, progress.newChild(100));
    }
  }

  private static IModuleResource[] flattenResources(
      IProject project, IProgressMonitor monitor) throws CoreException {
    List<IModuleResource> resources = new ArrayList<>();

    IModule[] modules = ServerUtil.getModules(project);
    for (IModule module : modules) {
      ModuleDelegate delegate = (ModuleDelegate) module.loadAdapter(ModuleDelegate.class, monitor);
      if (delegate == null) {
        continue;
      }

      // module references can either be as members or child modules (http://eclip.se/467759)
      Collections.addAll(resources, delegate.members());

      // now handle web fragment child modules, if they exist
      for (IModule child : delegate.getChildModules()) {
        ModuleDelegate childDelegate = (ModuleDelegate)
            child.loadAdapter(ModuleDelegate.class, monitor);
        IWebFragmentModule webFragmentModule = (IWebFragmentModule)
            child.loadAdapter(IWebFragmentModule.class, monitor);
        if (childDelegate == null || webFragmentModule == null || !webFragmentModule.isBinary()) {
          logger.log(Level.WARNING, "child modules other than binary web-fragments are not "
              + "supported: module=" + module + ", moduleType=" + module.getModuleType());
          continue;
        }

        // per "isBinary()" Javadoc, "members()" should have a single resource.
        IModuleResource zipResource = childDelegate.members()[0];
        // destination (not an actual zip), e.g., "WEB-INF/lib/spring-web-4.3.6.RELEASE.jar"
        IPath zip = new Path(delegate.getPath(child));
        IPath zipParent = zip.removeLastSegments(1);

        File javaIoFile = zipResource.getAdapter(File.class);
        IFile iFile = zipResource.getAdapter(IFile.class);

        if (javaIoFile != null) {
          resources.add(new ModuleFile(javaIoFile, zipResource.getName(), zipParent));
        } else if (iFile != null) {
          resources.add(new ModuleFile(iFile, zipResource.getName(), zipParent));
        }
      }
    }
    return resources.toArray(new IModuleResource[0]);
  }
}
