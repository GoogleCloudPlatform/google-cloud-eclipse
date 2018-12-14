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

import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

public class StandardFacetInstallDelegateTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();
  
  private final StandardFacetInstallDelegate delegate = new StandardFacetInstallDelegate();
  private final IProgressMonitor monitor = new NullProgressMonitor();
  private IProject project;
  
  @Before 
  public void setUp() {
    project = projectCreator.getProject();
  }

  @Test
  public void testCreateConfigFiles_appengineWebXml()
      throws CoreException, IOException, SAXException, AppEngineException {
    delegate.createConfigFiles(project, AppEngineStandardFacet.JRE7, monitor);

    IFile appengineWebXml = project.getFile("src/main/webapp/WEB-INF/appengine-web.xml");
    Assert.assertTrue(appengineWebXml.exists());

    try (InputStream in = appengineWebXml.getContents(true)) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(in);
      // AppEngineDescriptor treats an empty runtime as java7 as of 0.6.
      assertEquals("java7", descriptor.getRuntime());
    }
  }

  @Test
  public void testCreateConfigFiles_loggingProperties() throws CoreException {
    delegate.createConfigFiles(project, AppEngineStandardFacet.JRE7, monitor);

    IFile loggingProperties = project.getFile("src/main/webapp/WEB-INF/logging.properties");
    Assert.assertTrue(loggingProperties.exists());
  }

  @Test
  public void testCreateConfigFiles_dontOverwriteAppengineWebXml()
      throws CoreException, IOException {
    assertNoOverwriting("appengine-web.xml");
  }

  @Test
  public void testCreateConfigFiles_dontOverwriteLoggingProperties()
      throws CoreException, IOException {
    assertNoOverwriting("logging.properties");
  }

  private void assertNoOverwriting(String fileInWebInf) throws CoreException, IOException {
    IFolder webInfDir = project.getFolder("src/main/webapp/WEB-INF");
    ResourceUtils.createFolders(webInfDir, monitor);
    IFile file = webInfDir.getFile(fileInWebInf);
    file.create(new ByteArrayInputStream(new byte[0]), true, monitor);

    Assert.assertTrue(file.exists());

    delegate.createConfigFiles(project, AppEngineStandardFacet.JRE7, monitor);

    // Make sure createConfigFiles did not overwrite the file in WEB-INF
    try (InputStream in = file.getContents(true)) {
      Assert.assertEquals(fileInWebInf + " is not empty", -1, in.read());
    }
  }

}
