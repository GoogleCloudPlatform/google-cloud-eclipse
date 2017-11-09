/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.util.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for the utility methods on {@link ResourceUtils}.
 */
public class ResourceUtilsTest {
  private IProject project;

  @After
  public void tearDown() throws CoreException {
    if (project != null) {
      project.delete(true, null);
    }
  }

  @Test
  public void testCreateFolders() throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    project = root.getProject("test-project");
    project.create(null);
    project.open(null);
    assertTrue(project.exists());

    IFolder folder = project.getFolder(new Path("foo/bar/baz"));
    assertFalse(folder.exists());
    ResourceUtils.createFolders(folder, null);
    assertTrue(folder.exists());
  }
}
