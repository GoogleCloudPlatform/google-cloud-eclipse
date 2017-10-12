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
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProjectTest;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
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
public class CreateAppEngineFlexWtpProjectTest extends CreateAppEngineWtpProjectTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private ILibraryRepositoryService repositoryService;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    mockRepositoryService();
  }

  private void mockRepositoryService() throws IOException, CoreException {
    final Artifact someArtifact = mock(Artifact.class);
    when(someArtifact.getFile()).thenReturn(tempFolder.newFile());

    final Artifact servletApi31 = mock(Artifact.class);
    File servletApi31Jar = tempFolder.newFile("fake-servlet-api-3.1.jar");
    when(servletApi31.getFile()).thenReturn(servletApi31Jar);

    final Artifact jspApi231 = mock(Artifact.class);
    File jspApi231Jar = tempFolder.newFile("fake-jsp-api-2.3.1.jar");
    when(jspApi231.getFile()).thenReturn(jspApi231Jar);

    final Artifact jstl12 = mock(Artifact.class);
    File jstl12Jar = tempFolder.newFile("fake-jstl-1.2.jar");
    when(jstl12.getFile()).thenReturn(jstl12Jar);

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
            } else if ("javax.servlet.jsp".equals(coordinates.getGroupId())
                && "javax.servlet.jsp-api".equals(coordinates.getArtifactId())
                && "2.3.1".equals(coordinates.getVersion())) {
              return jspApi231;
            } else if ("jstl".equals(coordinates.getGroupId())
                && "jstl".equals(coordinates.getArtifactId())
                && "1.2".equals(coordinates.getVersion())) {
              return jstl12;
            } else {
              return someArtifact;
            }
          }
        });
  }

  @Override
  protected CreateAppEngineWtpProject newCreateAppEngineWtpProject() {
    return new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
  }

  @Test
  public void testServletApi31Added() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    assertTrue(project.getFile("lib/fake-servlet-api-3.1.jar").exists());
  }

  @Test
  public void testJspApi231Added() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    assertTrue(project.getFile("lib/fake-jsp-api-2.3.1.jar").exists());
  }

  @Test
  public void testJstl12Added() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    assertTrue(project.getFile("src/main/webapp/WEB-INF/lib/fake-jstl-1.2.jar").exists());
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
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertTrue(facetedProject.hasProjectFacet(WebFacetUtils.WEB_31));
  }

}
