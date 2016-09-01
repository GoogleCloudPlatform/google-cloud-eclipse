package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.junit.Assert;
import org.junit.Test;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;
import com.google.cloud.tools.eclipse.ui.util.console.TaggedMessageConsole;

public class LocalAppEngineConsoleTest {
  @Test
  public void testGetServerBehaviourDelegate_noDelegate() {
    TaggedMessageConsole console = new TaggedMessageConsole("test", null);
    Assert.assertNull(console.getTag());
  }
  
  @Test
  public void testGetServerBehaviourDelegate_withDelegate() {
    LocalAppEngineServerBehaviour delegate = new LocalAppEngineServerBehaviour();
    TaggedMessageConsole console = new TaggedMessageConsole("test", delegate);
    Assert.assertEquals(delegate, console.getTag());
  }
}
