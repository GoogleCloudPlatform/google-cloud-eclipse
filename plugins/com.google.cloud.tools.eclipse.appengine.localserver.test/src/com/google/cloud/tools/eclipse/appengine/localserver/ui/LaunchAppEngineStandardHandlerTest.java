/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.test.util.ui.ExecutionEventBuilder;
import com.google.common.collect.Lists;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for the {@link LaunchAppEngineStandardHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LaunchAppEngineStandardHandlerTest {
  private static final IProjectFacetVersion APPENGINE_STANDARD_FACET_VERSION_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");

  @Rule
  public ServerTracker tracker = new ServerTracker();

  private LaunchAppEngineStandardHandler handler;
  private IServer serverToReturn = null;

  @Rule
  public TestProjectCreator appEngineStandardProject1 =
      new TestProjectCreator().withFacetVersions(Lists.newArrayList(JavaFacet.VERSION_1_7,
          WebFacetUtils.WEB_25, APPENGINE_STANDARD_FACET_VERSION_1));
  @Rule
  public TestProjectCreator appEngineStandardProject2 =
      new TestProjectCreator().withFacetVersions(Lists.newArrayList(JavaFacet.VERSION_1_7,
          WebFacetUtils.WEB_25, APPENGINE_STANDARD_FACET_VERSION_1));


  @Before
  public void setUp() {
    handler = new LaunchAppEngineStandardHandler() {
      @Override
      protected void launch(IServer server, String launchMode, SubMonitor progress)
          throws CoreException {
        // do nothing
      }

      @Override
      protected IServer findExistingServer(IModule[] modules, SubMonitor progress) {
        if (serverToReturn != null) {
          return serverToReturn;
        }
        return super.findExistingServer(modules, progress);
      }
    };
  }


  @Test
  public void testWithDefaultModule() throws ExecutionException, CoreException {
    IModule module1 = appEngineStandardProject1.getModule();

    ExecutionEvent event = new ExecutionEventBuilder().withCurrentSelection(module1).build();
    handler.execute(event);
    assertEquals("new server should have been created", 1, tracker.getServers().size());
  }

  @Test
  public void testWithTwoModules() throws ExecutionException, CoreException {
    appEngineStandardProject1.setAppEngineServiceId("default");
    IModule module1 = appEngineStandardProject1.getModule();
    appEngineStandardProject2.setAppEngineServiceId("other");
    IModule module2 = appEngineStandardProject2.getModule();

    ExecutionEvent event =
        new ExecutionEventBuilder().withCurrentSelection(module1, module2).build();
    handler.execute(event);
    assertEquals("new server should have been created", 1, tracker.getServers().size());
  }

  @Test(expected = ExecutionException.class)
  public void failsIfAlreadyLaunched() throws ExecutionException, CoreException {
    IModule module1 = appEngineStandardProject1.getModule();

    ExecutionEvent event = new ExecutionEventBuilder().withCurrentSelection(module1).build();
    serverToReturn = mock(IServer.class);
    ILaunch launch = mock(ILaunch.class);
    when(serverToReturn.getServerState()).thenReturn(IServer.STATE_STARTED);
    when(serverToReturn.getLaunch()).thenReturn(launch);
    when(launch.getLaunchMode()).thenReturn(ILaunchManager.DEBUG_MODE);
    handler.execute(event);
  }

  public void testInvariantToModuleOrder() throws ExecutionException, CoreException {
    appEngineStandardProject1.setAppEngineServiceId("default");
    IModule module1 = appEngineStandardProject1.getModule();
    appEngineStandardProject2.setAppEngineServiceId("other");
    IModule module2 = appEngineStandardProject2.getModule();

    ExecutionEvent event =
        new ExecutionEventBuilder().withCurrentSelection(module1, module2).build();
    handler.execute(event);
    assertEquals("new server should have been created", 1, tracker.getServers().size());

    // because we don't actually launch the servers, we won't get an ExecutionException
    ExecutionEvent swappedEvent =
        new ExecutionEventBuilder().withCurrentSelection(module2, module1).build();
    handler.execute(swappedEvent);
    assertEquals("no new servers should be created", 1, tracker.getServers().size());
  }

  @Test(expected = ExecutionException.class)
  public void failsWithClashingServiceIds() throws ExecutionException, CoreException {
    appEngineStandardProject1.setAppEngineServiceId("other");
    IModule module1 = appEngineStandardProject1.getModule();
    appEngineStandardProject2.setAppEngineServiceId("other");
    IModule module2 = appEngineStandardProject2.getModule();

    ExecutionEvent event =
        new ExecutionEventBuilder().withCurrentSelection(module1, module2).build();
    handler.execute(event);
  }

}
