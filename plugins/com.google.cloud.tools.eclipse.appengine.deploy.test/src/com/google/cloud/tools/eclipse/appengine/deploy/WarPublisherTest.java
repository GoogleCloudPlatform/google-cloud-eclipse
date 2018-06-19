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

package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WarPublisherTest {

  @Mock private IProgressMonitor monitor;

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator()
      .withFacets(JavaFacet.VERSION_1_7);

  @Test
  public void testWriteProjectToStageDir_nullProject() throws CoreException {
    try {
      WarPublisher.publishExploded(null, null, null, monitor);
      fail();
    } catch (NullPointerException e) {
      assertEquals("project is null", e.getMessage());
    }
  }

  @Test
  public void testWriteProjectToStageDir_nullDestination() throws CoreException {
    try {
      WarPublisher.publishExploded(mock(IProject.class), null, null, monitor);
      fail();
    } catch (NullPointerException e) {
      assertEquals("destination is null", e.getMessage());
    }
  }

  @Test
  public void testWriteProjectToStageDir_emptyDestinationDirectory() throws CoreException {
    try {
      WarPublisher.publishExploded(mock(IProject.class), new Path(""), null, monitor);
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("destination is empty path", e.getMessage());
    }
  }

  @Test
  public void testWriteProjectToStageDir_nullSafeWorkDirectory() throws CoreException {
    try {
      WarPublisher.publishExploded(mock(IProject.class), new Path("/"), null, monitor);
      fail();
    } catch (NullPointerException e) {
      assertEquals("safeWorkDirectory is null", e.getMessage());
    }
  }

  @Test
  public void testWriteProjectToStageDir_cancelled() throws CoreException {
    try {
      when(monitor.isCanceled()).thenReturn(true);
      WarPublisher.publishExploded(mock(IProject.class), new Path("/"), new Path("/"), monitor);
      fail();
    } catch (OperationCanceledException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testPublishExploded() throws CoreException {
    IProject project = projectCreator.withFacets(WebFacetUtils.WEB_25).getProject();
    IFolder exploded = project.getFolder("exploded-war");
    IPath tempDirectory = project.getFolder("temp").getLocation();
    IStatus[] result =
        WarPublisher.publishExploded(project, exploded.getLocation(), tempDirectory, monitor);
    assertEquals(0, result.length);

    exploded.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    assertTrue(exploded.getFile("META-INF/MANIFEST.MF").exists());
    assertTrue(exploded.getFile("WEB-INF/web.xml").exists());
  }

  @Test
  public void testPublishWar() throws CoreException {
    IProject project = projectCreator.withFacets(WebFacetUtils.WEB_25).getProject();
    IFile war = project.getFile("my-app.war");
    IPath tempDirectory = project.getFolder("temp").getLocation();
    IStatus[] result = WarPublisher.publishWar(project, war.getLocation(), tempDirectory, monitor);
    assertEquals(0, result.length);

    war.refreshLocal(IResource.DEPTH_ZERO, monitor);
    assertTrue(war.exists());
  }

  @Test
  public void testPublishExploded_noResource() throws CoreException {
    IProject project = projectCreator.getProject();
    IStatus[] result = WarPublisher.publishExploded(project, new Path("/"), new Path("/"), monitor);
    assertEquals(1, result.length);
    assertThat(result[0].getMessage(), Matchers.endsWith(" has no resources to publish"));
  }

  @Test
  public void testPublishWar_noResource() throws CoreException {
    IProject project = projectCreator.getProject();
    IStatus[] result = WarPublisher.publishWar(project, new Path("/"), new Path("/"), monitor);
    assertEquals(1, result.length);
    assertThat(result[0].getMessage(), Matchers.endsWith(" has no resources to publish"));
  }
}
