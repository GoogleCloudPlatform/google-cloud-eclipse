/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

abstract public class CreateAppEngineWtpProjectTest {

  @Rule public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  protected final IProgressMonitor monitor = new NullProgressMonitor();
  protected final AppEngineProjectConfig config = new AppEngineProjectConfig();
  protected IProject project;

  abstract protected CreateAppEngineWtpProject newCreateAppEngineWtpProject();

  @SuppressWarnings("unused")  // Let subclasses throw arbitrary exceptions during "setUp()"
  @Before
  public void setUp() throws Exception {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("testproject" + Math.random());
    config.setProject(project);
  }

  @After
  public void tearDown() throws CoreException {
    if (project.exists()) {
      // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1945
      ProjectUtils.waitForProjects(project);
      project.delete(true, monitor);
    }
  }

  @Test
  public void testFaviconAdded() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);
    assertTrue("favicon.ico not found", project.getFile("src/main/webapp/favicon.ico").exists());
  }

  @Test
  public void testMostImportantFile() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    assertEquals("HelloAppEngine.java", creator.getMostImportant().getName());
  }

  @Test
  public void testUnitTestCreated() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);
    ProjectUtils.waitForProjects(project);

    assertJunitAndHamcrestAreOnClasspath();
  }

  private void assertJunitAndHamcrestAreOnClasspath() throws CoreException {
    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    IJavaProject javaProject = JavaCore.create(project);
    IType junit = javaProject.findType("org.junit.Assert");

    // Is findType doing what we think it's doing?
    // Locally where it passes it finds JUnit in
    // class Assert [in Assert.class [in org.junit [in /Users/elharo/workspace/.metadata/.plugins/org.eclipse.pde.core/.bundle_pool/plugins/org.junit_4.12.0.v201504281640/junit.jar]]]

    assertNotNull("Did not find junit", junit);
    assertTrue(junit.exists());
    IType hamcrest = javaProject.findType("org.hamcrest.CoreMatchers");
    assertNotNull("Did not find hamcrest", hamcrest);
    assertTrue(hamcrest.exists());
  }

  @Test
  public void testJavaTestSourceOutput() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    assertCorrectOutputPathForJavaTestSource();
  }

  private void assertCorrectOutputPathForJavaTestSource() throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE
          && containsSegment(entry.getPath(), "test")) {
        assertNotNull(entry.getOutputLocation());
        assertEquals("test-classes", entry.getOutputLocation().lastSegment());
        return;
      }
    }
    fail();
  }

  private boolean containsSegment(IPath path, String segment) {
    return Arrays.asList(path.segments()).contains(segment);
  }

  @Test
  public void testNoTestClassesInDeploymentAssembly()
      throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    assertNoTestClassesInDeploymentAssembly();
  }

  private void assertNoTestClassesInDeploymentAssembly() {
    StructureEdit core = StructureEdit.getStructureEditForRead(project);
    try {
      WorkbenchComponent component = core.getComponent();
      assertEquals(0, component.findResourcesBySourcePath(new Path("/src/test/java"), 0).length);
      assertEquals(1, component.findResourcesBySourcePath(new Path("/src/main/java"), 0).length);
    } finally {
      core.dispose();
    }
  }

  @Test
  public void testNoMavenNatureByDefault() throws InvocationTargetException, CoreException {
    assertFalse(config.getUseMaven());
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    assertFalse(project.hasNature(MavenUtils.MAVEN2_NATURE_ID));
    assertTrue(project.getFolder("build").exists());
    assertOutputDirectory("build/classes");
  }

  @Test
  public void testMavenNatureEnabled() throws InvocationTargetException, CoreException {
    config.setUseMaven("my.group.id", "my-artifact-id", "12.34.56");

    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);
    ProjectUtils.waitForProjects(project);

    assertTrue(project.hasNature(MavenUtils.MAVEN2_NATURE_ID));
    assertFalse(project.getFolder("build").exists());
    assertOutputDirectory("target/my-artifact-id-12.34.56/WEB-INF/classes");
  }

  protected void assertOutputDirectory(String expected) throws JavaModelException {
    assertTrue(project.getFolder(expected).exists());
    IJavaProject javaProject = JavaCore.create(project);
    assertEquals(new Path(expected), javaProject.getOutputLocation().removeFirstSegments(1));
  }

  @Test
  public void testJUnit4ClasspathIfNotUsingMaven() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);
    assertTrue(hasJUnit4Classpath(project));
  }

  @Test
  public void testNoJUnit4ClasspathIfUsingMaven() throws InvocationTargetException, CoreException {
    config.setUseMaven("my.group.id", "my-artifact-id", "12.34.56");

    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);
    assertFalse(hasJUnit4Classpath(project));
  }

  private static boolean hasJUnit4Classpath(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      if (entry.getPath().equals(JUnitCore.JUNIT4_CONTAINER_PATH)) {
        return true;
      }
    }
    return false;
  }
}
