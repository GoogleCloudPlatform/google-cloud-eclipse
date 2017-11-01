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

package com.google.cloud.tools.eclipse.appengine.newproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.junit.JUnitCore;
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
public abstract class CreateAppEngineProjectTest {

  @Rule public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock protected ILibraryRepositoryService repositoryService;

  protected final IProgressMonitor monitor = new NullProgressMonitor();
  protected final AppEngineProjectConfig config = new AppEngineProjectConfig();
  protected IProject project;

  protected abstract String getMostImportantFilename();
  protected abstract CreateAppEngineProject newCreateAppEngineProject();

  @Before
  public void setUp() throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("testproject" + Math.random());
    config.setProject(project);

    mockRepositoryService();
  }

  @After
  public void tearDown() throws CoreException {
    if (project.exists()) {
      // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1945
      ProjectUtils.waitForProjects(project);
      project.delete(true, monitor);
    }
  }

  private void mockRepositoryService() throws CoreException {
    final LoadingCache<String, Artifact> fakeArtifactStore = CacheBuilder.newBuilder().build(
        new CacheLoader<String, Artifact>() {
          @Override
          public Artifact load(String artifactKey) throws Exception {
            Artifact artifact = mock(Artifact.class);
            File jar = tempFolder.newFile("fake-" + artifactKey + ".jar");
            when(artifact.getFile()).thenReturn(jar);
            return artifact;
          }
        });

    Answer<Artifact> answerFakeArtifact = new Answer<Artifact>() {
      @Override
      public Artifact answer(InvocationOnMock invocation) throws Throwable {
        LibraryFile libraryFile = invocation.getArgumentAt(0, LibraryFile.class);
        MavenCoordinates coordinates = libraryFile.getMavenCoordinates();
        String artifactKey = coordinates.getGroupId() + "-" + coordinates.getArtifactId()
            + "-" + coordinates.getVersion();
        return fakeArtifactStore.get(artifactKey);
      }
    };
    when(repositoryService.resolveArtifact(any(LibraryFile.class), any(IProgressMonitor.class)))
        .thenAnswer(answerFakeArtifact);
  }

  @Test
  public void testMostImportantFile() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    String expectedName = getMostImportantFilename();
    assertEquals(expectedName, creator.getMostImportant().getName());
  }

  @Test
  public void testUnitTestCreated() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);
    ProjectUtils.waitForProjects(project);

    assertJunitAndHamcrestAreOnClasspath();
  }

  private void assertJunitAndHamcrestAreOnClasspath() throws CoreException {
    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    IJavaProject javaProject = JavaCore.create(project);
    IType junit = javaProject.findType("org.junit.Assert");

    // Is findType doing what we think it's doing?
    // Locally where it passes it finds JUnit in
    // class Assert [in Assert.class [in org.junit [in /Users/elharo/workspace/.metadata/.plugins/org.eclipse.pde.core/.bundle_pool/plugins/org.junit_4.12.0.v201504281640/junit.jar]]]

    assertNotNull("Did not find junit", junit);
    assertTrue(junit.exists());
    IType hamcrest = javaProject.findType("org.hamcrest.CoreMatchers");
    assertNotNull("Did not find hamcrest", hamcrest);
    assertTrue(hamcrest.exists());
  }

  @Test
  public void testJavaTestSourceOutput() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    assertCorrectOutputPathForJavaTestSource();
  }

  private void assertCorrectOutputPathForJavaTestSource() throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE
          && containsSegment(entry.getPath(), "test")) {
        assertNotNull(entry.getOutputLocation());
        assertEquals("test-classes", entry.getOutputLocation().lastSegment());
        return;
      }
    }
    fail();
  }

  private boolean containsSegment(IPath path, String segment) {
    return Arrays.asList(path.segments()).contains(segment);
  }

  @Test
  public void testNoTestClassesInDeploymentAssembly() throws InvocationTargetException,
      CoreException, OperationCanceledException, InterruptedException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);
    creator.deployAssemblyEntryRemoveJob.join(180000 /* 3 minutes */, monitor);

    assertFalse(DeployAssemblyEntryRemoveJobTest.hasSourcePathInDeployAssembly(project,
        new Path("src/test/java")));
    assertTrue(DeployAssemblyEntryRemoveJobTest.hasSourcePathInDeployAssembly(project,
        new Path("src/main/java")));
  }

  protected void assertOutputDirectory(String expected) throws JavaModelException {
    assertTrue(project.getFolder(expected).exists());
    IJavaProject javaProject = JavaCore.create(project);
    assertEquals(new Path(expected), javaProject.getOutputLocation().removeFirstSegments(1));
  }

  @Test
  public void testNoJUnit4ClasspathIfUsingMaven() throws InvocationTargetException, CoreException {
    config.setUseMaven("my.group.id", "my-other-artifact-id", "12.34.56");

    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);
    assertFalse(hasJUnit4Classpath(project));
  }

  protected static boolean hasJUnit4Classpath(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      if (entry.getPath().equals(JUnitCore.JUNIT4_CONTAINER_PATH)) {
        return true;
      }
    }
    return false;
  }
}
