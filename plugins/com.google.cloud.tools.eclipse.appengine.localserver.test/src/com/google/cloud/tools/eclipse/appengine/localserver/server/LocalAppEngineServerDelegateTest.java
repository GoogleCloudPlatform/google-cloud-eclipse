package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.internal.ModuleType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("restriction") //For ModuleType
@RunWith(MockitoJUnitRunner.class)
public class LocalAppEngineServerDelegateTest {

  private LocalAppEngineServerDelegate delegate = new LocalAppEngineServerDelegate();
  private @Mock IModule module1;
  private @Mock IModule module2;
  private @Mock IWebModule webModule;

  @Test
  public void testCanModifyModules() {
    IModule[] remove = new IModule[0];
    IModule[] add = new IModule[0];
    Assert.assertEquals(Status.OK_STATUS, delegate.canModifyModules(add, remove));
  }

  @Test
  public void testGetChildModules_emptyList(){
    IModule[] childModules = delegate.getChildModules(new IModule[0]);
    Assert.assertEquals(0, childModules.length);
  }

  @Test
  public void testGetChildModules_noModuleType(){
    when(module1.getModuleType()).thenReturn(null);
    IModule[] childModules = delegate.getChildModules(new IModule[]{module1});
    Assert.assertEquals(0, childModules.length);
  }

  @Test
  public void testGetChildModules_nonWebModuleType(){
    ModuleType nonWebModuleType = new ModuleType("non-web", "1.0");
    when(module1.getModuleType()).thenReturn(nonWebModuleType);

    IModule[] childModules = delegate.getChildModules(new IModule[]{module1});
    Assert.assertEquals(0, childModules.length);
  }

  @Test
  public void testGetChildModules_webModuleType(){
    ModuleType webModuleType = new ModuleType("jst.web", "1.0");
    when(module1.getModuleType()).thenReturn(webModuleType);
    when(module1.getId()).thenReturn("module1");
    when(module2.getId()).thenReturn("module2");
    when(webModule.getModules()).thenReturn(new IModule[]{module2});
    when(module1.loadAdapter(IWebModule.class, null)).thenReturn(webModule);

    IModule[] childModules = delegate.getChildModules(new IModule[]{module1});
    Assert.assertEquals(1, childModules.length);
    Assert.assertEquals("module2", childModules[0].getId());
  }

  @Test
  public void testGetRootModules() throws CoreException {
    when(module1.getId()).thenReturn("module1");

    IModule[] rootModules = delegate.getRootModules(module1);
    Assert.assertEquals(1, rootModules.length);
    Assert.assertEquals("module1", rootModules[0].getId());
  }

}
