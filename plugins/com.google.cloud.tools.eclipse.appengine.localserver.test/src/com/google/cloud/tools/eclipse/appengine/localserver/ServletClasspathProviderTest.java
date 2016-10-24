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

package com.google.cloud.tools.eclipse.appengine.localserver;

import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Paths;

@RunWith(MockitoJUnitRunner.class)
public class ServletClasspathProviderTest {

  private ServletClasspathProvider provider = new ServletClasspathProvider();
  @Mock private CloudSdk cloudSdk;
  
  @Before
  public void setUp() {
    when(cloudSdk.getJarPath("servlet-api.jar")).thenReturn(Paths.get("/path/to/servlet-api.jar"));
    when(cloudSdk.getJarPath("jsp-api.jar")).thenReturn(Paths.get("/path/to/jsp-api.jar"));
    provider.setCloudSdk(cloudSdk);
  }

  @Test
  public void testResolveClasspathContainer() {
    IClasspathEntry[] result = provider.resolveClasspathContainer(null, null);
    Assert.assertTrue(result[0].getPath().toString().endsWith("servlet-api.jar"));
    Assert.assertTrue(result[1].getPath().toString().endsWith("jsp-api.jar"));
  }

  @Test
  public void testResolveClasspathContainer_mavenProject() throws CoreException {
    IProject project = Mockito.mock(IProject.class);
    Mockito.when(project.hasNature("org.eclipse.m2e.core.maven2Nature")).thenReturn(true);
    Mockito.when(project.isAccessible()).thenReturn(true);
    IClasspathEntry[] result = provider.resolveClasspathContainer(project, null);
    Assert.assertEquals(0, result.length);
  }

}
