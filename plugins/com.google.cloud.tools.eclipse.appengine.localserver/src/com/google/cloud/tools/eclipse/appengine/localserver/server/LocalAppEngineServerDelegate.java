package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ServerDelegate;

public class LocalAppEngineServerDelegate extends ServerDelegate {

  @Override
  public IStatus canModifyModules(IModule[] add, IModule[] remove) {
    // todo what do we test here?
    return Status.OK_STATUS;
  }

  @Override
  public IModule[] getChildModules(IModule[] module) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IModule[] getRootModules(IModule module) throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor)
      throws CoreException {
    // TODO Auto-generated method stub

  }

}
