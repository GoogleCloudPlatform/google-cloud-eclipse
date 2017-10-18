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

package com.google.cloud.tools.eclipse.appengine.compat.gpe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.compat.gpe.GpeMigrator;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.io.IOException;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Migrates a classic GPE project (no facets). */
public class GpeClassicMigratorTest {

  private final IProgressMonitor monitor = new NullProgressMonitor();

  private IProject gpeProject;

  @Before
  public void setUp() throws IOException, CoreException {
    List<IProject> projects = ProjectUtils.importProjects(getClass(),
        "test-projects/GPE-classic-project.zip", false /* checkBuildErrors */, monitor);
    assertEquals(1, projects.size());
    gpeProject = projects.get(0);
  }

  @After
  public void tearDown() throws CoreException {
    gpeProject.delete(true /* force */,  monitor);
  }

  @Test
  public void testRemoveGpeNature() throws CoreException {
    assertTrue(gpeProject.hasNature("com.google.appengine.eclipse.core.gaeNature"));

    assertTrue(GpeMigrator.removeGpeNature(gpeProject, null));
    assertFalse(gpeProject.hasNature("com.google.appengine.eclipse.core.gaeNature"));

    assertFalse(GpeMigrator.removeGpeNature(gpeProject, monitor));
  }

  @Test
  public void testRemoveGpeClasspathEntries() throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(gpeProject);

    assertTrue(containsLibrary(javaProject, "com.google.appengine.eclipse.core.GAE_CONTAINER"));
    assertTrue(containsLibrary(javaProject, "com.google.gwt.eclipse.core.GWT_CONTAINER"));

    assertTrue(GpeMigrator.removeGpeClasspathEntries(gpeProject, monitor));
    assertFalse(containsLibrary(javaProject, "com.google.appengine.eclipse.core.GAE_CONTAINER"));
    assertFalse(containsLibrary(javaProject, "com.google.gwt.eclipse.core.GWT_CONTAINER"));

    assertFalse(GpeMigrator.removeGpeClasspathEntries(gpeProject, monitor));
  }

  private static boolean containsLibrary(IJavaProject javaProject, String libraryPath)
      throws JavaModelException {
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      if (entry.getPath().equals(new Path(libraryPath))) {
        return true;
      }
    }
    return false;
  }
}
