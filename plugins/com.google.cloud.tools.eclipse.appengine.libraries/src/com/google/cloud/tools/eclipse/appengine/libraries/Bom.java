/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.cloud.tools.eclipse.util.DependencyResolver;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A cache of a pom.xml file and all its transitive dependencies.
 */
class Bom {
  
  private Map<String, Artifact> artifacts;

  private Bom(Map<String, Artifact> artifacts) {
    this.artifacts = artifacts;
  }

  static Bom loadBom(String groupId, String artifactId, String version, IProgressMonitor monitor)
      throws CoreException {
    
    Collection<Dependency> dependencies =
        DependencyResolver.getManagedDependencies(groupId, artifactId, version, monitor);
    Map<String, Artifact> artifacts = new HashMap<>();
    for (Dependency dependency : dependencies) {
      Artifact artifact = dependency.getArtifact();
      artifacts.put(artifact.getGroupId() + ":" + artifact.getArtifactId(), artifact);

    }
    
    return new Bom(artifacts);
  }
  
  boolean defines(String groupId, String artifactId) {
    return artifacts.containsKey(groupId + ":" + artifactId);
  }

}
