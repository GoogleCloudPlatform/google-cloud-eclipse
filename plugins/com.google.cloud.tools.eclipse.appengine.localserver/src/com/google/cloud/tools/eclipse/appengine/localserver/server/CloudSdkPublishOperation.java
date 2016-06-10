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

import com.google.common.collect.Lists;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class CloudSdkPublishOperation extends PublishOperation {
  private static final String PLUGIN_ID = CloudSdkPublishOperation.class.getName();

  /**
   * Throws new {@link CoreException} if status list is not empty.
   */
  private static void checkStatuses(List<IStatus> statusList) throws CoreException {
    if (statusList == null || statusList.size() == 0) {
      return;
    }
    if (statusList.size() == 1) {
      throw new CoreException(statusList.get(0));
    }
    IStatus[] children = statusList.toArray(new IStatus[statusList.size()]);
    throw new CoreException(
        new MultiStatus(PLUGIN_ID, 0, children,
        "Error during publish operation", null));
  }

  private CloudSdkServerBehaviour server;
  private IModule[] modules;
  private int kind;
  private int deltaKind;
  private PublishHelper helper;

  public int getKind() {
    return REQUIRED;
  }

  @Override
  public int getOrder() {
    // TODO: should publishing tasks come later?
    // may be necessary to work with GWT compilation
    return 0;
  }

  /**
   * Construct the operation object to publish the specified modules(s) to the specified server.
   */
  public CloudSdkPublishOperation(CloudSdkServerBehaviour server, int kind, IModule[] modules,
      int deltaKind) {
    super("Publish to server", "Publish modules to App Engine Development Server");
    this.server = server;
    this.modules = modules;
    this.kind = kind;
    this.deltaKind = deltaKind;
    IPath base = server.getRuntimeBaseDirectory();
    helper = new PublishHelper(base.toFile());
  }

  @Override
  public void execute(IProgressMonitor monitor, IAdaptable info) throws CoreException {
    // TODO: use more advanced key to store modules publish locations? Because a dependent
    // java project (added as child modules and published as jar) cannot present in more than one
    // parent modules.
    // (Is this TODO still valid?)
    List<IStatus> statusList = Lists.newArrayList();
    IPath deployPath = server.getModuleDeployDirectory(modules[0]);
    if (modules.length == 1) {
      // root modules
      publishDirectory(deployPath, statusList, monitor);
    } else {
      // TODO: Why and when does this code happen?
      for (int i = 0; i < modules.length - 1; i++) {
        IWebModule webModule = (IWebModule) modules[i].loadAdapter(IWebModule.class, monitor);
        if (webModule == null) {
          statusList.add(newErrorStatus("Not a Web module: " + modules[i].getName()));
          return;
        }
        String uri = webModule.getURI(modules[i + 1]);
        if (uri != null) {
          deployPath = deployPath.append(uri);
        } else {
          // no uri is OK for removed modules
          if (deltaKind != ServerBehaviourDelegate.REMOVED) {
            statusList
                .add(newErrorStatus("Cannot get URI for module: " + modules[i + 1].getName()));
            return;
          }
        }
      }
      // TODO: Disabled saving and loading module locations below (marked with XXX)

      // modules given as parent-child chain
      // get last one, the prior modules should already be published
      IModule childModule = modules[modules.length - 1];
      // XXX: Properties moduleUrls = server.loadModulePublishLocations();
      Properties moduleUrls = new Properties(); // XXX: = server.loadModulePublishLocations();
      // get as j2ee
      IJ2EEModule childJ2EEModule =
          (IJ2EEModule) childModule.loadAdapter(IJ2EEModule.class, monitor);
      if (childJ2EEModule != null && childJ2EEModule.isBinary()) {
        publishArchiveModule(deployPath, moduleUrls, statusList, monitor, childModule);
      } else {
          publishDir(deployPath, moduleUrls, statusList, monitor, childModule);
      }
      // XXX: server.saveModulePublishLocations(moduleUrls);
    }
    checkStatuses(statusList);
    server.setModulePublishState2(modules, IServer.PUBLISH_STATE_NONE);
  }

  private IStatus newErrorStatus(String message) {
    return new Status(IStatus.ERROR, getClass().getName(), message);
  }

  /**
   * Publish as binary modules.
   */
  private void publishArchiveModule(IPath path, Properties mapping, List<IStatus> statusList,
      IProgressMonitor monitor, IModule childModule) {
    boolean isMoving = false;
    // check older publish
    String oldUri = (String) mapping.get(childModule.getId());
    String jarUri = path.toOSString();
    if (oldUri != null && jarUri != null) {
      isMoving = !oldUri.equals(jarUri);
    }
    // setup target
    IPath jarPath = (IPath) path.clone();
    IPath deployPath = jarPath.removeLastSegments(1);
    // remove if requested or if previously published and are now serving without publishing
    if (isMoving || kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      if (oldUri != null) {
        File file = new File(oldUri);
        if (file.exists()) {
          file.delete();
        }
      }
      mapping.remove(childModule.getId());
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // check for changes
    if (!isMoving && kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
      IModuleResourceDelta[] delta = server.getPublishedResourceDelta(modules);
      if (delta == null || delta.length == 0) {
        return;
      }
    }
    // ensure target directory
    if (!deployPath.toFile().exists()) {
      deployPath.toFile().mkdirs();
    }
    // do publish
    IModuleResource[] resources = server.getResources(modules);
    IStatus[] publishStatus = helper.publishToPath(resources, jarPath, monitor);
    statusList.addAll(Arrays.asList(publishStatus));
    // store into mapping
    mapping.put(childModule.getId(), jarUri);
  }

  /**
   * Publish modules as directory.
   */
  private void publishDirectory(IPath path, List<IStatus> statusList, IProgressMonitor monitor)
      throws CoreException {
    // delete if needed
    if (kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      File file = path.toFile();
      if (file.exists()) {
        IStatus[] status = PublishHelper.deleteDirectory(file, monitor);
        statusList.addAll(Arrays.asList(status));
      }
      // request for remove
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // republish or publish fully
    if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL) {
      IModuleResource[] resources = server.getResources(modules);
      IStatus[] publishStatus = helper.publishFull(resources, path, monitor);
      statusList.addAll(Arrays.asList(publishStatus));
      return;
    }
    // publish changes only
    IModuleResourceDelta[] deltas = server.getPublishedResourceDelta(modules);
    for (IModuleResourceDelta delta : deltas) {
      IStatus[] publishStatus = helper.publishDelta(delta, path, monitor);
      statusList.addAll(Arrays.asList(publishStatus));
    }
  }

  /**
   * Publish child modules as directory if not binary.
   */
  private void publishDir(IPath path, Properties mapping, List<IStatus> statusList,
      IProgressMonitor monitor, IModule childModule) throws CoreException {
    boolean isMoving = false;
    // check older publish
    String oldUri = (String) mapping.get(childModule.getId());
    String dirUri = path.toOSString();
    if (oldUri != null && dirUri != null) {
      isMoving = !oldUri.equals(dirUri);
    }
    // setup target
    IPath dirPath = (IPath) path.clone();
    // remove if needed
    if (isMoving || kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      if (oldUri != null) {
        File file = new File(oldUri);
        if (file.exists()) {
          IStatus[] status = PublishHelper.deleteDirectory(file, monitor);
          statusList.addAll(Arrays.asList(status));
        }
      }
      mapping.remove(childModule.getId());
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // check for changes
    if (!isMoving && kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
      IModuleResourceDelta[] delta = server.getPublishedResourceDelta(modules);
      if (delta == null || delta.length == 0) {
        return;
      }
    }
    // ensure directory exists
    if (!dirPath.toFile().exists()) {
      dirPath.toFile().mkdirs();
    }
    // do publish resources
    // republish or publish fully
    if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL) {
      IModuleResource[] resources = server.getResources(modules);
      IStatus[] publishStatus = helper.publishFull(resources, dirPath, monitor);
      statusList.addAll(Arrays.asList(publishStatus));
    } else {
      // publish changes only
      IModuleResourceDelta[] deltas = server.getPublishedResourceDelta(modules);
      for (IModuleResourceDelta delta : deltas) {
        IStatus[] publishStatus = helper.publishDelta(delta, dirPath, monitor);
        statusList.addAll(Arrays.asList(publishStatus));
      }
    }
    // store into mapping
    mapping.put(childModule.getId(), dirUri);
  }

}
