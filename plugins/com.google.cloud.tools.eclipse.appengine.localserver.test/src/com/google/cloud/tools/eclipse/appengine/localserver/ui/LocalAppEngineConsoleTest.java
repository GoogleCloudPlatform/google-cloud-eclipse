package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.junit.Assert;
import org.junit.Test;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;

public class LocalAppEngineConsoleTest {
  @Test
  public void testGetServerBehaviourDelegate_noDelegate() {
    LocalAppEngineConsole console = new LocalAppEngineConsole("test", null);
    Assert.assertNull(console.getServerBehaviourDelegate());
  }
  
  @Test
  public void testGetServerBehaviourDelegate_withDelegate() {
    LocalAppEngineServerBehaviour delegate = new LocalAppEngineServerBehaviour();
    LocalAppEngineConsole console = new LocalAppEngineConsole("test", delegate);
    Assert.assertEquals(delegate, console.getServerBehaviourDelegate());
  }
}
