/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *     Google Inc - Generalized to a base implementation
 *******************************************************************************/

package com.google.cloud.tools.eclipse.jst.server.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

/**
 * Publish operation suitable for standard web app servers.
 */
public abstract class BasePublishOperation extends PublishOperation {
  private static final String PLUGIN_ID = "com.google.cloud.tools.eclipse.jst.server.core";

  protected IModule[] module;
  protected int kind;
  protected int deltaKind;
  protected PublishHelper helper;

  /**
   * Construct the operation object to publish the specified module to the specified server.
   * 
   * @param server server to which the module will be published
   * @param kind kind of publish
   * @param module module to publish
   * @param deltaKind kind of change
   */
  public BasePublishOperation(String label, String description, int kind, IModule[] module,
      int deltaKind) {
    super(label, description);
    this.module = module;
    this.kind = kind;
    this.deltaKind = deltaKind;
  }

  /**
   * Return the base directory used where modules are deployed. 
   */
  protected abstract IPath getRuntimeBaseDirectory();

  /**
   * Return the location to where this server expects to find the given module. This location is
   * typically under {@link #getRuntimeBaseDirectory()}.
   */
  protected abstract IPath getModuleDeployDirectory(IModule module);


  /**
   * Update the server's notion of the module's publish state
   * 
   * @see IServer#PUBLISH_STATE_NONE
   * @see IServer#PUBLISH_STATE_UNKNOWN
   * @see IServer#PUBLISH_STATE_INCREMENTAL
   * @see IServer#PUBLISH_STATE_FULL
   */
  protected abstract void setModulePublishState(IModule[] module, int publishState);

  /**
   * Determine the changed resources since last published.
   */
  protected abstract IModuleResourceDelta[] getPublishedResourceDelta(IModule[] module);

  /**
   * Return all the resources to be published for the given module.
   */
  protected abstract IModuleResource[] getResources(IModule[] module);

  /**
   * Return {@code true} if this server can serve modules without explicitly publishing.
   */
  protected abstract boolean isServeModulesWithoutPublish();

  public int getOrder() {
    return 0;
  }

  public int getKind() {
    return REQUIRED;
  }

  /**
   * Initializes this instance.
   */
  protected void initialize() throws CoreException {
    IPath base = getRuntimeBaseDirectory();
    if (base != null) {
      helper = new PublishHelper(base.append("temp").toFile());
    } else {
      // We are doomed without a base directory. However, allow the catastrophe
      // to occur elsewhere and hope for a useful error message.
      helper = new PublishHelper(null);
    }
  }


