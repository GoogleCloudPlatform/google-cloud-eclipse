/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject.standard;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProjectTest;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
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
public class CreateAppEngineStandardWtpProjectTest extends CreateAppEngineWtpProjectTest {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock
  private ILibraryRepositoryService repositoryService;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    mockRepositoryService();
  }

  private void mockRepositoryService() throws IOException, CoreException {
    final Artifact someArtifact = mock(Artifact.class);
    when(someArtifact.getFile()).thenReturn(tempFolder.newFile());

    final Artifact jstl12 = mock(Artifact.class);
    File jstl12Jar = tempFolder.newFile("fake-jstl-1.2.jar");
    when(jstl12.getFile()).thenReturn(jstl12Jar);

    when(repositoryService.resolveArtifact(any(LibraryFile.class), any(IProgressMonitor.class)))
        .thenAnswer(new Answer<Artifact>() {
          @Override
          public Artifact answer(InvocationOnMock invocation) throws Throwable {
            LibraryFile libraryFile = invocation.getArgumentAt(0, LibraryFile.class);
            MavenCoordinates coordinates = libraryFile.getMavenCoordinates();
            if ("jstl".equals(coordinates.getGroupId())
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
    return new CreateAppEngineStandardWtpProject(config, mock(IAdaptable.class), repositoryService);
  }

  @Test
  public void testConstructor() {
    newCreateAppEngineWtpProject();
  }

  @Test
  public void testAppEngineRuntimeAdded() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    IRuntime primaryRuntime = facetedProject.getPrimaryRuntime();
    assertTrue(AppEngineStandardFacet.isAppEngineStandardRuntime(primaryRuntime));
  }

  @Test
  public void testAppEngineLibrariesAdded() throws InvocationTargetException, CoreException {
    Library library = CloudLibraries.getLibrary("appengine-api");
    List<Library> libraries = new ArrayList<>();
    libraries.add(library);
    config.setAppEngineLibraries(libraries);
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    assertAppEngineApiSdkOnClasspath();
  }

  @Test
  public void testJstlAdded() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    assertTrue(project.getFile("src/main/webapp/WEB-INF/lib/fake-jstl-1.2.jar").exists());
  }

  private void assertAppEngineApiSdkOnClasspath() throws CoreException {
    IJavaProject javaProject = JavaCore.create(project);
    Matcher<IClasspathEntry> masterLibraryEntryMatcher =
        new CustomTypeSafeMatcher<IClasspathEntry>("has master container") {
      @Override
      protected boolean matchesSafely(IClasspathEntry entry) {
        return entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && entry.getPath().toString()
            .equals("com.google.cloud.tools.eclipse.appengine.libraries/master-container");
      }};
    Matcher<IClasspathEntry> appEngineSdkMatcher =
        new CustomTypeSafeMatcher<IClasspathEntry>("has appengine-api-1.0-sdk") {
          @Override
          protected boolean matchesSafely(IClasspathEntry entry) {
            return entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY
                && entry.getPath().toString().contains("appengine-api-1.0-sdk");
          }
        };
    assertThat(Arrays.asList(javaProject.getRawClasspath()),
        Matchers.hasItem(masterLibraryEntryMatcher));
    assertThat(Arrays.asList(javaProject.getResolvedClasspath(true)), Matchers.hasItem(appEngineSdkMatcher));
  }

  @Test
  public void testNullConfig() {
    try {
      new CreateAppEngineStandardWtpProject(null, mock(IAdaptable.class),
          mock(ILibraryRepositoryService.class));
      Assert.fail("allowed null config");
    } catch (NullPointerException ex) {
      // success
    }
  }
}
