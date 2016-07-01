package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.junit.Assert.assertTrue;


public class AppEngineStandardDeployCommandHandlerTest {

  public void testIsEnabled() {
    assertTrue(new AppEngineStandardDeployCommandHandler().isEnabled());
  }
}
