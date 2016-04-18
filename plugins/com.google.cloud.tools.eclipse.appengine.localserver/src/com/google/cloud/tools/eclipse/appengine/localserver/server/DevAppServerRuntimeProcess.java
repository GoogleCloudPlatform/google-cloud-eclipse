package com.google.cloud.tools.eclipse.appengine.localserver.server;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

public class DevAppServerRuntimeProcess extends RuntimeProcess {

  public DevAppServerRuntimeProcess(ILaunch launch,
                                    Process process,
                                    String name,
                                    Map<String, String> attributes) {
    super(launch, process, name, attributes);
  }

  @Override
  public void terminate() throws DebugException {
    try {
      sendQuitRequest();
      if (!getLaunch().isTerminated()) {
        super.terminate();
      }
    } catch (CoreException e) {
      throw new DebugException(e.getStatus());
    }

  }

  private void sendQuitRequest() throws CoreException {
    final IServer server = ServerUtil.getServer(getLaunch().getLaunchConfiguration());
    if (server == null) {
      return;
    }
    server.stop(true);
    try {
      // the stop command is async, let's give it some time to execute
      Thread.sleep(2000L);
    } catch (InterruptedException e) {
    }
  }

}
