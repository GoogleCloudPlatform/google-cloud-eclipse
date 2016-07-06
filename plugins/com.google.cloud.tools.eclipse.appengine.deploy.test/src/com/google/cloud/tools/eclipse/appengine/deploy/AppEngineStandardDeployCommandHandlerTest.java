package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class AppEngineStandardDeployCommandHandlerTest {

  @Test
  public void testIsEnabled() {
    assertTrue(new AppEngineStandardDeployCommandHandler().isEnabled());
  }
}
