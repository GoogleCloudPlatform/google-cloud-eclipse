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

package com.google.cloud.tools.eclipse.appengine.facets;

import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for {@code FacetUtil}
 */
@RunWith(MockitoJUnitRunner.class)
public class FacetUtilTest {
  @Mock private IFacetedProject mockFacetedProject;
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();
  private Set<IFacetedProject.Action> facetInstallSet = new HashSet<>();

  @Test
  public void testAddJavaFacetToBatch_facetDoesNotExitsInProject() throws CoreException
  {
    when(mockFacetedProject.hasProjectFacet(JavaFacet.VERSION_1_7)).thenReturn(false);
    when(mockFacetedProject.getProject()).thenReturn(projectCreator.getProject());
    
    Assert.assertTrue(FacetUtil.addJavaFacetToBatch(
        JavaFacet.VERSION_1_7, mockFacetedProject, facetInstallSet));
  }

  @Test
  public void testAddJavaFacetToBatch_facetExitsInProject() throws CoreException
  {
    when(mockFacetedProject.hasProjectFacet(JavaFacet.VERSION_1_7)).thenReturn(true);
    
    Assert.assertFalse(FacetUtil.addJavaFacetToBatch(
        JavaFacet.VERSION_1_7, mockFacetedProject, facetInstallSet));
  }

  @Test(expected = CoreException.class)
  public void testAddJavaFacetToBatch_nonJavaFacet() throws CoreException
  {
    FacetUtil.addJavaFacetToBatch(WebFacetUtils.WEB_25, mockFacetedProject, facetInstallSet);
  }

  @Test
  public void testAddWebFacetToBatch_facetDoesNotExitsInProject() throws CoreException
  {
    when(mockFacetedProject.hasProjectFacet(WebFacetUtils.WEB_25)).thenReturn(false);
    when(mockFacetedProject.getProject()).thenReturn(projectCreator.getProject());
    
    Assert.assertTrue(FacetUtil.addWebFacetToBatch(
        WebFacetUtils.WEB_25, mockFacetedProject, facetInstallSet));
  }

  @Test
  public void testAddWebFacetToBatch_facetExitsInProject() throws CoreException
  {
    when(mockFacetedProject.hasProjectFacet(WebFacetUtils.WEB_25)).thenReturn(true);
    
    Assert.assertFalse(FacetUtil.addWebFacetToBatch(
        WebFacetUtils.WEB_25, mockFacetedProject, facetInstallSet));
  }

  @Test(expected = CoreException.class)
  public void testAddWebFacetToBatch_nonWebFacet() throws CoreException
  {
    FacetUtil.addWebFacetToBatch(JavaFacet.VERSION_1_7, mockFacetedProject, facetInstallSet);
  }

  @Test
  public void testAddFacetSetToProject_emptyFacetSet() throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(projectCreator.getProject());
    FacetUtil.addFacetSetToProject(facetedProject, facetInstallSet, null);
    Set<IProjectFacetVersion> listOfFacets = facetedProject.getProjectFacets();
    Assert.assertEquals(0, listOfFacets.size());
  }

  @Test
  public void testAddFacetSetToProject_nonEmptyfacetSet() throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(projectCreator.getProject());
    FacetUtil.addJavaFacetToBatch(JavaFacet.VERSION_1_7, facetedProject, facetInstallSet);
    FacetUtil.addFacetSetToProject(facetedProject, facetInstallSet, null);
    Set<IProjectFacetVersion> listOfFacets = facetedProject.getProjectFacets();
    Assert.assertEquals(1, listOfFacets.size());
    Assert.assertEquals(JavaFacet.VERSION_1_7, listOfFacets.iterator().next());
  }

  @Test
  public void testFindAllWebInfFolders_noWebInfFolders() {
    List<IFolder> webInfFolders =
        FacetUtil.findAllWebInfFolders(projectCreator.getProject());
    Assert.assertTrue(webInfFolders.isEmpty());
  }

  @Test
  public void testFindAllWebInfFolders() throws CoreException {
    IProject project = projectCreator.getProject();
    createPath(project, new Path("src/my-webapp/WEB-INF"));

    List<IFolder> webInfFolders =
        FacetUtil.findAllWebInfFolders(project);
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

    List<IFolder> webInfFolders = FacetUtil.findAllWebInfFolders(project);
    Assert.assertEquals(4, webInfFolders.size());
    Assert.assertTrue(webInfFolders.contains(project.getFolder("webapps/first-webapp/WEB-INF")));
    Assert.assertTrue(webInfFolders.contains(project.getFolder("webapps/second-webapp/WEB-INF")));
    Assert.assertTrue(webInfFolders.contains(project.getFolder("third-webapp/WEB-INF")));
    Assert.assertTrue(webInfFolders.contains(project.getFolder("WEB-INF")));
  }

  @Test
  public void testFindMainWebAppDirectory_noWebInfFolders() {
    IPath mainWebApp = FacetUtil.findMainWebAppDirectory(projectCreator.getProject());
    Assert.assertNull(mainWebApp);
  }

  @Test
  public void testFindMainWebAppDirectory() throws CoreException {
    createPath(projectCreator.getProject(), new Path("webapps/first-webapp/WEB-INF"));
    IPath mainWebApp = FacetUtil.findMainWebAppDirectory(projectCreator.getProject());
    Assert.assertEquals(new Path("webapps/first-webapp"), mainWebApp);
  }

  @Test
  public void testFindMainWebAppDirectory_returnsFolderWithWebXml() throws CoreException {
    IProject project = projectCreator.getProject();
    createPath(project, new Path("webapps/first-webapp/WEB-INF"));
    createEmptyFile(project, new Path("webapps/second-webapp/WEB-INF/web.xml"));
    createPath(project, new Path("third-webapp/WEB-INF"));
    createPath(project, new Path("WEB-INF"));

    IPath mainWebApp = FacetUtil.findMainWebAppDirectory(project);
    Assert.assertEquals(new Path("webapps/second-webapp"), mainWebApp);
  }

  private static void createEmptyFile(IProject project, IPath relativePath) throws CoreException {
    createPath(project, relativePath.removeLastSegments(1));

    InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
    project.getFile(relativePath).create(emptyStream, false /* force */, null /* monitor */);
    Assert.assertTrue(project.getFile(relativePath).exists());
  }

  // TODO: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1573
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
}
