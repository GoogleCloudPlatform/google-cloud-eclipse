package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.model.RuntimeDelegate;

public class AppToolsRuntime extends RuntimeDelegate {
  @Override
  public IStatus validate() {
    // TODO Check validation from app tools lib?
    return Status.OK_STATUS;
  }

}
