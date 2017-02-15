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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;;

@RunWith(MockitoJUnitRunner.class)
public class CreateAppEngineStandardWtpProjectTest {

  private static final String APP_ENGINE_API = "appengine-api";

  @Mock private IAdaptable adaptable;

  private NullProgressMonitor monitor = new NullProgressMonitor();
  private AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
  private IProject project;
  
  @Before
  public void setUp() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("testproject" + Math.random());
    config.setProject(project);
  }
  
  @After
  public void cleanUp() throws CoreException {
    project.delete(true, monitor);
  }
  
  @Test
  public void testConstructor() {
    new CreateAppEngineStandardWtpProject(config, adaptable);
  }
  
  @Test
  public void testUnitTestCreated() throws InvocationTargetException, CoreException {
    CreateAppEngineStandardWtpProject creator = new CreateAppEngineStandardWtpProject(config, adaptable);
    creator.execute(new NullProgressMonitor());
    
    assertJunitAndHamcrestAreOnClasspath();
  }

  private void assertJunitAndHamcrestAreOnClasspath() throws CoreException {
    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    IJavaProject javaProject = JavaCore.create(project);
    IType junit = javaProject.findType("org.junit.Assert");
    
    // Is findType doing what we think it's doing?
    // Locally where it passes it finds JUnit in
    // class Assert [in Assert.class [in org.junit [in /Users/elharo/workspace/.metadata/.plugins/org.eclipse.pde.core/.bundle_pool/plugins/org.junit_4.12.0.v201504281640/junit.jar]]]
    
    assertNotNull("Did not find junit", junit);
    assertTrue(junit.exists());
    IType hamcrest = javaProject.findType("org.hamcrest.CoreMatchers");
    assertNotNull("Did not find hamcrest", hamcrest);
    assertTrue(hamcrest.exists());
  }

  @Test
  public void testAppEngineLibrariesAdded() throws InvocationTargetException, CoreException {
    Library library = new Library(APP_ENGINE_API);
    config.setAppEngineLibraries(Collections.singletonList(library));
    CreateAppEngineStandardWtpProject creator = new CreateAppEngineStandardWtpProject(config, adaptable);
    creator.execute(new NullProgressMonitor());
    assertAppEngineContainerOnClasspath(library);
  }

  private void assertAppEngineContainerOnClasspath(Library library) throws CoreException {
    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    IJavaProject javaProject = JavaCore.create(project);
    for (IClasspathEntry iClasspathEntry : javaProject.getRawClasspath()) {
      if (iClasspathEntry.getPath().equals(library.getContainerPath())) {
        return;
      }
    }
    fail("Classpath container " + APP_ENGINE_API + " was not added to the build path");
  }

  @Test
  public void testNullConfig() {
    try {
      new CreateAppEngineStandardWtpProject(null, adaptable);
      Assert.fail("allowed null config");
    } catch (NullPointerException ex) {
      // success
    }
  }

}
