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

package com.google.cloud.tools.eclipse.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.common.collect.Lists;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ClasspathUtilTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator()
      .withFacetVersions(JavaFacet.VERSION_1_7);

  private final IProgressMonitor monitor = new NullProgressMonitor();

  private IProject project;
  private IJavaProject javaProject;

  @Before
  public void setUp() {
    project = projectCreator.getProject();
    javaProject = projectCreator.getJavaProject();
  }

  @Test
  public void testAddClasspathEntry() throws JavaModelException {
    IClasspathEntry entry = JavaCore.newSourceEntry(new Path("/my/src"));
    assertFalse(classpathExists(entry));
    ClasspathUtil.addClasspathEntry(project, entry, monitor);
    assertTrue(classpathExists(entry));
  }

  @Test
  public void testAddClasspathEntries() throws JavaModelException {
    IClasspathEntry entry1 = JavaCore.newSourceEntry(new Path("/my/src"));
    IClasspathEntry entry2 = JavaCore.newContainerEntry(new Path("/my/container"));
    List<IClasspathEntry> toAdd = Lists.newArrayList(entry1, entry2);

    assertFalse(classpathExists(entry1));
    assertFalse(classpathExists(entry2));
    ClasspathUtil.addClasspathEntries(project, toAdd, monitor);
    assertTrue(classpathExists(entry1));
    assertTrue(classpathExists(entry2));
  }

  private boolean classpathExists(IClasspathEntry entry) throws JavaModelException {
    List<IClasspathEntry> entries = Lists.newArrayList(javaProject.getRawClasspath());
    return entries.contains(entry);
  }
}
