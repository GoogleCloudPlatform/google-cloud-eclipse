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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.resolution.ArtifactResult;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;

import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
// import org.eclipse.aether.transport.file.FileTransporterFactory;
// import org.eclipse.aether.transport.http.HttpTransporterFactory;


public class DependencyResolver {

  public static List<String> getTransitiveDependencies(final String groupId,
      final String artifactId, final String version)
      throws DependencyResolutionException, CoreException {

    IMavenExecutionContext context = MavenPlugin.getMaven().createExecutionContext();
    
    ICallable<List<String>> callable = new ICallable<List<String>>() {
      @Override
      public List<String> call(IMavenExecutionContext context, IProgressMonitor monitor)
          throws CoreException {
        DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        Artifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + version);

        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
        final RepositorySystem system = newRepositorySystem();
        // RepositorySystem system = MavenPluginActivator.getDefault().getRepositorySystem();
        collectRequest.setRepositories(newRepositories(system));
        final DependencyRequest request = new DependencyRequest(collectRequest, filter);
        List<String> dependencies = new ArrayList<>();
        RepositorySystemSession session = context.getRepositorySession();
        try {
          List<ArtifactResult> artifacts =
              system.resolveDependencies(session, request).getArtifactResults();
          for (ArtifactResult result : artifacts) {
            dependencies.add(result.toString());
          }
          return dependencies;
        } catch (DependencyResolutionException ex) {
          throw new CoreException(StatusUtil.error(ex, "Ooops"));
        }
      }
      
    };
    List<String> x = context.execute(callable, new NullProgressMonitor());
    return x;
    // RepositorySystemSession session = newRepositorySystemSession(system);    


  }

  private static RepositorySystem newRepositorySystem() {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    // locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    // locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
     locator.addService(TransporterFactory.class, WagonTransporterFactory.class);

    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable ex) {
        ex.printStackTrace();
      }
    });

    return locator.getService(RepositorySystem.class);
  }

   private static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
      DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    LocalRepository localRepository = new LocalRepository("target/local-repo");
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
/*
      session.setTransferListener(new ConsoleTransferListener() );
      session.setRepositoryListener( new ConsoleRepositoryListener() );
*/
      
      return session;
  }

  private static List<RemoteRepository> newRepositories(RepositorySystem system) {
    RemoteRepository.Builder builder =
        new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/");
    RemoteRepository repository = builder.build();
    List<RemoteRepository> repositories = new ArrayList<>();
    repositories.add(repository);
    return repositories;
  }
}
