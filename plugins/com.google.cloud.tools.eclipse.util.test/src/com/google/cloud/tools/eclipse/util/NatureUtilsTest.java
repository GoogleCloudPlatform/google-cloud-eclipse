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

import com.google.cloud.tools.eclipse.test.util.ArrayAssertions;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class NatureUtilsTest {

  @Rule public final TestProjectCreator projectCreator = new TestProjectCreator();

  private final IProgressMonitor monitor = new NullProgressMonitor();

  private IProject project;

  @Before
  public void setUp() {
    project = projectCreator.getProject();
  }

  @Test
  public void testAddNature() throws CoreException {
    assertFalse(NatureUtils.hasNature(project, JavaCore.NATURE_ID));
    NatureUtils.addNature(project, JavaCore.NATURE_ID, monitor);
    assertTrue(NatureUtils.hasNature(project, JavaCore.NATURE_ID));
  }

  @Test
  public void testRemoveNature() throws CoreException {
    NatureUtils.addNature(project, JavaCore.NATURE_ID, monitor);
    NatureUtils.removeNature(project, JavaCore.NATURE_ID, monitor);
    assertFalse(project.hasNature(JavaCore.NATURE_ID));
  }

  @Test
  public void testRemoveNature_nonExistingNature() throws CoreException {
    ArrayAssertions.assertIsEmpty(project.getDescription().getNatureIds());

    NatureUtils.removeNature(project, JavaCore.NATURE_ID, monitor);
    ArrayAssertions.assertIsEmpty(project.getDescription().getNatureIds());
  }
}
