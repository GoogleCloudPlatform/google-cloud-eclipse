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

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.PublishTaskDelegate;

import java.util.List;

/**
 * Setup task for module publishing.
 */
public class CloudSdkPublishTaskDelegate extends PublishTaskDelegate {

  @Override
  @SuppressWarnings("rawtypes")
  public PublishOperation[] getTasks(IServer server, int kind, List/* <IModule[]> */ modules,
      List/* <Integer> */ kindList) {
    if (modules == null || modules.isEmpty()) {
      return null;
    }

    CloudSdkServerBehaviour gaeServer =
        (CloudSdkServerBehaviour) server.loadAdapter(CloudSdkServerBehaviour.class, null);

    List<PublishOperation> tasks = Lists.newArrayList();
    for (int i = 0; i < modules.size(); i++) {
      IModule[] module = (IModule[]) modules.get(i);
      tasks.add(new CloudSdkPublishOperation(gaeServer, kind, module, (Integer) kindList.get(i)));
    }

    return tasks.toArray(new PublishOperation[tasks.size()]);
  }
}
