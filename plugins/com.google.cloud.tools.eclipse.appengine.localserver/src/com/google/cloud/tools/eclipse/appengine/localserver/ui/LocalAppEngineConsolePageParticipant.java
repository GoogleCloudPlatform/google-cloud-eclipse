package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServer;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;
import com.google.cloud.tools.eclipse.ui.util.console.CustomConsolePageParticipant;
import com.google.cloud.tools.eclipse.ui.util.console.TaggedMessageConsole;

/**
 * Adds a stop button for the App Engine runtime to the {@link TaggedMessageConsole}
 */
public class LocalAppEngineConsolePageParticipant extends CustomConsolePageParticipant<LocalAppEngineServerBehaviour> {
  
  @Override
  protected void runTerminateAction() {
      LocalAppEngineServerBehaviour serverBehaviour = getConsole().getTag();
      if (serverBehaviour != null) {
        // try to initiate a nice shutdown
        boolean force = serverBehaviour.getServer().getServerState() == IServer.STATE_STOPPING;
        serverBehaviour.stop(force);
      }
  }

  @Override
  protected boolean terminateEnabled(LocalAppEngineServerBehaviour serverBehaviour) {
    if (serverBehaviour != null) {
      IStatus status = serverBehaviour.canStop();
      return status.isOK();
    }
    return false;
  }
 
}

