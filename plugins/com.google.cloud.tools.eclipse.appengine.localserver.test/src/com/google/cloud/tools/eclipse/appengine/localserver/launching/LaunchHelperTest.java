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

package com.google.cloud.tools.eclipse.appengine.localserver.launching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.localserver.ui.ServerTracker;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.Collection;
import java.util.Collections;
import org.eclipse.core.resources.IFile;
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
public class LaunchHelperTest {
  private static final IProjectFacetVersion APPENGINE_STANDARD_FACET_VERSION_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");

  @Rule
  public ServerTracker tracker = new ServerTracker();

  private LaunchHelper handler;
  private IServer serverToReturn = null;

  @Rule
  public TestProjectCreator appEngineStandardProject1 = new TestProjectCreator().withFacetVersions(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, APPENGINE_STANDARD_FACET_VERSION_1);
  @Rule
  public TestProjectCreator appEngineStandardProject2 = new TestProjectCreator().withFacetVersions(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, APPENGINE_STANDARD_FACET_VERSION_1);


  @Before
  public void setUp() {
    handler = new LaunchHelper() {
      @Override
      protected void launch(IServer server, String launchMode, SubMonitor progress)
          throws CoreException {
        // do nothing
      }


      @Override
      public Collection<IServer> findExistingServers(IModule[] modules, boolean exact,
          SubMonitor progress) {
        if (serverToReturn != null) {
          return Collections.singleton(serverToReturn);
        }
        return super.findExistingServers(modules, exact, progress);
      }
    };
  }


  @Test
  public void testWithDefaultModule() throws CoreException {
    IModule module1 = appEngineStandardProject1.getModule();

    handler.launch(new IModule[] {module1}, ILaunchManager.DEBUG_MODE);
    assertEquals("new server should have been created", 1, tracker.getServers().size());
  }

  @Test
  public void testWithTwoModules() throws CoreException {
    appEngineStandardProject1.setAppEngineServiceId("default");
    IModule module1 = appEngineStandardProject1.getModule();
    appEngineStandardProject2.setAppEngineServiceId("other");
    IModule module2 = appEngineStandardProject2.getModule();

    handler.launch(new IModule[] {module1, module2}, ILaunchManager.DEBUG_MODE);
    assertEquals("new server should have been created", 1, tracker.getServers().size());
  }

  @Test(expected = CoreException.class)
  public void failsIfAlreadyLaunched() throws CoreException {
    IModule module1 = appEngineStandardProject1.getModule();

    serverToReturn = mock(IServer.class);
    ILaunch launch = mock(ILaunch.class);
    when(serverToReturn.getServerState()).thenReturn(IServer.STATE_STARTED);
    when(serverToReturn.getLaunch()).thenReturn(launch);
    when(launch.getLaunchMode()).thenReturn(ILaunchManager.DEBUG_MODE);
    handler.launch(new IModule[] {module1}, ILaunchManager.DEBUG_MODE);
  }

  @Test
  public void testInvariantToModuleOrder() throws CoreException {
    appEngineStandardProject1.setAppEngineServiceId("default");
    IModule module1 = appEngineStandardProject1.getModule();
    appEngineStandardProject2.setAppEngineServiceId("other");
    IModule module2 = appEngineStandardProject2.getModule();

    handler.launch(new IModule[] {module1, module2}, ILaunchManager.DEBUG_MODE);
    assertEquals("new server should have been created", 1, tracker.getServers().size());

    // because we don't actually launch the servers, we won't get an ExecutionException
    handler.launch(new IModule[] {module2, module1}, ILaunchManager.DEBUG_MODE);
    assertEquals("no new servers should be created", 1, tracker.getServers().size());
  }

  @Test(expected = CoreException.class)
  public void failsWithClashingServiceIds() throws CoreException {
    appEngineStandardProject1.setAppEngineServiceId("other");
    IModule module1 = appEngineStandardProject1.getModule();
    appEngineStandardProject2.setAppEngineServiceId("other");
    IModule module2 = appEngineStandardProject2.getModule();

    handler.launch(new IModule[] {module1, module2}, ILaunchManager.DEBUG_MODE);
  }

  @Test
  public void testToProjectWithProject() {
    assertEquals(appEngineStandardProject1.getProject(),
        LaunchHelper.toProject(appEngineStandardProject1.getProject()));
  }

  @Test
  public void testToProjectWithJavaProject() {
    assertEquals(appEngineStandardProject1.getProject(),
        LaunchHelper.toProject(appEngineStandardProject1.getJavaProject()));
  }

  @Test
  public void testToProjectWithFile() {
    IFile file = appEngineStandardProject1.getProject().getFile("WebContent/META-INF/MANIFEST.MF");
    assertTrue("MANIFEST.MF not found", file.exists());
    assertEquals(appEngineStandardProject1.getProject(), LaunchHelper.toProject(file));
  }
}
