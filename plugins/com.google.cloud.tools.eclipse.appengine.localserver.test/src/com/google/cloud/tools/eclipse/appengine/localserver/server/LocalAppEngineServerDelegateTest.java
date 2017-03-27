/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.ModuleType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("restriction") //For ModuleType
@RunWith(MockitoJUnitRunner.class)
public class LocalAppEngineServerDelegateTest {
  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  private LocalAppEngineServerDelegate delegate = new LocalAppEngineServerDelegate();
  private static final IProjectFacetVersion APPENGINE_STANDARD_FACET_VERSION_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");

  @Mock private IModule module1;
  @Mock private IWebModule webModule1;
  @Mock private IModule module2;
  @Mock private IWebModule webModule2;
  @Mock private IModule module3;
  @Rule public TestProjectCreator dynamicWebProject =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);
  @Rule
  public TestProjectCreator appEngineStandardProject =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
          APPENGINE_STANDARD_FACET_VERSION_1);

  @Test
  public void testCanModifyModules() throws CoreException {
    delegate = getDelegateWithServer();
    IModule[] remove = new IModule[0];
    IModule[] add = new IModule[0];
    Assert.assertEquals(Status.OK_STATUS, delegate.canModifyModules(add, remove));
  }

  @Test
  public void testCheckProjectFacets_NoAppEngineStandardFacet() throws CoreException {
    delegate = getDelegateWithServer();
    IModule[] add = new IModule[]{ module1 };
    when(module1.getProject()).thenReturn(dynamicWebProject.getProject());
    Assert.assertEquals(Status.ERROR, delegate.checkProjectFacets(add).getSeverity());
  }

  @Test
  public void testCheckProjectFacets_appEngineStandardFacet() throws CoreException {
    delegate = getDelegateWithServer();
    IModule[] add = new IModule[]{ module1 };
    when(module1.getProject()).thenReturn(appEngineStandardProject.getProject());
    Assert.assertEquals(Status.OK_STATUS, delegate.checkProjectFacets(add));
  }

  @Test
  public void testCheckConflictingId_defaultServiceIds() throws CoreException {
    delegate = getDelegateWithServer();
    delegate.serviceIdFunction = new Function<IModule, String>() {
      @Override
      public String apply(IModule module) {
        return "default";
      }
    };

    Assert.assertEquals(Status.ERROR, delegate.checkConflictingServiceIds(new IModule[] {module1},
        new IModule[] {module2}, null).getSeverity());

    // should be ok if we remove module1 and add module2
    Assert.assertEquals(Status.OK, delegate.checkConflictingServiceIds(new IModule[] {module1},
        new IModule[] {module2}, new IModule[] {module1}).getSeverity());
  }

  @Test
  public void testCheckConflictingId_differentServiceIds() throws CoreException {
    delegate = getDelegateWithServer();
    delegate.serviceIdFunction = new ModuleNameFunction();
    when(module1.getName()).thenReturn("module1");
    when(module2.getName()).thenReturn("module2");
    Assert.assertEquals(Status.OK, delegate.checkConflictingServiceIds(
        new IModule[] {module1}, new IModule[] {module2}, null).getSeverity());
  }

  /** https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1029 */
  @Test
  public void testCheckConflictingId_addExitingModule() throws CoreException {
    delegate = getDelegateWithServer();
    delegate.serviceIdFunction = new ModuleNameFunction();
    when(module1.getName()).thenReturn("module1");
    Assert.assertEquals(Status.OK, delegate.checkConflictingServiceIds(new IModule[] {module1},
        new IModule[] {module1}, null).getSeverity());
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
    IModuleType nonWebModuleType = ModuleType.getModuleType("non-web", "1.0");
    when(module1.getModuleType()).thenReturn(nonWebModuleType);

    IModule[] childModules = delegate.getChildModules(new IModule[]{module1});
    Assert.assertEquals(0, childModules.length);
  }

  @Test
  public void testGetChildModules_webModuleType() {
    IModuleType webModuleType = ModuleType.getModuleType("jst.web", "1.0");
    when(module1.getModuleType()).thenReturn(webModuleType);
    when(module1.getId()).thenReturn("module1");
    when(module1.loadAdapter(IWebModule.class, null)).thenReturn(webModule1);
    when(webModule1.getModules()).thenReturn(new IModule[] {module2});
    when(module2.getModuleType()).thenReturn(webModuleType);
    when(module2.getId()).thenReturn("module2");
    when(module2.loadAdapter(IWebModule.class, null)).thenReturn(webModule2);
    when(webModule2.getModules()).thenReturn(new IModule[] {module3});
    when(module3.getModuleType()).thenReturn(webModuleType);
    when(module3.getId()).thenReturn("module3");

    IModule[] childModules;
    childModules = delegate.getChildModules(new IModule[] {module1});
    Assert.assertEquals(1, childModules.length);
    Assert.assertEquals("module2", childModules[0].getId());

    childModules = delegate.getChildModules(new IModule[] {module1, module2});
    Assert.assertEquals(1, childModules.length);
    Assert.assertEquals("module3", childModules[0].getId());
  }

  @Test
  public void testGetRootModules() throws CoreException {
    when(module1.getId()).thenReturn("module1");

    IModule[] rootModules = delegate.getRootModules(module1);
    Assert.assertEquals(1, rootModules.length);
    Assert.assertEquals("module1", rootModules[0].getId());
  }

  @Test
  public void testGetModuleUrls_nullOnNoBehaviour() throws CoreException {
    delegate = getDelegateWithServer();
    Assert.assertEquals("localhost", delegate.getServer().getHost());
    LocalAppEngineServerBehaviour behaviour =
        delegate.getServer().getAdapter(LocalAppEngineServerBehaviour.class);
    assertNull(behaviour); // since not started, and no loadAdapter
    assertNull(delegate.getModuleRootURL(null));
  }

  @Test
  public void testGetModuleUrls_nullModule() throws CoreException {
    delegate = getDelegateWithServer();
    Assert.assertEquals("localhost", delegate.getServer().getHost());
    LocalAppEngineServerBehaviour behaviour = (LocalAppEngineServerBehaviour) delegate.getServer()
        .loadAdapter(LocalAppEngineServerBehaviour.class, null);
    behaviour.adminPort = 9999;
    URL url = delegate.getModuleRootURL(null);
    assertNotNull(url);
    assertEquals("http://localhost:9999", url.toString());
  }

  @Test
  public void testGetModuleUrls_baseModule() throws CoreException {
    delegate = getDelegateWithServer();
    Assert.assertEquals("localhost", delegate.getServer().getHost());
    LocalAppEngineServerBehaviour behaviour = (LocalAppEngineServerBehaviour) delegate.getServer()
        .loadAdapter(LocalAppEngineServerBehaviour.class, null);
    delegate.serviceIdFunction = new ModuleNameFunction();
    behaviour.moduleToUrlMap.put("module1", "http://foo:9999");
    when(module1.getName()).thenReturn("module1");

    URL url = delegate.getModuleRootURL(module1);
    assertNotNull(url);
    assertEquals("http://foo:9999", url.toString());
  }

  private LocalAppEngineServerDelegate getDelegateWithServer() throws CoreException {
    IServerWorkingCopy serverWorkingCopy =
        ServerCore.findServerType("com.google.cloud.tools.eclipse.appengine.standard.server")
          .createServer("testServer", null, null);
    IRuntimeWorkingCopy runtimeWorkingCopy =
        ServerCore.findRuntimeType("com.google.cloud.tools.eclipse.appengine.standard.runtime")
          .createRuntime("testRuntime", null);
    IRuntime runtime = runtimeWorkingCopy.save(true, null);
    serverWorkingCopy.setRuntime(runtime);
    IServer original = serverWorkingCopy.save(true, null);
    return LocalAppEngineServerDelegate.getAppEngineServer(original);
  }

  private static class ModuleNameFunction implements Function<IModule, String> {
    @Override
    public String apply(IModule module) {
      Preconditions.checkNotNull(module);
      return module.getName();
    }
  }
}
