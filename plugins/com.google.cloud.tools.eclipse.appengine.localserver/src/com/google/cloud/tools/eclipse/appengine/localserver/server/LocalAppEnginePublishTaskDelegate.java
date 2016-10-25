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

import java.util.List;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.PublishTaskDelegate;

import com.google.common.collect.Lists;

public class LocalAppEnginePublishTaskDelegate extends PublishTaskDelegate {
  @Override
  @SuppressWarnings("rawtypes")
  public PublishOperation[] getTasks(IServer server, int kind, List/* <IModule[]> */ modules,
      List/* <Integer> */ kindList) {
    if (modules == null || modules.isEmpty()) {
      return null;
    }

    LocalAppEngineServerBehaviour gaeServer =
        (LocalAppEngineServerBehaviour) server.loadAdapter(LocalAppEngineServerBehaviour.class, null);

    List<PublishOperation> tasks = Lists.newArrayList();
    for (int i = 0; i < modules.size(); i++) {
      IModule[] module = (IModule[]) modules.get(i);
      tasks.add(new LocalAppEnginePublishOperation(gaeServer, kind, module, (Integer) kindList.get(i)));
    }

    return tasks.toArray(new PublishOperation[tasks.size()]);
  }
}
