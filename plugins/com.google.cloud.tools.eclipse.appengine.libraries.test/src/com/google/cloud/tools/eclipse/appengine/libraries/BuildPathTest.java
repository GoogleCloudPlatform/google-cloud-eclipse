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

import static org.junit.Assert.assertFalse;
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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BuildPathTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator()
      .withFacets(JavaFacet.VERSION_1_7);

  private final IProgressMonitor monitor = new NullProgressMonitor();
  private IJavaProject project;
  private int initialClasspathSize;

  @Before
  public void setUp() throws JavaModelException {
    project = projectCreator.getJavaProject();
    initialClasspathSize = project.getRawClasspath().length;
  }

  @Test
  public void testAddMavenLibraries_emptyList() throws CoreException {
    List<Library> libraries = new ArrayList<>();
    BuildPath.addMavenLibraries(project.getProject(), libraries, monitor);
  }

  @Test
  public void testAddNativeLibrary() throws CoreException {
    Library library = new Library("libraryId");
    List<Library> libraries = new ArrayList<>();
    libraries.add(library);

    BuildPath.addNativeLibrary(project, libraries, monitor);
    Assert.assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);
  }
  
  @Test
  public void testAddLibraries() throws CoreException {
    Library library = new Library("libraryId");
    List<Library> libraries = new ArrayList<>();
    libraries.add(library);
    BuildPath.addNativeLibrary(project, libraries, monitor);
    Assert.assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);

    BuildPath.addNativeLibrary(project, libraries, monitor);
    Assert.assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);
  }

  @Test
  public void testCheckLibraries() throws CoreException {
    IPath librariesIdPath =
        new Path(".settings").append(LibraryClasspathContainer.CONTAINER_PATH_PREFIX)
            .append("_libraries.container");

    // original classpath without our master-library container
    IClasspathEntry[] originalClasspath = project.getRawClasspath();

    Library library = new Library("libraryId");
    List<Library> libraries = new ArrayList<>();
    libraries.add(library);

    BuildPath.addNativeLibrary(project, libraries, monitor);
    Assert.assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);
    assertTrue(project.getProject().exists(librariesIdPath));

    // master-library container exists, so checkLibraryList should make no change
    BuildPath.checkLibraryList(project, null);
    Assert.assertEquals(initialClasspathSize + 1, project.getRawClasspath().length);
    assertTrue(project.getProject().exists(librariesIdPath));

    // remove the master-library container, so checkLibraryList should remove the library ids file
    project.setRawClasspath(originalClasspath, null);
    BuildPath.checkLibraryList(project, null);
    assertFalse(librariesIdPath + " not removed", project.getProject().exists(librariesIdPath));
  }
}
