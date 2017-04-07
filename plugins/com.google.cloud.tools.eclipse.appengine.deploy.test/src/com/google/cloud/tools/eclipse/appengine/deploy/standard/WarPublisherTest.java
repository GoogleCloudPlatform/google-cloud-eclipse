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

package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WarPublisherTest {

  @Mock IProgressMonitor monitor;

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator()
      .withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);

  @Test(expected = NullPointerException.class)
  public void testWriteProjectToStageDir_nullProject() throws CoreException {
    WarPublisher.publishExploded(null, null, monitor);
  }

  @Test(expected = NullPointerException.class)
  public void testWriteProjectToStageDir_nullStagingDir() throws CoreException {
    WarPublisher.publishExploded(mock(IProject.class), null, monitor);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWriteProjectToStageDir_emptyStagingDir() throws CoreException {
    WarPublisher.publishExploded(mock(IProject.class), new Path(""), monitor);
  }

  @Test(expected = OperationCanceledException.class)
  public void testWriteProjectToStageDir_cancelled() throws CoreException {
    when(monitor.isCanceled()).thenReturn(true);
    WarPublisher.publishExploded(mock(IProject.class), new Path(""), monitor);
  }

  @Test
  public void testPublishExploded() throws CoreException {
    IProject project = projectCreator.getProject();
    IFolder exploded = project.getFolder("exloded-war");
    exploded.create(true, true, monitor);
    WarPublisher.publishExploded(project, exploded.getLocation(), monitor);

    exploded.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    assertTrue(exploded.getFile("META-INF/MANIFEST.MF").exists());
    assertTrue(exploded.getFile("WEB-INF/web.xml").exists());
  }

  @Test
  public void testPublishWar() throws CoreException {
    IProject project = projectCreator.getProject();
    IFile war = project.getFile("my-app.war");
    WarPublisher.publishWar(project, war.getLocation(), monitor);

    war.refreshLocal(IResource.DEPTH_ZERO, monitor);
    assertTrue(war.exists());
  }
}
