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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Test creation of a new standard App Engine project.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class NewNativeAppEngineStandardProjectTest extends BaseProjectTest {
  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Test
  public void testWithDefaults() throws Exception {
    String[] projectFiles = {"src/main/java/HelloAppEngine.java",
        "src/main/webapp/META-INF/MANIFEST.MF", "src/main/webapp/WEB-INF/appengine-web.xml",
        "src/main/webapp/WEB-INF/web.xml", "src/main/webapp/index.html"};
    createAndCheck("appWithDefault", null, projectFiles);
  }

  @Test
  public void testWithPackage() throws Exception {
    String[] projectFiles = {"src/main/java/app/engine/test/HelloAppEngine.java",
        "src/main/webapp/META-INF/MANIFEST.MF", "src/main/webapp/WEB-INF/appengine-web.xml",
        "src/main/webapp/WEB-INF/web.xml", "src/main/webapp/index.html",};
    createAndCheck("appWithPackage", "app.engine.test", projectFiles);
  }

  /** Create a project with the given parameters. */
  private void createAndCheck(String projectName, String packageName, String[] projectFiles)
                                                                              throws CoreException {
    assertFalse(projectExists(projectName));
    project = SwtBotAppEngineActions.createNativeWebAppProject(bot, projectName, null,
        packageName);
    assertTrue(project.exists());

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull("Native App Engine projects should be faceted", facetedProject);
    assertTrue("Project does not have standard facet",
        AppEngineStandardFacet.hasFacet(facetedProject));

    for (String projectFile : projectFiles) {
      Path projectFilePath = new Path(projectFile);
      assertTrue(project.exists(projectFilePath));
    }
    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.
    ProjectUtils.failIfBuildErrors("New native project has errors", project);
  }
}
