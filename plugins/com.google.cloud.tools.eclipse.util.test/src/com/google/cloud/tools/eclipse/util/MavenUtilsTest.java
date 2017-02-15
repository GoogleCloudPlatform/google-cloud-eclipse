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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class MavenUtilsTest {
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
  public void testAreDependenciesEqual_nullDependencies() {
    Assert.assertFalse(MavenUtils.areDependenciesEqual(null, null));
    Assert.assertFalse(MavenUtils.areDependenciesEqual(new Dependency(), null));
    Assert.assertFalse(MavenUtils.areDependenciesEqual(null, new Dependency()));
  }

  @Test
  public void testAreDependenciesEqual_equalDependencies() {
    Dependency dependency1 = new Dependency();
    Dependency dependency2 = new Dependency();
    Dependency dependency3 = new Dependency();
    dependency3.setGroupId("groupId");
    dependency3.setArtifactId("artifactId");
    Dependency dependency4 = new Dependency();
    dependency4.setGroupId("groupId");
    dependency4.setArtifactId("artifactId");

    Assert.assertTrue(MavenUtils.areDependenciesEqual(dependency1, dependency2));
    Assert.assertTrue(MavenUtils.areDependenciesEqual(dependency3, dependency4));
  }

  @Test
  public void testAreDependenciesEqual_unEqualDependencies() {
    Dependency dependency1 = new Dependency();
    Dependency dependency2 = new Dependency();
    dependency2.setGroupId("groupId1");
    dependency2.setArtifactId("artifactId1");
    Dependency dependency3 = new Dependency();
    dependency3.setGroupId("groupId2");
    dependency3.setArtifactId("artifactId2");

    Assert.assertFalse(MavenUtils.areDependenciesEqual(dependency1, dependency2));
    Assert.assertFalse(MavenUtils.areDependenciesEqual(dependency2, dependency3));
  }

  @Test
  public void testDoesListContainDependency_existingDependency() {
    Dependency dependency1 = new Dependency();
    dependency1.setGroupId("groupId");
    dependency1.setArtifactId("artifactId");
    List<Dependency> dependencies = new ArrayList<Dependency>();
    dependencies.add(dependency1);

    Dependency dependency2 = new Dependency();
    dependency2.setGroupId("groupId");
    dependency2.setArtifactId("artifactId");

    Assert.assertTrue(MavenUtils.doesListContainDependency(dependencies, dependency2));
  }

  @Test
  public void testDoesListContainDependency_nonExistingDependency() {
    List<Dependency> dependencies1 = new ArrayList<Dependency>();
    List<Dependency> dependencies2 = new ArrayList<Dependency>();
    Dependency dependency1 = new Dependency();
    Dependency dependency2 = new Dependency();
    dependency2.setGroupId("groupId2");
    dependency2.setArtifactId("artifactId2");
    Dependency dependency3 = new Dependency();
    dependency3.setGroupId("groupId3");
    dependency3.setArtifactId("artifactId3");
    dependencies2.add(dependency2);

    Assert.assertFalse(MavenUtils.doesListContainDependency(null, null));
    Assert.assertFalse(MavenUtils.doesListContainDependency(dependencies1, null));
    Assert.assertFalse(MavenUtils.doesListContainDependency(dependencies1, dependency1));
    Assert.assertFalse(MavenUtils.doesListContainDependency(dependencies1, dependency2));
    Assert.assertFalse(MavenUtils.doesListContainDependency(dependencies2, dependency3));
  }
}
