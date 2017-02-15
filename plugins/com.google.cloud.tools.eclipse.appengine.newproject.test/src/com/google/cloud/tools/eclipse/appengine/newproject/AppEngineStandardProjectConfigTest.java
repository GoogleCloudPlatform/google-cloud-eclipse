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

package com.google.cloud.tools.eclipse.appengine.newproject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Test;

public class AppEngineStandardProjectConfigTest {

  private AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
  
  public void testProject() throws CoreException {
    try {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IProject project = workspace.getRoot().getProject("foobar");
      config.setProject(project);
      Assert.assertTrue(project.getRawLocationURI().getPath().endsWith("foobar"));
    } finally {
      config.getProject().delete(true, new NullProgressMonitor());
    }
  }
  
  @Test
  public void testPackageName() {
    config.setPackageName("com.foo.bar");
    Assert.assertEquals("com.foo.bar", config.getPackageName());
  }
  
  @Test
  public void testEclipseProjectLocationUri() throws URISyntaxException {   
    config.setEclipseProjectLocationUri(new URI("file://foo/bar"));   
    Assert.assertEquals(new URI("file://foo/bar"), config.getEclipseProjectLocationUri());    
  }

  @Test
  public void testAppEngineLibraries() {
    config.setAppEngineLibraries(Collections.singletonList(new Library("app-engine-library")));
    assertThat(config.getAppEngineLibraries().size(), is(1));
    assertThat(config.getAppEngineLibraries().iterator().next().getId(), is("app-engine-library"));
  }
}
