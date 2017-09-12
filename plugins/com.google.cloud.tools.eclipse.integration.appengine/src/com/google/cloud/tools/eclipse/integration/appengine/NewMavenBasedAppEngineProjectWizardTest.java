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
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineRuntime;
import com.google.cloud.tools.eclipse.test.util.ArrayAssertions;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.JavaRuntimeUtils;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test creation of a new project with the "Maven-Based Google App Engine Standard Java Project"
 * wizard.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class NewMavenBasedAppEngineProjectWizardTest extends BaseProjectTest {
  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Test
  public void testHelloWorld_java7() throws Exception {
    String[] projectFiles =
        {"src/main/webapp/WEB-INF/appengine-web.xml", "src/main/webapp/WEB-INF/web.xml", "pom.xml"};
    createAndCheck("appWithPackageProjectJava7", null, "com.example.baz",
        AppEngineRuntime.STANDARD_JAVA_7, projectFiles);
    assertEquals("1.7", getPomProperty(project, "maven.compiler.source"));
    assertEquals("1.7", getPomProperty(project, "maven.compiler.target"));
  }

  @Test
  public void testHelloWorld_java8() throws Exception {
    Assume.assumeTrue("Requires a Java 8 JRE", JavaRuntimeUtils.hasJavaSE8());
    String[] projectFiles =
        {"src/main/webapp/WEB-INF/appengine-web.xml", "src/main/webapp/WEB-INF/web.xml", "pom.xml"};
    createAndCheck("appWithPackageProjectJava8", null, "com.example.baz",
        AppEngineRuntime.STANDARD_JAVA_8, projectFiles);
    assertEquals("1.8", getPomProperty(project, "maven.compiler.source"));
    assertEquals("1.8", getPomProperty(project, "maven.compiler.target"));
  }

  @Test
  public void testHelloWorld_nonDefaultLocation() throws Exception {
    File location = File.createTempFile("maven", "dir");
    assertTrue("Unable to remove temp file", location.delete());
    assertTrue("Unable to turn temp location -> dir", location.mkdir());

    String[] projectFiles =
        {"src/main/webapp/WEB-INF/appengine-web.xml", "src/main/webapp/WEB-INF/web.xml", "pom.xml"};
    createAndCheck("appWithPackageProjectJava7InTemp", location.getAbsolutePath(), "com.example.foo",
        AppEngineRuntime.STANDARD_JAVA_7, projectFiles);
  }

  private static String getPomProperty(IProject project, String propertyName)
      throws CoreException, IOException {
    try (InputStream pom = project.getFile("pom.xml").getContents()) {
      return MavenUtils.getProperty(pom, propertyName);
    }
  }

  /** Create a project with the given parameters. */
  private void createAndCheck(String artifactId, String location, String packageName,
      AppEngineRuntime runtime, String[] projectFiles) throws CoreException, IOException {
    assertFalse(projectExists(artifactId));

    project = SwtBotAppEngineActions.createMavenWebAppProject(bot, artifactId, location,
        packageName, runtime, "com.google.groupId", artifactId);
    assertTrue(project.exists());
    if (location != null) {
      assertEquals(new File(location).getCanonicalPath(),
          project.getLocation().toFile().getCanonicalPath());
    }

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull("m2e-wtp should create a faceted project", facetedProject);
    if (runtime == null || runtime == AppEngineRuntime.STANDARD_JAVA_7) {
      assertEquals("Project does not have standard facet", AppEngineStandardFacet.JRE7,
          facetedProject.getProjectFacetVersion(AppEngineStandardFacet.FACET));
      assertEquals(JavaFacet.VERSION_1_7, facetedProject.getProjectFacetVersion(JavaFacet.FACET));
      assertEquals(WebFacetUtils.WEB_25,
          facetedProject.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    } else {
      // we don't currently export a JRE8 facet version
      assertNotNull("Project does not have standard facet",
          facetedProject.getProjectFacetVersion(AppEngineStandardFacet.FACET));
      assertEquals("Project does not have standard facet", "JRE8",
          facetedProject.getProjectFacetVersion(AppEngineStandardFacet.FACET).getVersionString());
      assertEquals(JavaFacet.VERSION_1_8, facetedProject.getProjectFacetVersion(JavaFacet.FACET));
      assertEquals(WebFacetUtils.WEB_31,
          facetedProject.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    }

    for (String projectFile : projectFiles) {
      Path projectFilePath = new Path(projectFile);
      assertTrue(project.exists(projectFilePath));
    }
    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.
    ProjectUtils.failIfBuildErrors("New Maven project has errors", project);
    
    ArrayAssertions.assertIsEmpty("runtime classpath should be empty for Maven projects", getAppEngineServerRuntimeClasspathEntries(project));
  }

  static IClasspathEntry[] getAppEngineServerRuntimeClasspathEntries(IProject project) {
    IJavaProject javaProject = JavaCore.create(project);
    IPath containerPath = new Path(
        "org.eclipse.jst.server.core.container/com.google.cloud.tools.eclipse.appengine.standard.runtimeClasspathProvider");
    try {
      for (IClasspathEntry entry : javaProject.getRawClasspath()) {
        if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
            && containerPath.isPrefixOf(entry.getPath())) {
          // resolve and return the entries
          IClasspathContainer container =
              JavaCore.getClasspathContainer(entry.getPath(), javaProject);
          return container.getClasspathEntries();
        }
      }
    } catch (JavaModelException ex) {
      fail(ex.toString());
      /* NOTREACHED */
    }
    fail("AppEngine Server Runtime classpath container not found");
    return null;
  }
}
