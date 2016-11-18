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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/** Verify that the server properly published submodule projects */
public class LocalAppEnginePublishOperationTest {
  private List<IProject> projects;
  private IProject serverProject;
  private IProject sharedProject;
  private IModule serverModule;
  private IModule sharedModule;
  private IServer server;

  @Before
  public void setUp() throws IOException, CoreException {
    Assume.assumeFalse("This test is flakey on travis and needs to be de-flaked",
        Boolean.parseBoolean(System.getenv("TRAVIS")));

    projects = ProjectUtils.importProjects(getClass(), "projects/test-submodules.zip", null);
    assertEquals(2, projects.size());
    assertTrue("sox-server".equals(projects.get(0).getName())
        || "sox-server".equals(projects.get(1).getName()));
    assertTrue("sox-shared".equals(projects.get(1).getName())
        || "sox-shared".equals(projects.get(0).getName()));
    serverProject = projects.get("sox-server".equals(projects.get(0).getName()) ? 0 : 1);
    assertNotNull(serverProject);
    sharedProject = projects.get("sox-shared".equals(projects.get(0).getName()) ? 0 : 1);
    assertNotNull(sharedProject);

    assertTrue(serverProject.getFile("bin/sox/server/GreetingServiceImpl.class").exists());
    assertTrue(sharedProject.getFile("bin/sox/shared/GreetingService.class").exists());

    serverModule = ServerUtil.getModule(serverProject);
    assertNotNull(serverModule);
    sharedModule = ServerUtil.getModule(sharedProject);
    assertNotNull(sharedModule);
  }

  @After
  public void tearDown() throws CoreException {
    if (projects != null) {
      for (IProject project : projects) {
        project.delete(true, null);
      }
    }
    if (server != null) {
      server.delete();
    }
  }

  /**
   * Verify that multi-web-module works.
   */
  @Test
  public void testPublishingSubmodules() throws IOException, CoreException {
    IServerType serverType =
        ServerCore.findServerType("com.google.cloud.tools.eclipse.appengine.standard.server");
    IServerWorkingCopy serverWorkingCopy =
        serverType.createServer(getClass().getName(), null, null);
    serverWorkingCopy.modifyModules(new IModule[] {serverModule}, null, null);
    server = serverWorkingCopy.saveAll(true, null);
    assertTrue(server.canPublish().isOK());
    assertTrue("publish failed", server.publish(IServer.PUBLISH_CLEAN, null).isOK());

    LocalAppEngineServerBehaviour serverBehaviour =
        server.getAdapter(LocalAppEngineServerBehaviour.class);
    assertNotNull(serverBehaviour);

    // now verify the result
    IPath deployDirectory = serverBehaviour.getModuleDeployDirectory(serverModule);
    File publishedModule = deployDirectory.toFile();
    assertTrue(publishedModule.isDirectory());
    File webInf = new File(publishedModule, "WEB-INF");
    assertTrue(webInf.isDirectory());

    assertTrue(new File(webInf, "appengine-web.xml").isFile());
    assertTrue(new File(webInf, "web.xml").isFile());
    assertTrue(new File(webInf, "classes/sox/server/GreetingServiceImpl.class").isFile());
    assertTrue(new File(webInf, "lib/servlet-2.5.jar").isFile());
    assertTrue(new File(webInf, "lib/sox-shared.jar").isFile());
  }
}
