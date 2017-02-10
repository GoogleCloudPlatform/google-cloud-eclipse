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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;
import com.google.cloud.tools.eclipse.util.MavenUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Test creation of a new project with the "Maven-Based Google App Engine Standard Java Project"
 * wizard.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class NewMavenBasedAppEngineProjectWizardTest extends BaseProjectTest {
  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Test
  public void testHelloWorld() throws Exception {
    String[] projectFiles =
        {"src/main/webapp/WEB-INF/appengine-web.xml", "src/main/webapp/WEB-INF/web.xml", "pom.xml"};
    createAndCheck("appWithPackageProject", null, "com.example.baz", "Hello World template",
        projectFiles);
  }

  @Test
  public void testHelloWorldInTemp() throws Exception {
    File location = File.createTempFile("maven", "dir");
    assertTrue("Unable to remove temp file", location.delete());
    assertTrue("Unable to turn temp location -> dir", location.mkdir());

    String[] projectFiles =
        {"src/main/webapp/WEB-INF/appengine-web.xml", "src/main/webapp/WEB-INF/web.xml", "pom.xml"};
    createAndCheck("appWithPackageProjectInTemp", location.getAbsolutePath(), "com.example.foo",
        "Hello World template", projectFiles);
  }

  @Test
  public void testGuestbookExample() throws Exception {
    String[] projectFiles = {"src/main/webapp/guestbook.jsp",
        "src/main/webapp/WEB-INF/appengine-web.xml", "src/main/webapp/WEB-INF/web.xml", "pom.xml"};
    createAndCheck("guestbookExampleProject", null, "com.example.bar", "Guestbook example",
        projectFiles);
    // no projectId then archetypes use artifactID
    assertEquals("guestbookExampleProject", getPomProperty(project, "app.id"));
  }

  private static String getPomProperty(IProject project, String propertyName)
      throws CoreException, IOException {
    try (InputStream pom = project.getFile("pom.xml").getContents()) {
      return new MavenUtils().getProperty(pom, propertyName);
    }
  }

  /** Create a project with the given parameters. */
  private void createAndCheck(String artifactId, String location,
      String packageName, String archetypeDescription, String[] projectFiles)
      throws CoreException, IOException {
    assertFalse(projectExists(artifactId));

    project = SwtBotAppEngineActions.createMavenWebAppProject(bot, location,
        "com.google.groupId", artifactId, packageName, archetypeDescription);
    assertTrue(project.exists());
    if (location != null) {
      assertEquals(new File(location).getCanonicalPath(),
          project.getLocation().toFile().getParentFile().getCanonicalPath());
    }

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull("m2e-wtp should create a faceted project", facetedProject);
    assertTrue("Project does not have standard facet",
        FacetedProjectHelper.projectHasFacet(facetedProject, AppEngineStandardFacet.ID));

    for (String projectFile : projectFiles) {
      Path projectFilePath = new Path(projectFile);
      assertTrue(project.exists(projectFilePath));
    }
    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.
    ProjectUtils.failIfBuildErrors("New Maven project has errors", project);
  }
}
