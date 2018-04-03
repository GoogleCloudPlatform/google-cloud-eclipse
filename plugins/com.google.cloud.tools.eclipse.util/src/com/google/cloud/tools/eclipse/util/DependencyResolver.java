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

import com.google.cloud.tools.eclipse.util.MavenUtils.ExceptionalCallableWithProgress;
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
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;

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

    ICallable<List<Artifact>> callable =
        new ICallable<List<Artifact>>() {
          @Override
          public List<Artifact> call(IMavenExecutionContext context, IProgressMonitor monitor)
              throws CoreException {
            DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);
            // todo we'd prefer not to depend on m2e here
            RepositorySystem system = MavenPluginActivator.getDefault().getRepositorySystem();

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
              ExceptionalCallableWithProgress<List<Artifact>, RepositoryException>
                  retrieveDependenciesBlock =
                      progress -> {
                        List<ArtifactResult> artifacts =
                            system.resolveDependencies(session, request).getArtifactResults();
                        progress.setWorkRemaining(artifacts.size());
                        List<Artifact> dependencies = new ArrayList<>();
                        for (ArtifactResult result : artifacts) {
                          Artifact dependency = result.getArtifact();
                          dependencies.add(dependency);
                          progress.worked(1);
                        }
                        return dependencies;
                      };
              return MavenUtils.runWithRule(retrieveDependenciesBlock, monitor);
            } catch (RepositoryException ex) {
              throw new CoreException(StatusUtil.error(this, "Could not resolve dependencies", ex));
            } catch (NullPointerException ex) {
              throw new CoreException(
                  StatusUtil.error(
                      this, "Possible corrupt artifact in local .m2 repository for " + coords, ex));
            }
          }
        };

    IMavenExecutionContext context = MavenPlugin.getMaven().createExecutionContext();
    return context.execute(callable, monitor);
  }

  private static List<RemoteRepository> centralRepository(RepositorySystem system) {
    RemoteRepository.Builder builder =
        new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/");
    RemoteRepository repository = builder.build();
    List<RemoteRepository> repositories = new ArrayList<>();
    repositories.add(repository);
    return repositories;
  }

  public static Collection<Dependency> getManagedDependencies(String groupId, String artifactId,
      String version, IProgressMonitor monitor) throws CoreException {

    ICallable<List<Dependency>> callable =
        new ICallable<List<Dependency>>() {
          @Override
          public List<Dependency> call(IMavenExecutionContext context, IProgressMonitor monitor)
              throws CoreException {

            // todo we'd prefer not to depend on m2e here
            RepositorySystem system = MavenPluginActivator.getDefault().getRepositorySystem();

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
              ExceptionalCallableWithProgress<List<Dependency>, RepositoryException>
                  retrieveDependenciesBlock =
                      progress -> {
                        List<Dependency> managedDependencies =
                            system
                                .readArtifactDescriptor(session, request)
                                .getManagedDependencies();
                        return managedDependencies;
                      };
              return MavenUtils.runWithRule(retrieveDependenciesBlock, monitor);
            } catch (RepositoryException ex) {
              IStatus status = StatusUtil.error(DependencyResolver.class, ex.getMessage(), ex);
              throw new CoreException(status);
            }
          }
        };
    // todo we'd prefer not to depend on m2e here
    IMavenExecutionContext context = MavenPlugin.getMaven().createExecutionContext();
    return context.execute(callable, monitor);
  }
}
