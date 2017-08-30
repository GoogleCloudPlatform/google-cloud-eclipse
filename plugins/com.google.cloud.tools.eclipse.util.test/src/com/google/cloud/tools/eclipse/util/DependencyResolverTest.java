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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Test;

public class DependencyResolverTest {
  
  private NullProgressMonitor monitor = new NullProgressMonitor();

  @Test
  public void testStorage() throws CoreException {
    List<String> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.google.cloud", "google-cloud-storage", "1.4.0", monitor);
    Assert.assertTrue(dependencies.contains("com.google.cloud:google-cloud-storage:1.4.0"));
    Assert.assertFalse(dependencies.contains("io.grpc:grpc-protobuf:1.4.0"));
    Assert.assertTrue(dependencies.contains("com.fasterxml.jackson.core:jackson-core:2.1.3"));
  }
  
  @Test
  public void testBadDependency() throws CoreException {
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
    List<String> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.google.cloud", "google-cloud-datastore", "1.4.0", monitor);
    Assert.assertTrue(dependencies.contains("com.google.cloud:google-cloud-datastore:1.4.0"));
    Assert.assertTrue(dependencies.contains("io.grpc:grpc-protobuf:1.4.0"));
    Assert.assertTrue(dependencies.contains("com.fasterxml.jackson.core:jackson-core:2.1.3"));
  }

  @Test
  public void testGuava() throws CoreException {
    List<String> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.google.guava", "guava", "19.0", monitor);
    Assert.assertTrue(dependencies.contains("com.google.guava:guava:19.0"));
  }

}
