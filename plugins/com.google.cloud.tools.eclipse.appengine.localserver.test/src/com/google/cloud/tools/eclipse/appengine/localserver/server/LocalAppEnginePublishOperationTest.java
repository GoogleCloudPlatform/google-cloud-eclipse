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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.ModuleFactory;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Verify that the server properly published submodule projects */
public class LocalAppEnginePublishOperationTest {
  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  private final List<IProject> projects = new ArrayList<>();
  private IProject serverProject;
  private IProject sharedProject;
  private IModule serverModule;
  private IModule sharedModule;
  private IServer server;

  @Before
  public void setUp() throws Exception {
    sharedProject = importProject("sox-shared");
    serverProject = importProject("sox-server");
    assertEquals(2, projects.size());

    serverModule = ServerUtil.getModule(serverProject);
    sharedModule = ServerUtil.getModule(sharedProject);

    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1798
    waitUntilProjectsReady();

    // To diagnose https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1798.
    logModules(serverProject);
    logModules(sharedProject);
    assertTrue(serverProject.getFile("bin/sox/server/GreetingServiceImpl.class").exists());
    assertTrue(sharedProject.getFile("bin/sox/shared/GreetingService.class").exists());

    assertNotNull(serverModule);
    assertNotNull(sharedModule);
  }

  @After
  public void tearDown() throws CoreException {
    if (!projects.isEmpty()) {
      projects.get(0).getWorkspace().delete(projects.toArray(new IProject[0]), true, null);
    }
    if (server != null) {
      server.delete();
    }
  }

  private IProject importProject(String projectName) throws IOException, CoreException {
    Map<String, IProject> imported = ProjectUtils.importProjects(getClass(),
        "projects/" + projectName + ".zip", true /* checkBuildErrors */, null);
    assertEquals(1, imported.size());
    IProject project = imported.get(projectName);
    assertNotNull(project);
    projects.add(project);
    return project;
  }

  /**
   * Verify that multi-web-module works.
   */
  @Test
  public void testPublishingSubmodules() throws CoreException {
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

  // Code taken from "ServerUtil.getModules()" (which "ServerUtil.getModule()" calls) to diagnose
  // one of the failures in https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1798.
  private static void logModules(IProject project) {
    System.out.println("  #### Testing getModules(" + project.getName() + ") ####");

    ModuleFactory[] factories = ServerPlugin.getModuleFactories();
    assertNotNull(factories);
    assertNotEquals(0, factories.length);

    for (ModuleFactory factory : factories) {
      boolean factoryEnabled = factory.isEnabled(project, null);
      System.out.println("    * " + factory + (!factoryEnabled? " (not enabled, skipping)" : ""));

      System.out.print("      - All factory modules:");
      printModules(factory.getDelegate(null).getModules());

      if (factoryEnabled) {
        System.out.print("      - Project modules:");
        printModules(factory.getModules(project, null));
      }
    }
  }

  private static void printModules(IModule[] modules) {
    if (modules == null) {
      System.out.println(" <null>");
    } else if (modules.length == 0) {
      System.out.println(" <empty>");
    } else {
      for (IModule module : modules) {
        System.out.println(" " + module.getName());
      }
    }
  }

  private void waitUntilProjectsReady() throws Exception {
    waitUntilCondition("Until fully built", this::classFilesExist, () -> Thread.sleep(100), 100);
    waitUntilCondition("Until modules fully ready", this::modulesReady, this::reopenProjects, 10);
  }

  private static void waitUntilCondition(String comment, BooleanSupplier condition,
      ThrowingRunnable interimAction, int tries) throws Exception {
    Stopwatch stopwatch = Stopwatch.createStarted();
    for (int i = 0; i < tries && !condition.getAsBoolean(); i++) {
      ThreadDumpingWatchdog.report(comment, stopwatch);
      interimAction.run();
    }
  }

  private boolean classFilesExist() {
    return serverProject.getFile("bin/sox/server/GreetingServiceImpl.class").exists()
        && sharedProject.getFile("bin/sox/shared/GreetingService.class").exists();
  }

  private boolean modulesReady() {
    return serverModule != null && sharedModule != null;
  }

  private void reopenProjects() throws CoreException {
    for (IProject project : projects) {
      project.close(null);
      project.open(null);
    }
    ProjectUtils.waitForProjects(projects);
    serverModule = ServerUtil.getModule(serverProject);
    sharedModule = ServerUtil.getModule(sharedProject);
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  };
}
