package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.junit.Assert;
import org.junit.Test;


public class LocalAppEngineServerDelegateTest {

  private LocalAppEngineServerDelegate delegate = new LocalAppEngineServerDelegate();
  
  @Test
  public void testCanModifyModules() throws CoreException {
    IModule[] remove = new IModule[0];
    IModule[] add = new IModule[0];
    Assert.assertEquals(Status.OK_STATUS, delegate.canModifyModules(add, remove));
  }

}
