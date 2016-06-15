package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

public class LocalAppEngineServerBehaviour extends ServerBehaviourDelegate {

  @Override
  public void stop(boolean force) {
    // TODO Auto-generated method stub
  }

  void setStarted() {
    setServerState(IServer.STATE_STARTED);    
  }
  
  void setStarting() {
    setServerState(IServer.STATE_STARTING);    
  }

}
