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

package com.google.cloud.tools.eclipse.util;

import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class MavenUtilsTest {
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testMavenNature_mavenProject() throws CoreException {
    IProject project = Mockito.mock(IProject.class);
    Mockito.when(project.hasNature("org.eclipse.m2e.core.maven2Nature")).thenReturn(true);
    Mockito.when(project.isAccessible()).thenReturn(true);

    Assert.assertTrue(MavenUtils.hasMavenNature(project));
  }

  @Test
  public void testMavenNature_nonMavenProject() {
    IProject project = Mockito.mock(IProject.class);
    Mockito.when(project.isAccessible()).thenReturn(true);

    Assert.assertFalse(MavenUtils.hasMavenNature(project));
  }

  @Test
  public void testMavenResovingRule() {
    assertEquals(
        MavenPlugin.getProjectConfigurationManager().getRule(), MavenUtils.mavenResolvingRule());
  }
}
