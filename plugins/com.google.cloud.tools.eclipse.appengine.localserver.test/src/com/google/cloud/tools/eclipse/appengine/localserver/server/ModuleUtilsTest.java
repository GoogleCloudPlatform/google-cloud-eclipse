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

import static org.mockito.Matchers.any;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;

public class ModuleUtilsTest {

  private IModule module = Mockito.mock(IModule.class);
  private IFile descriptorFile = Mockito.mock(IFile.class);
  private IFolder webinf = Mockito.mock(IFolder.class);

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

  private void mockAppEngineWebXml(String testfile) throws CoreException {
    InputStream in = this.getClass().getResourceAsStream(testfile);
    Mockito.when(descriptorFile.getContents()).thenReturn(in);
  }

  @Test
  public void testGetAllModules_single() {
    IServer server = Mockito.mock(IServer.class);
    Mockito.when(server.getModules()).thenReturn(new IModule[] {module});
    Mockito.when(server.getChildModules(any(IModule[].class), any(IProgressMonitor.class)))
        .thenReturn(new IModule[0]);

    IModule[] result = ModuleUtils.getAllModules(server);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.length);
    Assert.assertThat(result[0], Matchers.sameInstance(module));
  }

  @Test
  public void testGetAllModules_multi() {
    IServer server = Mockito.mock(IServer.class);
    Mockito.when(server.getModules()).thenReturn(new IModule[] {module});
    Mockito.when(server.getChildModules(any(IModule[].class), any(IProgressMonitor.class)))
        .thenReturn(new IModule[0]);

    IModule module2a = Mockito.mock(IModule.class, "module2a");
    IModule module2b = Mockito.mock(IModule.class, "module2b");
    Mockito.when(server.getChildModules(AdditionalMatchers.aryEq(new IModule[] {module}),
        any(IProgressMonitor.class))).thenReturn(new IModule[] {module2a, module2b});

    IModule module3 = Mockito.mock(IModule.class, "module3");
    Mockito.when(server.getChildModules(AdditionalMatchers.aryEq(new IModule[] {module, module2b}),
        any(IProgressMonitor.class))).thenReturn(new IModule[] {module3});

    IModule[] result = ModuleUtils.getAllModules(server);
    Assert.assertNotNull(result);
    Assert.assertEquals(4, result.length);
    Assert.assertThat(result[0], Matchers.sameInstance(module));
    Assert.assertThat(result[1], Matchers.sameInstance(module2a));
    Assert.assertThat(result[2], Matchers.sameInstance(module2b));
    Assert.assertThat(result[3], Matchers.sameInstance(module3));
  }

}
