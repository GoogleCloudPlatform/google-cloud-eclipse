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

package com.google.cloud.tools.eclipse.appengine.newproject.standard;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProjectTest;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.junit.Assert;
import org.junit.Test;

public class CreateAppEngineStandardWtpProjectTest extends CreateAppEngineWtpProjectTest {

  private static final String APP_ENGINE_API = "appengine-api";

  @Override
  protected CreateAppEngineWtpProject newCreateAppEngineWtpProject() {
    return new CreateAppEngineStandardWtpProject(config, mock(IAdaptable.class));
  }

  @Test
  public void testConstructor() {
    newCreateAppEngineWtpProject();
  }

  @Test
  public void testAppEngineRuntimeAdded() throws InvocationTargetException, CoreException {
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    IRuntime primaryRuntime = facetedProject.getPrimaryRuntime();
    assertTrue(AppEngineStandardFacet.isAppEngineStandardRuntime(primaryRuntime));
  }

  @Test
  public void testAppEngineLibrariesAdded() throws InvocationTargetException, CoreException {
    Library library = new Library(APP_ENGINE_API);
    config.setAppEngineLibraries(Collections.singletonList(library));
    CreateAppEngineWtpProject creator = newCreateAppEngineWtpProject();
    creator.execute(monitor);

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
      new CreateAppEngineStandardWtpProject(null, mock(IAdaptable.class));
      Assert.fail("allowed null config");
    } catch (NullPointerException ex) {
      // success
    }
  }
}
