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

import static org.junit.Assert.assertArrayEquals;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Rule;
import org.junit.Test;

public class NatureUtilsTest {

  // FacetedProjectNature.NATURE_ID = FacetCorePlugin.PLUGIN_ID + ".nature"
  private static final String FACETED_NATURE_ID =
      "org.eclipse.wst.common.project.facet.core.nature";
  @Rule public final TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testRemoveNature() throws CoreException {
    IProject project = projectCreator.getProject();
    // By default, project has faceted project nature.
    assertArrayEquals(new String[] {FACETED_NATURE_ID}, project.getDescription().getNatureIds());

    NatureUtils.removeNature(project, FACETED_NATURE_ID);
    assertArrayEquals(new String[] {}, project.getDescription().getNatureIds());
  }

  @Test
  public void testRemoveNature_nonExistingNature() throws CoreException {
    IProject project = projectCreator.getProject();
    NatureUtils.removeNature(project, JavaCore.NATURE_ID);
    assertArrayEquals(new String[] {FACETED_NATURE_ID}, project.getDescription().getNatureIds());
  }
}
