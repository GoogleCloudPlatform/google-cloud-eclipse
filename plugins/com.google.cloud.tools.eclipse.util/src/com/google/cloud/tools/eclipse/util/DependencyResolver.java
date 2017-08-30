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
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;

public class DependencyResolver {

  /**
   * Returns all transitive runtime dependencies of the specified Maven artifact
   * including the artifact itself.
   * 
   * @param groupId group ID of the Maven artifact to resolve
   * @param artifactId artifact ID of the Maven artifact to resolve
   * @param version version of the Maven artifact to resolve
   * @return a list of strings in the form groupId:artifactId:version
   * @throws CoreException if the dependencies could not be resolved
   */
  public static List<String> getTransitiveDependencies(
      String groupId, String artifactId, String version, IProgressMonitor monitor)
          throws CoreException {
       
    final Artifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + version);

    IMavenExecutionContext context = MavenPlugin.getMaven().createExecutionContext();
    
    ICallable<List<String>> callable = new ICallable<List<String>>() {
      @Override
      public List<String> call(IMavenExecutionContext context, IProgressMonitor monitor)
          throws CoreException {
        List<String> dependencies = new ArrayList<>();
        DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);
        // todo we'd prefer not to depend on m2e here
        RepositorySystem system = MavenPluginActivator.getDefault().getRepositorySystem();
        
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.RUNTIME));
        collectRequest.setRepositories(centralRepository(system));
        final DependencyRequest request = new DependencyRequest(collectRequest, filter);
        RepositorySystemSession session = context.getRepositorySession();

        try {
          List<ArtifactResult> artifacts =
              system.resolveDependencies(session, request).getArtifactResults();
          for (ArtifactResult result : artifacts) {
            Artifact dependency = result.getArtifact();
            dependencies.add(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":"
                + dependency.getVersion());
          }
          return dependencies;
        } catch (DependencyResolutionException ex) {
          throw new CoreException(StatusUtil.error(ex, "Could not resolve dependencies"));
        } catch (NullPointerException ex) {
          throw new CoreException(StatusUtil.error(ex,
              "Possible corrupt artifact in local .m2 repository for " + artifact));
        }
      }
      
    };
    List<String> x = context.execute(callable, monitor);
    return x;
  }

  private static List<RemoteRepository> centralRepository(RepositorySystem system) {
    RemoteRepository.Builder builder =
        new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/");
    RemoteRepository repository = builder.build();
    List<RemoteRepository> repositories = new ArrayList<>();
    repositories.add(repository);
    return repositories;
  }
}