/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ModuleUtilsTest {

  @Mock
  private IModule module;
  @Mock
  private IFile descriptorFile;
  @Mock
  private IFolder webinf;

  @Before
  public void turnOffLogging() {
    Logger logger = Logger.getLogger(ModuleUtils.class.getName());
    logger.setLevel(Level.OFF);
  }

  @Before
  public void setUpMocks() {
    IProject project = Mockito.mock(IProject.class);
    Mockito.when(module.getProject()).thenReturn(project);
    IFolder webapp = Mockito.mock(IFolder.class);
    Mockito.when(project.getFolder("src/main/webapp")).thenReturn(webapp);
    Mockito.when(webapp.getFolder("WEB-INF/")).thenReturn(webinf);
    Mockito.when(webinf.exists()).thenReturn(true);
    IPath ipath = Mockito.any();
    Mockito.when(webinf.getFile(ipath)).thenReturn(descriptorFile);
    Mockito.when(descriptorFile.exists()).thenReturn(true);
  }

  @Test(expected = NullPointerException.class)
  public void testGetServiceId_null() {
    ModuleUtils.getServiceId(null);
  }

  @Test
  public void testGetServiceId_noWebInf() {
    Mockito.when(webinf.exists()).thenReturn(false);
    Assert.assertEquals("default", ModuleUtils.getServiceId(module));
  }

  @Test
  public void testGetServiceId_default() throws CoreException {
    Mockito.when(descriptorFile.getContents()).thenThrow(new CoreException(Status.CANCEL_STATUS));
    Assert.assertEquals("default", ModuleUtils.getServiceId(module));
  }

  @Test
  public void testGetServiceId() throws CoreException {
    mockAppEngineWebXml("appengine-web.xml");
    Assert.assertEquals("myServiceId", ModuleUtils.getServiceId(module));
  }

  @Test
  public void testGetServiceId_module() throws CoreException {
    mockAppEngineWebXml("appengine-web_module.xml");
    Assert.assertEquals("myServiceId", ModuleUtils.getServiceId(module));
  }

  @Test
  public void testGetServiceId_notPresent() throws CoreException {
    mockAppEngineWebXml("appengine-web_noservice.xml");
    Assert.assertEquals("default", ModuleUtils.getServiceId(module));
  }

  @Test
  public void testGetServiceId_malformedXml() throws CoreException {
    mockAppEngineWebXml("appengine-web_malformed.xml");
    Assert.assertEquals("default", ModuleUtils.getServiceId(module));
  }

  private void mockAppEngineWebXml(String testfile) throws CoreException {
    InputStream in = this.getClass().getResourceAsStream(testfile);
    Mockito.when(descriptorFile.getContents()).thenReturn(in);
  }
}
