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
