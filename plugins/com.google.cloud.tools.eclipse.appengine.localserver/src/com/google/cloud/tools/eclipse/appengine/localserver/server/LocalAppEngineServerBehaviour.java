package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

public class LocalAppEngineServerBehaviour extends ServerBehaviourDelegate {

  @Override
  public void stop(boolean force) {
    setServerState(IServer.STATE_STOPPING);
    // TODO stop server
    setServerState(IServer.STATE_STOPPED);    
  }

  void setStarted() {
    setServerState(IServer.STATE_STARTED);    
  }
  
  void setStarting() {
    setServerState(IServer.STATE_STARTING);    
  }
  
}
