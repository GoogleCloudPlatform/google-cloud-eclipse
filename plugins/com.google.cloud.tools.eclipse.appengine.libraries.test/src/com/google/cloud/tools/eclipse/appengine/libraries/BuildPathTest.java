/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.m2e.core.MavenPlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BuildPathTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator()
      .withFacets(JavaFacet.VERSION_1_8);

  private final IProgressMonitor monitor = new NullProgressMonitor();
  private IJavaProject project;
  private int initialClasspathSize;

  @Before
  public void setUp() throws JavaModelException {
    project = projectCreator.getJavaProject();
    initialClasspathSize = project.getRawClasspath().length;
    assertNull(BuildPath.findMasterContainer(project));
  }

  @Test
  public void testAddMavenLibraries_emptyList() throws CoreException {
    List<Library> libraries = new ArrayList<>();
    BuildPath.addMavenLibraries(project.getProject(), libraries, monitor);
    assertNull(BuildPath.findMasterContainer(project));
  }

  @Test
  public void testAddNativeLibrary() throws CoreException {
    Library library = new Library("libraryId");
    List<Library> libraries = new ArrayList<>();
    libraries.add(library);

    BuildPath.addNativeLibrary(project, libraries, monitor);
    assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);
    assertNotNull(BuildPath.findMasterContainer(project));
  }
  
  @Test
  public void testAddLibraries() throws CoreException {
    Library library = new Library("libraryId");
    List<Library> libraries = new ArrayList<>();
    libraries.add(library);
    BuildPath.addNativeLibrary(project, libraries, monitor);
    assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);
    assertNotNull(BuildPath.findMasterContainer(project));

    BuildPath.addNativeLibrary(project, libraries, monitor);
    assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);
  }

  @Test
  public void testCheckLibraries() throws CoreException {
    // Activator.listener monitors Java classpath changes and schedules concurrent jobs that call
    // "checkLibraryList()", which is the method being tested here. Prevent concurrent updates
    // to avoid flakiness: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2758
    ISchedulingRule buildRule = project.getProject().getWorkspace().getRuleFactory().buildRule();
    Job.getJobManager().beginRule(buildRule, null);

    try {
      IPath librariesIdPath =
          new Path(".settings").append(LibraryClasspathContainer.CONTAINER_PATH_PREFIX)
              .append("_libraries.container");

      // original classpath without our master-library container
      IClasspathEntry[] originalClasspath = project.getRawClasspath();

      Library library = new Library("libraryId");
      List<Library> libraries = new ArrayList<>();
      libraries.add(library);

      BuildPath.addNativeLibrary(project, libraries, monitor);
      assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);
      assertNotNull(BuildPath.findMasterContainer(project));
      assertTrue(project.getProject().exists(librariesIdPath));

      // master-library container exists, so checkLibraryList should make no change
      BuildPath.checkLibraryList(project, null);
      assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);
      assertNotNull(BuildPath.findMasterContainer(project));
      assertTrue(project.getProject().exists(librariesIdPath));

      // remove the master-library container, so checkLibraryList should remove the library ids file
      project.setRawClasspath(originalClasspath, null);
      BuildPath.checkLibraryList(project, null);
      assertFalse(librariesIdPath + " not removed", project.getProject().exists(librariesIdPath));
    } finally {
      Job.getJobManager().endRule(buildRule);
    }
  }

  @Test
  public void testResolvingRule() {
    ISchedulingRule rule = BuildPath.resolvingRule(project);
    assertTrue(rule.contains(MavenPlugin.getProjectConfigurationManager().getRule()));
    assertTrue(rule.isConflicting(MavenPlugin.getProjectConfigurationManager().getRule()));
    assertTrue(rule.contains(project.getProject()));
  }
}
