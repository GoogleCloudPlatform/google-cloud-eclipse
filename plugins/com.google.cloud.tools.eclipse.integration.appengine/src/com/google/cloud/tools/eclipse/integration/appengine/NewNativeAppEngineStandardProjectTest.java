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
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineRuntime;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.JavaRuntimeUtils;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test creation of a new standard App Engine project.
 */
public class NewNativeAppEngineStandardProjectTest extends BaseProjectTest {
  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Test
  public void testWithDefaults() throws Exception {
    Assume.assumeTrue("Requires a Java 8 JRE", JavaRuntimeUtils.hasJavaSE8());
    String[] projectFiles = {"src/main/java/HelloAppEngine.java",
        "src/main/webapp/META-INF/MANIFEST.MF", "src/main/webapp/WEB-INF/appengine-web.xml",
        "src/main/webapp/WEB-INF/web.xml", "src/main/webapp/index.html"};
    createAndCheck("appWithDefault_java8", null /* packageName */, null /* runtime */,
        projectFiles);
  }

  @Test
  public void testWithPackage() throws Exception {
    Assume.assumeTrue("Requires a Java 8 JRE", JavaRuntimeUtils.hasJavaSE8());
    String[] projectFiles = {"src/main/java/app/engine/test/HelloAppEngine.java",
        "src/main/webapp/META-INF/MANIFEST.MF", "src/main/webapp/WEB-INF/appengine-web.xml",
        "src/main/webapp/WEB-INF/web.xml", "src/main/webapp/index.html",};
    createAndCheck("appWithPackage_java8", "app.engine.test", null /* runtime */, projectFiles);
  }

  @Test
  public void testWithPackage_java7() throws Exception {
    String[] projectFiles = {"src/main/java/app/engine/test/HelloAppEngine.java",
        "src/main/webapp/META-INF/MANIFEST.MF", "src/main/webapp/WEB-INF/appengine-web.xml",
        "src/main/webapp/WEB-INF/web.xml", "src/main/webapp/index.html",};
    createAndCheck("appWithPackage_java7", "app.engine.test", AppEngineRuntime.STANDARD_JAVA_7,
        projectFiles);
  }

  /** Create a project with the given parameters. */
  private void createAndCheck(String projectName, String packageName, AppEngineRuntime runtime,
      String[] projectFiles) throws CoreException {
    assertFalse(projectExists(projectName));
    project = SwtBotAppEngineActions.createNativeWebAppProject(bot, projectName, null, packageName,
        runtime);
    assertTrue(project.exists());

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull("Native App Engine projects should be faceted", facetedProject);

    if (runtime == null || runtime == AppEngineRuntime.STANDARD_JAVA_8) {
      // we don't currently export a JRE8 facet version
      assertNotNull("Project does not have standard facet",
          facetedProject.getProjectFacetVersion(AppEngineStandardFacet.FACET));
      assertEquals("Project does not have standard facet", "JRE8",
          facetedProject.getProjectFacetVersion(AppEngineStandardFacet.FACET).getVersionString());
      assertEquals(JavaFacet.VERSION_1_8, facetedProject.getProjectFacetVersion(JavaFacet.FACET));
      assertEquals(WebFacetUtils.WEB_31,
          facetedProject.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    } else {
      assertEquals("Project does not have standard facet", AppEngineStandardFacet.JRE7,
          facetedProject.getProjectFacetVersion(AppEngineStandardFacet.FACET));
      assertEquals(JavaFacet.VERSION_1_7, facetedProject.getProjectFacetVersion(JavaFacet.FACET));
      assertEquals(WebFacetUtils.WEB_25,
          facetedProject.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    }

    for (String projectFile : projectFiles) {
      Path projectFilePath = new Path(projectFile);
      assertTrue(project.exists(projectFilePath));
    }
    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.
    ProjectUtils.failIfBuildErrors("New native project has errors", project);
  }
}
