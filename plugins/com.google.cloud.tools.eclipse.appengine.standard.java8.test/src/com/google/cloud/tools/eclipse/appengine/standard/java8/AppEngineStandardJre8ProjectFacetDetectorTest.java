/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.standard.java8;

import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineStandardJre8ProjectFacetDetectorTest {

  @Rule public final TestProjectCreator projectCreator = new TestProjectCreator();

  private IProject project;

  @Before
  public void setUp() {
    project = projectCreator.getProject();
  }

  @Test
  public void testGetWebFacetVersionToInstall_noWebXml() {
    assertEquals(WebFacetUtils.WEB_31,
        AppEngineStandardJre8ProjectFacetDetector.getWebFacetVersionToInstall(project));
  }

  @Test
  public void testGetWebFacetVersionToInstall_servlet25WebXml() throws CoreException {
    createFile("src/main/webapp/WEB-INF/web.xml", "<web-app version='2.5'/>");
    assertEquals(WebFacetUtils.WEB_25,
        AppEngineStandardJre8ProjectFacetDetector.getWebFacetVersionToInstall(project));
  }

  @Test
  public void testGetWebFacetVersionToInstall_servlet31WebXml() throws CoreException {
    createFile("src/main/webapp/WEB-INF/web.xml", "<web-app version='3.1'/>"); 
    assertEquals(WebFacetUtils.WEB_31,
        AppEngineStandardJre8ProjectFacetDetector.getWebFacetVersionToInstall(project));
  }

  @Test
  public void testGetWebFacetVersionToInstall_noVersionWebXml() throws CoreException {
    createFile("src/main/webapp/WEB-INF/web.xml", "<web-app/>"); 
    assertEquals(WebFacetUtils.WEB_31,
        AppEngineStandardJre8ProjectFacetDetector.getWebFacetVersionToInstall(project));
  }

  @Test
  public void testGetWebFacetVersionToInstall_invalidWebXml() throws CoreException {
    createFile("src/main/webapp/WEB-INF/web.xml", "not XML"); 
    assertEquals(WebFacetUtils.WEB_31,
        AppEngineStandardJre8ProjectFacetDetector.getWebFacetVersionToInstall(project));
  }

  @Test
  public void testGetWebFacetVersionToInstall_webContentFolder() throws CoreException {
    createFile("WebContent/WEB-INF/web.xml", "<web-app version='2.5'/>"); 
    assertEquals(WebFacetUtils.WEB_25,
        AppEngineStandardJre8ProjectFacetDetector.getWebFacetVersionToInstall(project));
  }

  private IFile createFile(String path, String content) throws CoreException {
    InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    IFile file = project.getFile(path);
    ResourceUtils.createFolders(file.getParent(), null);
    file.create(in, true, null);
    return file;
  }
}
