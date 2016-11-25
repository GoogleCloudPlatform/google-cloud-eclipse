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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.cloud.tools.eclipse.appengine.localserver.server.ModuleUtils;

public class ModuleUtilsTest {

  @Test
  public void testGetServiceId_null() {
    try {
      ModuleUtils.getServiceId(null);
      Assert.fail();
    } catch (NullPointerException expected) {
    }
  }
  
  @Test
  public void testGetServiceId_noWebInf() {
    IModule module = Mockito.mock(IModule.class);
    IProject project = Mockito.mock(IProject.class);
    Mockito.when(module.getProject()).thenReturn(project);
    IFolder webapp = Mockito.mock(IFolder.class);
    Mockito.when(project.getFolder("src/main/webapp")).thenReturn(webapp);
    IFolder webinf = Mockito.mock(IFolder.class);
    Mockito.when(webapp.getFolder("WEB-INF/")).thenReturn(webinf);
    Assert.assertNull(ModuleUtils.getServiceId(module));
  }
  
  @Test
  public void testGetServiceId_default() throws CoreException {
    IModule module = Mockito.mock(IModule.class);
    IProject project = Mockito.mock(IProject.class);
    Mockito.when(module.getProject()).thenReturn(project);
    IFolder webapp = Mockito.mock(IFolder.class);
    Mockito.when(project.getFolder("src/main/webapp")).thenReturn(webapp);
    IFolder webinf = Mockito.mock(IFolder.class);
    Mockito.when(webapp.getFolder("WEB-INF/")).thenReturn(webinf);
    IFile descriptorFile = Mockito.mock(IFile.class);
    Mockito.when(webinf.exists()).thenReturn(true);
    IPath ipath = Mockito.any();
    Mockito.when(webinf.getFile(ipath)).thenReturn(descriptorFile);
    Mockito.when(descriptorFile.exists()).thenReturn(true);
    Mockito.when(descriptorFile.getContents()).thenThrow(new CoreException(Status.CANCEL_STATUS));
    Assert.assertEquals("default", ModuleUtils.getServiceId(module));
  }

}
