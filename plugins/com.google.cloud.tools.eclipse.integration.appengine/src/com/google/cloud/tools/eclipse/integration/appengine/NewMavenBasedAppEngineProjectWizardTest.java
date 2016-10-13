/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;
import com.google.cloud.tools.eclipse.util.MavenUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Test creation of a new project with the "Maven-Based Google App Engine Standard Java Project"
 * wizard.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class NewMavenBasedAppEngineProjectWizardTest extends AbstractProjectTests {

  @Test
  public void testHelloWorld() throws Exception {
    // appengine-skeleton-archetype still missing base index.html and HelloWorld.java
    String[] projectFiles =
        {"src/main/webapp/WEB-INF/appengine-web.xml", "src/main/webapp/WEB-INF/web.xml", "pom.xml"};
    createAndCheck("appWithPackage", null, "app.engine.test", null, "Hello-world template",
        projectFiles);
    assertEquals("${app.id}",
        SwtBotAppEngineActions
            .getAppEngineProjectId(project.getFile("src/main/webapp/WEB-INF/appengine-web.xml")));
  }

  @Test
  public void testGuestbookExampleNoProjectId() throws Exception {
    // appengine-skeleton-archetype still missing base index.html and HelloWorld.java
    String[] projectFiles = {"src/main/webapp/guestbook.jsp",
        "src/main/webapp/WEB-INF/appengine-web.xml", "src/main/webapp/WEB-INF/web.xml", "pom.xml"};
    createAndCheck("guestbookExample", null, "app.engine.test", null, "Guestbook example",
        projectFiles);
    assertEquals("${app.id}",
        SwtBotAppEngineActions
            .getAppEngineProjectId(project.getFile("src/main/webapp/WEB-INF/appengine-web.xml")));
    // no projectId then archetypes use artifactID
    assertEquals("guestbookExample", getPomProperty(project, "app.id"));
  }

  @Test
  public void testGuestbookExampleWithProjectId() throws Exception {
    // appengine-skeleton-archetype still missing base index.html and HelloWorld.java
    String[] projectFiles = {"src/main/webapp/guestbook.jsp",
        "src/main/webapp/WEB-INF/appengine-web.xml", "src/main/webapp/WEB-INF/web.xml", "pom.xml"};
    createAndCheck("guestbookExample", null, "app.engine.test", "my-project-id",
        "Guestbook example", projectFiles);
    assertEquals("${app.id}",
        SwtBotAppEngineActions
            .getAppEngineProjectId(project.getFile("src/main/webapp/WEB-INF/appengine-web.xml")));
    assertEquals("my-project-id", getPomProperty(project, "app.id"));
  }

  private static String getPomProperty(IProject project, String propertyName)
      throws CoreException, IOException {
    try (InputStream pom = project.getFile("pom.xml").getContents()) {
      return new MavenUtils().getProperty(pom, propertyName);
    }
  }

  /** Create a project with the given parameters. */
  private void createAndCheck(String projectName, String location,
      String packageName, String projectId, String archetypeDescription, String[] projectFiles)
      throws Exception {
    assertFalse(projectExists(projectName));
    project = SwtBotAppEngineActions.createMavenWebAppProject(bot, location, "test", projectName,
        packageName, projectId, archetypeDescription);
    assertTrue(project.exists());

    IFacetedProject facetedProject = new FacetedProjectHelper().getFacetedProject(project);
    assertNotNull("m2e-wtp should create a faceted project", facetedProject);
    assertTrue(
        new FacetedProjectHelper().projectHasFacet(facetedProject, AppEngineStandardFacet.ID));

    for (String projectFile : projectFiles) {
      Path projectFilePath = new Path(projectFile);
      assertTrue(project.exists(projectFilePath));
    }
    List<String> buildErrors = SwtBotProjectActions.getAllBuildErrors(bot);
    if (!buildErrors.isEmpty()) {
      fail(buildErrors.get(0));
    }
  }
}
