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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class CreateAppEngineFlexWtpProjectTest {

  @Rule public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private ILibraryRepositoryService repositoryService;

  private final IProgressMonitor monitor = new NullProgressMonitor();
  private final AppEngineProjectConfig config = new AppEngineProjectConfig();
  private IProject project;

  @Before
  public void setUp() throws IOException, CoreException {
    mockRepositoryService();

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("testproject" + Math.random());
    config.setProject(project);
  }

  private void mockRepositoryService() throws IOException, CoreException {
    final Artifact someArtifact = mock(Artifact.class);
    when(someArtifact.getFile()).thenReturn(tempFolder.newFile());

    final Artifact servletApi31 = mock(Artifact.class);
    File servletApi31Jar = tempFolder.newFile("fake-servlet-api-3.1.jar");
    when(servletApi31.getFile()).thenReturn(servletApi31Jar);

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
  }

  @After
  public void tearDown() throws CoreException {
    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1945
    ProjectUtils.waitForProjects(project);
    project.delete(true, monitor);
  }

  @Test
  public void testServletApi31Added() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator =
        new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
    creator.execute(monitor);

    assertTrue(project.getFile("lib/fake-servlet-api-3.1.jar").exists());
  }

  @Test
  public void testNonDependencyAttributeOnJarsInLib()
      throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator =
        new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
    creator.execute(monitor);

    File lib = project.getFolder("lib").getLocation().toFile();
    for (File jar : lib.listFiles()) {
      assertTrue(hasNonDependencyAttribute(jar));
    }
  }

  private boolean hasNonDependencyAttribute(File jar) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      if (entry.getPath().toFile().equals(jar)) {
        for (IClasspathAttribute attribute : entry.getExtraAttributes()) {
          if (isNonDependencyAttribute(attribute)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean isNonDependencyAttribute(IClasspathAttribute attribute) {
    return IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY
        .equals(attribute.getName());
  }

  @Test
  public void testDynamicWebModuleFacet31Added() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator =
        new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
    creator.execute(monitor);

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertTrue(facetedProject.hasProjectFacet(WebFacetUtils.WEB_31));
  }

  @Test
  public void testNoMavenNatureByDefault() throws InvocationTargetException, CoreException {
    assertFalse(config.getUseMaven());
    CreateAppEngineWtpProject creator =
        new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
    creator.execute(monitor);

    assertFalse(project.hasNature(MavenUtils.MAVEN2_NATURE_ID));
    assertTrue(project.getFolder("build").exists());
    assertOutputDirectory("build/classes");
  }

  @Test
  public void testMavenNatureEnabled() throws InvocationTargetException, CoreException {
    config.setUseMaven("my.group.id", "my-artifact-id", "12.34.56");

    CreateAppEngineWtpProject creator =
        new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
    creator.execute(monitor);
    ProjectUtils.waitForProjects(project);

    assertTrue(project.hasNature(MavenUtils.MAVEN2_NATURE_ID));
    assertFalse(project.getFolder("build").exists());
    assertOutputDirectory("target/my-artifact-id-12.34.56/WEB-INF/classes");
  }

  private void assertOutputDirectory(String expected) throws JavaModelException {
    assertTrue(project.getFolder(expected).exists());
    IJavaProject javaProject = JavaCore.create(project);
    assertEquals(new Path(expected), javaProject.getOutputLocation().removeFirstSegments(1));
  }
}
