package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.junit.Test;

public class LocalAppEngineServerBehaviourTest {

  private LocalAppEngineServerBehaviour behaviour = new LocalAppEngineServerBehaviour();
  
  @Test
  public void testStop() {
    behaviour.stop(false);
  }

}
