/*
 * Copyright 2017 Google LLC
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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.deploy.WarPublisher;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.ZipUtil;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public abstract class ChildModuleWarPublishTest {

  @Rule public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  private static final IProgressMonitor monitor = new NullProgressMonitor();
  private static Map<String, IProject> allProjects;
  private static IProject project;

  private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

  protected abstract List<String> getExpectedChildModuleNames();

  protected static void loadTestProjectZip(String testZip, String mainProject)
      throws IOException, CoreException {
    allProjects = ProjectUtils.importProjects(ChildModuleWarPublishTest.class,
        testZip, false /* checkBuildErrors */, monitor);
    project = allProjects.get(mainProject);
    assertNotNull(project);
  }

  @AfterClass
  public static void tearDown() {
    // Collapse projects to avoid "No IModelProvider exists for project" errors
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=511541
    SwtBotProjectActions.collapseProjects(bot);

    // close editors, so no property changes are dispatched on delete
    bot.closeAllEditors();

    IProject[] projects = allProjects.values().toArray(new IProject[0]);
    ProjectUtils.waitForProjects(projects);
    if (projects.length > 0) {
      try {
        projects[0].getWorkspace().delete(projects, true, null);
      } catch (CoreException | RuntimeException ex) {
        Logger.getLogger(ChildModuleWarPublishTest.class.getName()).log(Level.WARNING,
            ex.getMessage(), ex);
      }
    }
  }

  @Test
  public void testPublishExploded_childModulePublished() throws CoreException {
    IFolder exploded = project.getFolder("exploded-war");
    IFolder tempDirectory = project.getFolder("temp");
    try {
      WarPublisher.publishExploded(project,
          exploded.getLocation(), tempDirectory.getLocation(), monitor);

      exploded.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      for (String childModule : getExpectedChildModuleNames()) {
        assertTrue(exploded.getFile("WEB-INF/lib/" + childModule).exists());
      }
    } finally {
      exploded.delete(true, monitor);
      tempDirectory.delete(true, monitor);
    }
  }

  @Test
  public void testPublishWar_childModulePublished() throws CoreException {
    IFile war = project.getFile("my-app.war");
    IFolder unzipped = project.getFolder("unzipped");
    IFolder tempDirectory = project.getFolder("temp");
    try {
      WarPublisher.publishWar(project, war.getLocation(), tempDirectory.getLocation(), monitor);

      ZipUtil.unzip(war.getLocation().toFile(), unzipped.getLocation().toFile(), monitor);
      unzipped.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      for (String childModule : getExpectedChildModuleNames()) {
        assertTrue(unzipped.getFile("WEB-INF/lib/" + childModule).exists());
      }
    } finally {
      war.delete(true, monitor);
      unzipped.delete(true, monitor);
      tempDirectory.delete(true, monitor);
    }
  }
}
