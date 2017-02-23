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

package com.google.cloud.tools.eclipse.appengine.facets;

import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntimeType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineStandardFacetTest {
  @Mock private org.eclipse.wst.server.core.IRuntime serverRuntime;
  @Mock private IRuntimeType runtimeType;

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testStandardFacetExists() {
    Assert.assertTrue(
        ProjectFacetsManager.isProjectFacetDefined("com.google.cloud.tools.eclipse.appengine.facets.standard"));
  }

  @Test
  public void testIsAppEngineStandardRuntime_appEngineRuntime() {
    when(runtimeType.getId()).thenReturn(AppEngineStandardFacet.DEFAULT_RUNTIME_ID);
    when(serverRuntime.getRuntimeType()).thenReturn(runtimeType);

    Assert.assertTrue(AppEngineStandardFacet.isAppEngineStandardRuntime(serverRuntime));
  }

  @Test
  public void testIsAppEngineStandardRuntime_nonAppEngineRuntime() {
    when(runtimeType.getId()).thenReturn("some id");
    when(serverRuntime.getRuntimeType()).thenReturn(runtimeType);

    Assert.assertFalse(AppEngineStandardFacet.isAppEngineStandardRuntime(serverRuntime));
  }

  @Test
  public void testFacetLabel() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID);

    Assert.assertEquals("App Engine Java Standard Environment", projectFacet.getLabel());
  }

  @Test
  public void testFindAllWebInfFolders_noWebInfFolders() {
    List<IFolder> webInfFolders =
        AppEngineStandardFacet.findAllWebInfFolders(projectCreator.getProject());
    Assert.assertTrue(webInfFolders.isEmpty());
  }

  @Test
  public void testFindAllWebInfFolders() throws CoreException {
    IProject project = projectCreator.getProject();
    createPath(project, new Path("src/my-webapp/WEB-INF"));

    List<IFolder> webInfFolders =
        AppEngineStandardFacet.findAllWebInfFolders(project);
    Assert.assertEquals(1, webInfFolders.size());
    Assert.assertEquals(project.getFolder("src/my-webapp/WEB-INF"), webInfFolders.get(0));
  }

  @Test
  public void testFindAllWebInfFolders_multipleFolders() throws CoreException {
    IProject project = projectCreator.getProject();
    createPath(project, new Path("webapps/first-webapp/WEB-INF"));
    createPath(project, new Path("webapps/second-webapp/WEB-INF"));
    createPath(project, new Path("third-webapp/WEB-INF"));
    createPath(project, new Path("WEB-INF"));

    List<IFolder> webInfFolders = AppEngineStandardFacet.findAllWebInfFolders(project);
    Assert.assertEquals(4, webInfFolders.size());
    Assert.assertTrue(webInfFolders.contains(project.getFolder("webapps/first-webapp/WEB-INF")));
    Assert.assertTrue(webInfFolders.contains(project.getFolder("webapps/second-webapp/WEB-INF")));
    Assert.assertTrue(webInfFolders.contains(project.getFolder("third-webapp/WEB-INF")));
    Assert.assertTrue(webInfFolders.contains(project.getFolder("WEB-INF")));
  }

  @Test
  public void testFindMainWebAppDirectory_noWebInfFolders() {
    IPath mainWebApp = AppEngineStandardFacet.findMainWebAppDirectory(projectCreator.getProject());
    Assert.assertNull(mainWebApp);
  }

  @Test
  public void testFindMainWebAppDirectory() throws CoreException {
    createPath(projectCreator.getProject(), new Path("webapps/first-webapp/WEB-INF"));
    IPath mainWebApp = AppEngineStandardFacet.findMainWebAppDirectory(projectCreator.getProject());
    Assert.assertEquals(new Path("webapps/first-webapp"), mainWebApp);
  }

  @Test
  public void testFindMainWebAppDirectory_returnsFolderWithWebXml() throws CoreException {
    IProject project = projectCreator.getProject();
    createPath(project, new Path("webapps/first-webapp/WEB-INF"));
    createEmptyFile(project, new Path("webapps/second-webapp/WEB-INF/web.xml"));
    createPath(project, new Path("third-webapp/WEB-INF"));
    createPath(project, new Path("WEB-INF"));

    IPath mainWebApp = AppEngineStandardFacet.findMainWebAppDirectory(project);
    Assert.assertEquals(new Path("webapps/second-webapp"), mainWebApp);
  }

  private static void createPath(IContainer parent, IPath relativePath) throws CoreException {
    if (!relativePath.isEmpty()) {
      String firstSegment = relativePath.segment(0);
      IFolder child = parent.getFolder(new Path(firstSegment));
      if (!child.exists()) {
        child.create(false /* force */, true /* local */, null /* monitor */);
      }
      Assert.assertTrue(child.exists());

      createPath(child, relativePath.removeFirstSegments(1));
    }
  }

  private static void createEmptyFile(IProject project, IPath relativePath) throws CoreException {
    createPath(project, relativePath.removeLastSegments(1));

    InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
    project.getFile(relativePath).create(emptyStream, false /* force */, null /* monitor */);
    Assert.assertTrue(project.getFile(relativePath).exists());
  }
}
