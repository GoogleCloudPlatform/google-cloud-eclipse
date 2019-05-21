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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Test;

public class DependencyResolverTest {

  private final NullProgressMonitor monitor = new NullProgressMonitor();

  @Test
  public void testGetManagedDependencies() throws CoreException {
    Collection<Dependency> dependencies = DependencyResolver.getManagedDependencies(
        "com.google.cloud", "google-cloud-bom", "0.42.0-alpha", null);
    Assert.assertFalse(dependencies.isEmpty());
  }
  
  @Test
  public void testStorage() throws CoreException {
    Collection<Artifact> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.google.cloud", "google-cloud-storage", "1.4.0", monitor);
    Collection<String> actual = getMavenCoordinates(dependencies);
    Assert.assertTrue(actual.contains("com.google.cloud:google-cloud-storage:1.4.0"));
    Assert.assertFalse(actual.contains("io.grpc:grpc-protobuf:1.4.0"));
    Assert.assertTrue(actual.contains("com.fasterxml.jackson.core:jackson-core:2.1.3"));
  }

  @Test
  public void testBadDependency() {
    try {
      DependencyResolver.getTransitiveDependencies(
          "com.google.cloud", "google-cloud-nonesuch", "1.4.0", monitor);
      Assert.fail();
    } catch (CoreException ex) {
      Assert.assertEquals(IStatus.ERROR, ex.getStatus().getSeverity());
    }
  }

  @Test
  public void testDatastore() throws CoreException {
    Collection<Artifact> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.google.cloud", "google-cloud-datastore", "1.4.0", monitor);
    Collection<String> actual = getMavenCoordinates(dependencies);
    Assert.assertTrue(actual.contains("com.google.cloud:google-cloud-datastore:1.4.0"));
    Assert.assertTrue(actual.contains("io.grpc:grpc-protobuf:1.4.0"));
    Assert.assertTrue(actual.contains("com.fasterxml.jackson.core:jackson-core:2.1.3"));
  }

  @Test
  public void testOptionalDependenciesIncluded() throws CoreException {
    Collection<Artifact> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.googlecode.objectify", "objectify", "5.1.22", monitor);
    Collection<String> actual = getMavenCoordinates(dependencies);
    Assert.assertTrue(actual.contains("org.joda:joda-money:0.10.0"));
  }
  
  @Test
  public void testObjectify6() throws CoreException {
    Collection<Artifact> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.googlecode.objectify", "objectify", "6.0.3", monitor);
    String result = "";
    for (Artifact artifact : dependencies) {
      if ("com.fasterxml.jackson.core".equals(artifact.getGroupId())
          && "jackson-core".equals(artifact.getArtifactId())) {
        return;
      }
      result += artifact.toString() + "\n";
    }
    Assert.fail("Jackson missing but contained: \n" + result);
  }

  /**
   * Easier to check for inclusion by Maven coordinates.
   */
  private static Collection<String> getMavenCoordinates(Collection<Artifact> artifacts) {
    Set<String> actual = new HashSet<>();
    for (Artifact artifact : artifacts) {
      actual.add(
          artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
    }
    return actual;
  }
}
