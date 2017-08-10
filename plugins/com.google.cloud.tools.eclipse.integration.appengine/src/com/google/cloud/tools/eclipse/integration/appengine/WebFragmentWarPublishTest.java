/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.deploy.WarPublisher;
import com.google.cloud.tools.eclipse.test.util.ZipUtil;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.io.IOException;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebFragmentWarPublishTest {

  private static final IProgressMonitor monitor = new NullProgressMonitor();
  private static IProject project;

  @BeforeClass
  public static void setUp() throws IOException, CoreException {
    List<IProject> projects = ProjectUtils.importProjects(WebFragmentWarPublishTest.class,
        "test-projects/web-fragment-example.zip", false /* checkBuildErrors */, monitor);
    assertEquals(1, projects.size());
    project = projects.get(0);
  }

  @AfterClass
  public static void tearDown() throws CoreException {
    if (project != null) {
      ProjectUtils.waitForProjects(project);
      project.delete(true, null);
    }
  }

  @Test
  public void testPublishExploded_webFragmentJarPublished() throws CoreException {
    IFolder exploded = project.getFolder("exploded-war");
    try {
      WarPublisher.publishExploded(project, exploded.getLocation(), monitor);

      exploded.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      assertTrue(exploded.getFile("WEB-INF/lib/spring-web-4.3.8.RELEASE.jar").exists());
    } finally {
      exploded.delete(true, monitor);
    }
  }

  @Test
  public void testPublishWar_webFragmentJarPublished() throws CoreException {
    IFile war = project.getFile("my-app.war");
    IFolder unzipped = project.getFolder("unzipped");
    try {
      WarPublisher.publishWar(project, war.getLocation(), monitor);

      ZipUtil.unzip(war.getLocation().toFile(), unzipped.getLocation().toFile(), monitor);
      unzipped.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      assertTrue(unzipped.getFile("WEB-INF/lib/spring-web-4.3.8.RELEASE.jar").exists());
    } finally {
      war.delete(true, monitor);
      unzipped.delete(true, monitor);
    }
  }
}
