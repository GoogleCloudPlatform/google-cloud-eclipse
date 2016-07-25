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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineDeployInfo;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;

/**
 * Test creation of a new standard App Engine project.
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CreateNativeAppEngineStandardProjectTest {
  private static SWTWorkbenchBot bot;
  private IProject project;

  @BeforeClass
  public static void beforeClass() throws Exception {
    bot = new SWTWorkbenchBot();
    bot.viewByTitle("Welcome").close();
  }

  @After
  public void tearDown() throws CoreException {
    if (project != null) {
      SwtBotProjectActions.deleteProject(bot, project.getName());
    }
  }

  @AfterClass
  public static void sleep() {
    bot.sleep(100);
  }

  @Test
  public void testCreateNewNativeProjectWithDefaults() throws Exception {
    String projectName = "appWithDefault";
    assertFalse(projectExists(projectName));
    project = SwtBotAppEngineActions.createNativeWebAppProject(bot, projectName, null, null, null);
    assertTrue(project.exists());

    IFacetedProject facetedProject = new FacetedProjectHelper().getFacetedProject(project);
    assertTrue(
        new FacetedProjectHelper().projectHasFacet(facetedProject, AppEngineStandardFacet.ID));

    String[] projectFiles = {"src/main/java/HelloAppEngine.java",
        "src/main/webapp/META-INF/MANIFEST.MF", "src/main/webapp/WEB-INF/appengine-web.xml",
        "src/main/webapp/WEB-INF/web.xml", "src/main/webapp/index.html",};
    for (String projectFile : projectFiles) {
      Path projectFilePath = new Path(projectFile);
      assertTrue(project.exists(projectFilePath));
    }
    assertNull(getAppEngineProjectId(project.getFile("src/main/webapp/WEB-INF/appengine-web.xml")));
  }

  @Test
  public void testCreateNewNativeProjectWithPackage() throws Exception {
    String projectName = "appWithPackage";
    assertFalse(projectExists(projectName));
    project = SwtBotAppEngineActions.createNativeWebAppProject(bot, projectName, null,
        "app.engine.test", null);
    assertTrue(project.exists());

    IFacetedProject facetedProject = new FacetedProjectHelper().getFacetedProject(project);
    assertTrue("project doesn't have facet " + AppEngineStandardFacet.ID,
        new FacetedProjectHelper().projectHasFacet(facetedProject, AppEngineStandardFacet.ID));

    String[] projectFiles = {"src/main/java/app/engine/test/HelloAppEngine.java",
        "src/main/webapp/META-INF/MANIFEST.MF", "src/main/webapp/WEB-INF/appengine-web.xml",
        "src/main/webapp/WEB-INF/web.xml", "src/main/webapp/index.html",};
    for (String projectFile : projectFiles) {
      Path projectFilePath = new Path(projectFile);
      assertTrue(project.exists(projectFilePath));
    }
    assertNull(getAppEngineProjectId(project.getFile("src/main/webapp/WEB-INF/appengine-web.xml")));
  }

  @Test
  public void testCreateNewNativeProjectWithPackageAndProjectId() throws Exception {
    String projectName = "appWithPackageAndProjectId";
    assertFalse(projectExists(projectName));
    project = SwtBotAppEngineActions.createNativeWebAppProject(bot, projectName, null,
        "app.engine.test",
        "my-project-id");
    assertTrue(project.exists());

    IFacetedProject facetedProject = new FacetedProjectHelper().getFacetedProject(project);
    assertTrue("project doesn't have facet " + AppEngineStandardFacet.ID,
        new FacetedProjectHelper().projectHasFacet(facetedProject, AppEngineStandardFacet.ID));

    String[] projectFiles = {"src/main/java/app/engine/test/HelloAppEngine.java",
        "src/main/webapp/META-INF/MANIFEST.MF", "src/main/webapp/WEB-INF/appengine-web.xml",
        "src/main/webapp/WEB-INF/web.xml", "src/main/webapp/index.html",};
    for (String projectFile : projectFiles) {
      Path projectFilePath = new Path(projectFile);
      assertTrue(project.exists(projectFilePath));
    }
    assertEquals("my-project-id",
        getAppEngineProjectId(project.getFile("src/main/webapp/WEB-INF/appengine-web.xml")));
  }

  /**
   * Extracts the project ID from the given file.
   * 
   * @return the project ID or {@code null} if none found
   * @throws a variety of exceptions
   */
  private String getAppEngineProjectId(IFile appEngineXml) throws Exception {
    try (InputStream contents = appEngineXml.getContents()) {
      AppEngineDeployInfo info = new AppEngineDeployInfo();
      info.parse(contents);
      String projectId = info.getProjectId();
      if (projectId == null || projectId.trim().isEmpty()) {
        return null;
      }
      return projectId;
    }
  }


  /**
   * Returns the named project; it may not yet exist.
   */
  private IProject findProject(String projectName) {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
  }

  /**
   * Return true if a project by the given name exists.
   */
  private boolean projectExists(String projectName) {
    IProject project = findProject(projectName);
    return project.exists();
  }
}