  public void execute(IProgressMonitor monitor, IAdaptable info) throws CoreException {
    initialize();

    List<IStatus> status = new ArrayList<IStatus>();
    // If parent web module
    if (module.length == 1) {
      publishDir(module[0], status, monitor);
    } else {
      // Else a child module
      Properties p = loadModulePublishLocations();

      // Try to determine the URI for the child module
      IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, monitor);
      String childURI = null;
      if (webModule != null) {
        childURI = webModule.getURI(module[1]);
      }
      // Try to determine if child is binary
      IJ2EEModule childModule = (IJ2EEModule) module[1].loadAdapter(IJ2EEModule.class, monitor);
      boolean isBinary = false;
      if (childModule != null) {
        isBinary = childModule.isBinary();
      }

      if (isBinary) {
        publishArchiveModule(childURI, p, status, monitor);
      } else {
        publishJar(childURI, p, status, monitor);
      }
      saveModulePublishLocations(p);
    }
    throwException(status);
    setModulePublishState(module, IServer.PUBLISH_STATE_NONE);
  }

  private void publishDir(IModule module2, List<IStatus> status, IProgressMonitor monitor)
      throws CoreException {
    IPath path = getModuleDeployDirectory(module2);

    // Remove if requested or if previously published and are now serving without publishing
    if (kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED
        || isServeModulesWithoutPublish()) {
      File f = path.toFile();
      if (f.exists()) {
        IStatus[] stat = PublishHelper.deleteDirectory(f, monitor);
        addArrayToList(status, stat);
      }

      if (deltaKind == ServerBehaviourDelegate.REMOVED
          || isServeModulesWithoutPublish())
        return;
    }

    if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL) {
      IModuleResource[] mr = getResources(module);
      IStatus[] stat = helper.publishFull(mr, path, monitor);
      addArrayToList(status, stat);
      return;
    }

    IModuleResourceDelta[] delta = getPublishedResourceDelta(module);

    int size = delta.length;
    for (int i = 0; i < size; i++) {
      IStatus[] stat = helper.publishDelta(delta[i], path, monitor);
      addArrayToList(status, stat);
    }
  }

  protected void publishJar(String jarURI, Properties p, List<IStatus> status,
      IProgressMonitor monitor) throws CoreException {
    IPath path = getModuleDeployDirectory(module[0]);
    boolean moving = false;
    // Get URI used for previous publish, if known
    String oldURI = (String) p.get(module[1].getId());
    if (oldURI != null) {
      // If old URI found, detect if jar is moving or changing its name
      if (jarURI != null) {
        moving = !oldURI.equals(jarURI);
      }
    }
    // If we don't have a jar URI, make a guess so we have one if we need it
    if (jarURI == null) {
      jarURI = "WEB-INF/lib/" + module[1].getName() + ".jar";
    }
    IPath jarPath = path.append(jarURI);
    // Make our best determination of the path to the old jar
    IPath oldJarPath = jarPath;
    if (oldURI != null) {
      oldJarPath = path.append(oldURI);
    }
    // Establish the destination directory
    path = jarPath.removeLastSegments(1);

    // Remove if requested or if previously published and are now serving without publishing
    if (moving || kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED
        || isServeModulesWithoutPublish()) {
      File file = oldJarPath.toFile();
      if (file.exists())
        file.delete();
      p.remove(module[1].getId());

      if (deltaKind == ServerBehaviourDelegate.REMOVED
          || isServeModulesWithoutPublish())
        return;
    }
    if (!moving && kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
      // avoid changes if no changes to module since last publish
      IModuleResourceDelta[] delta = getPublishedResourceDelta(module);
      if (delta == null || delta.length == 0)
        return;
    }

    // make directory if it doesn't exist
    if (!path.toFile().exists())
      path.toFile().mkdirs();

    IModuleResource[] mr = getResources(module);
    IStatus[] stat = helper.publishZip(mr, jarPath, monitor);
    addArrayToList(status, stat);
    p.put(module[1].getId(), jarURI);
  }

  private void publishArchiveModule(String jarURI, Properties p, List<IStatus> status,
      IProgressMonitor monitor) {
    IPath path = getModuleDeployDirectory(module[0]);
    boolean moving = false;
    // Get URI used for previous publish, if known
    String oldURI = (String) p.get(module[1].getId());
    if (oldURI != null) {
      // If old URI found, detect if jar is moving or changing its name
      if (jarURI != null) {
        moving = !oldURI.equals(jarURI);
      }
    }
    // If we don't have a jar URI, make a guess so we have one if we need it
    if (jarURI == null) {
      jarURI = "WEB-INF/lib/" + module[1].getName();
    }
    IPath jarPath = path.append(jarURI);
    // Make our best determination of the path to the old jar
    IPath oldJarPath = jarPath;
    if (oldURI != null) {
      oldJarPath = path.append(oldURI);
    }
    // Establish the destination directory
    path = jarPath.removeLastSegments(1);

    // Remove if requested or if previously published and are now serving without publishing
    if (moving || kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED
        || isServeModulesWithoutPublish()) {
      File file = oldJarPath.toFile();
      if (file.exists()) {
        file.delete();
      }
      p.remove(module[1].getId());

      if (deltaKind == ServerBehaviourDelegate.REMOVED
          || isServeModulesWithoutPublish())
        return;
    }
    if (!moving && kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
      // avoid changes if no changes to module since last publish
      IModuleResourceDelta[] delta = getPublishedResourceDelta(module);
      if (delta == null || delta.length == 0)
        return;
    }

    // make directory if it doesn't exist
    if (!path.toFile().exists())
      path.toFile().mkdirs();

    IModuleResource[] mr = getResources(module);
    IStatus[] stat = helper.publishToPath(mr, jarPath, monitor);
    addArrayToList(status, stat);
    p.put(module[1].getId(), jarURI);
  }

  /**
   * Save the locations of the configured modules.
   */
  protected void saveModulePublishLocations(Properties p) {
    IPath path = getRuntimeBaseDirectory().append("publish.txt");
    try (FileOutputStream fout = new FileOutputStream(path.toFile())) {
      p.store(fout, "Module publish data");
    } catch (Exception e) {
      // ignore
    }
  }

  /**
   * Obtain the locations of the configured modules.
   */
  protected Properties loadModulePublishLocations() {
    Properties p = new Properties();
    IPath path = getRuntimeBaseDirectory().append("publish.txt");
    try (FileInputStream fin = new FileInputStream(path.toFile())) {
      p.load(fin);
    } catch (Exception e) {
      // ignore
    }
    return p;
  }


  /**
   * Utility method to throw a CoreException based on the contents of a list of error and warning
   * status.
   * 
   * @param status a List containing error and warning IStatus
   * @throws CoreException
   */
  protected static void throwException(List<IStatus> status) throws CoreException {
    if (status == null || status.size() == 0)
      return;

    if (status.size() == 1) {
      IStatus status2 = status.get(0);
      throw new CoreException(status2);
    }
    IStatus[] children = new IStatus[status.size()];
    status.toArray(children);
    String message = "Publishing failed with multiple errors"; // Messages.errorPublish;
    MultiStatus status2 = new MultiStatus(PLUGIN_ID, 0, children, message, null);
    throw new CoreException(status2);
  }

  protected static void addArrayToList(List<IStatus> list, IStatus[] a) {
    if (list == null || a == null || a.length == 0)
      return;

    int size = a.length;
    for (int i = 0; i < size; i++)
      list.add(a[i]);
  }
}
