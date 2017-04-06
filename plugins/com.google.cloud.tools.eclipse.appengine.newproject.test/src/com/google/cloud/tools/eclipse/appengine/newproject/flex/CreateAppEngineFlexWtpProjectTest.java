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

package com.google.cloud.tools.eclipse.appengine.newproject.flex;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CreateAppEngineFlexWtpProjectTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final IProgressMonitor monitor = new NullProgressMonitor();
  private final AppEngineProjectConfig config = new AppEngineProjectConfig();
  private IProject project;

  @Before
  public void setUp() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("testproject" + Math.random());
    config.setProject(project);
  }

  @After
  public void tearDown() throws CoreException {
    project.delete(true, monitor);
  }

  @Test
  public void testServletApi31Added() throws InvocationTargetException, CoreException, IOException {
    final Artifact servletApi31 = mock(Artifact.class);
    File servletApi31Jar = tempFolder.newFile();
    when(servletApi31.getFile()).thenReturn(servletApi31Jar);

    final Artifact someArtifact = mock(Artifact.class);
    when(someArtifact.getFile()).thenReturn(tempFolder.newFile());

    ILibraryRepositoryService repositoryService = mock(ILibraryRepositoryService.class);
    when(repositoryService.resolveArtifact(any(LibraryFile.class), any(IProgressMonitor.class)))
        .thenAnswer(new Answer<Artifact>() {
          @Override
          public Artifact answer(InvocationOnMock invocation) throws Throwable {
            LibraryFile libraryFile = invocation.getArgumentAt(0, LibraryFile.class);
            MavenCoordinates coordinates = libraryFile.getMavenCoordinates();

            if ("javax.servlet".equals(coordinates.getGroupId())
                && "javax.servlet-api".equals(coordinates.getArtifactId())
                && "3.1.0".equals(coordinates.getVersion())) {
              return servletApi31;
            } else {
              return someArtifact;
            }
          }
        });

    CreateAppEngineWtpProject creator =
        new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
    creator.execute(monitor);

    assertTrue(project.getFile("lib/" + servletApi31Jar.getName()).exists());
  }

}
