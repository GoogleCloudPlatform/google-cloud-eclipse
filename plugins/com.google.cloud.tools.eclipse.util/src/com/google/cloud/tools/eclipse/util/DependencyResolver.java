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

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;

public class DependencyResolver {

  /**
   * Returns all transitive runtime dependencies of the specified Maven jar artifact including the
   * artifact itself.
   *
   * @param groupId group ID of the Maven artifact to resolve
   * @param artifactId artifact ID of the Maven artifact to resolve
   * @param version version of the Maven artifact to resolve
   * @return artifacts in the transitive dependency graph. Order not guaranteed.
   * @throws CoreException if the dependencies could not be resolved
   */
  public static Collection<Artifact> getTransitiveDependencies(
      String groupId, String artifactId, String version, IProgressMonitor monitor)
      throws CoreException {
    return MavenUtils.runOperation(
        monitor,
        (context, system, progress) ->
            _getTransitiveDependencies(context, system, groupId, artifactId, version, progress));
  }

  private static Collection<Artifact> _getTransitiveDependencies(
      IMavenExecutionContext context,
      RepositorySystem system,
      String groupId,
      String artifactId,
      String version,
      IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor);
    DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);
    
    // todo we'd prefer not to depend on m2e here

    String coords = groupId + ":" + artifactId + ":" + version;
    Artifact artifact = new DefaultArtifact(coords);
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, JavaScopes.RUNTIME));
    collectRequest.setRepositories(centralRepository(system));
    DependencyRequest request = new DependencyRequest(collectRequest, filter);
    
    // ensure checksum errors result in failure
    DefaultRepositorySystemSession session =
        new DefaultRepositorySystemSession(context.getRepositorySession());
    session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);

    try {
      DependencyResult resolved = system.resolveDependencies(session, request);
      List<ArtifactResult> artifacts = resolved.getArtifactResults();
      progress.setWorkRemaining(artifacts.size());
      List<Artifact> dependencies = new ArrayList<>();
      for (ArtifactResult result : artifacts) {
        Artifact dependency = result.getArtifact();
        dependencies.add(dependency);
        progress.worked(1);
      }
      return dependencies;
    } catch (RepositoryException ex) {
      throw new CoreException(
          StatusUtil.error(DependencyResolver.class, "Could not resolve dependencies", ex));
    } catch (NullPointerException ex) {
      throw new CoreException(
          StatusUtil.error(
              DependencyResolver.class,
              "Possible corrupt artifact in local .m2 repository for " + coords,
              ex));
    }
  }

  private static List<RemoteRepository> centralRepository(RepositorySystem system) {
    RemoteRepository.Builder builder =
        new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/");
    RemoteRepository repository = builder.build();
    List<RemoteRepository> repositories = new ArrayList<>();
    repositories.add(repository);
    return repositories;
  }

  public static Collection<Dependency> getManagedDependencies(
      String groupId, String artifactId, String version, IProgressMonitor monitor)
      throws CoreException {

    return MavenUtils.runOperation(
        monitor,
        (context, system, progress) ->
            _getManagedDependencies(context, system, groupId, artifactId, version, progress));
  }

  private static Collection<Dependency> _getManagedDependencies(
      IMavenExecutionContext context,
      RepositorySystem system,
      String groupId,
      String artifactId,
      String version,
      SubMonitor monitor)
      throws CoreException {
    ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
    String coords = groupId + ":" + artifactId + ":" + version;
    Artifact artifact = new DefaultArtifact(coords);
    request.setArtifact(artifact);
    request.setRepositories(centralRepository(system));

    // ensure checksum errors result in failure
    DefaultRepositorySystemSession session =
        new DefaultRepositorySystemSession(context.getRepositorySession());
    session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);

    try {
      List<Dependency> managedDependencies =
          system.readArtifactDescriptor(session, request).getManagedDependencies();
      
      return managedDependencies;
    } catch (RepositoryException ex) {
      IStatus status = StatusUtil.error(DependencyResolver.class, ex.getMessage(), ex);
      throw new CoreException(status);
    }
  }
}
