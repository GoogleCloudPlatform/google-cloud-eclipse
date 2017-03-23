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
import org.junit.Rule;
import org.junit.Test;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;

public class StandardFacetInstallDelegateTest {

  private StandardFacetInstallDelegate delegate = new StandardFacetInstallDelegate();
  private IProgressMonitor monitor = new NullProgressMonitor(); 
  private IProject project;

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();
  
  @Test
  public void testCreateConfigFiles() throws CoreException, IOException {
    project = projectCreator.getProject();
    delegate.createConfigFiles(project, monitor);
    
    IFile appengineWebXml = project.getFile("src/main/webapp/WEB-INF/appengine-web.xml");
    Assert.assertTrue(appengineWebXml.exists());
    
    try (InputStream in = appengineWebXml.getContents()) {
      Assert.assertEquals('<', in.read());       
    }
  }
  
  @Test
  public void testCreateConfigFiles_dontOverwrite() 
      throws CoreException, IOException {
    project = projectCreator.getProject();
    
    IFolder webInfDir = project.getFolder("src/main/webapp/WEB-INF");
    ResourceUtils.createFolders(webInfDir, monitor);
    IFile appengineWebXml = project.getFile("src/main/webapp/WEB-INF/appengine-web.xml");
    appengineWebXml.create(new ByteArrayInputStream(new byte[0]), true, monitor);

    Assert.assertTrue(appengineWebXml.exists());
    
    delegate.createConfigFiles(project, monitor);
    
    // Make sure createConfigFiles did not write any data into appengine-web.xml
    try (InputStream in = appengineWebXml.getContents()) {
      Assert.assertEquals("appengine-web.xml is not empty", -1, in.read());       
    }

  }

}
