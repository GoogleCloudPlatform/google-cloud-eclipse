package com.google.cloud.tools.eclipse.appengine.localserver.server;

import java.util.Map;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IProcessFactory;
import org.eclipse.debug.core.model.IProcess;

public class DevAppServerProcessFactory implements IProcessFactory {

  public static final String ID = "com.google.cloud.tools.eclipse.appengine.localserver.processfactory";

  @Override
  public IProcess newProcess(ILaunch launch,
                             Process process,
                             String label,
                             Map<String, String> attributes) {
    return new DevAppServerRuntimeProcess(launch, process, label, attributes);
  }

}
