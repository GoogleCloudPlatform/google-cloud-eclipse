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
import org.junit.Assert;
import org.junit.Test;

public class DependencyResolverTest {

  @Test
  public void testStorage() throws CoreException {
    List<String> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.google.cloud", "google-cloud-storage", "1.4.0");
    Assert.assertTrue(dependencies.contains("com.google.cloud:google-cloud-storage:1.4.0"));
  }

  @Test
  public void testDatastore() throws CoreException {
    List<String> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.google.cloud", "google-cloud-datastore", "1.4.0");
    Assert.assertTrue(dependencies.contains("com.google.cloud:google-cloud-datastore:1.4.0"));
    for (String dep : dependencies) {
      System.out.println("Datastore" + " depends on " + dep);
    }
  }

  @Test
  public void testGuava() throws CoreException {
    List<String> dependencies = DependencyResolver.getTransitiveDependencies(
        "com.google.guava", "guava", "19.0");
    Assert.assertTrue(dependencies.contains("com.google.guava:guava:19.0"));
    for (String dep : dependencies) {
      System.out.println("Guava" + " depends on " + dep);
    }
  }

}
