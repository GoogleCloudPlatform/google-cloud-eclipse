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

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineProject;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProjectTest;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;

public class CreateAppEngineStandardWtpProjectTest extends CreateAppEngineWtpProjectTest {

  @Override
  protected CreateAppEngineProject newCreateAppEngineProject() {
    return new CreateAppEngineStandardWtpProject(config, mock(IAdaptable.class), repositoryService);
  }

  @Test
  public void testConstructor() {
    newCreateAppEngineProject();
  }

  @Test
  public void testAppEngineRuntimeAdded() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    IRuntime primaryRuntime = facetedProject.getPrimaryRuntime();
    assertTrue(AppEngineStandardFacet.isAppEngineStandardRuntime(primaryRuntime));
  }

  @Test
  public void testAppEngineLibrariesAdded() throws InvocationTargetException, CoreException {
    Library library = CloudLibraries.getLibrary("appengine-api");
    List<Library> libraries = new ArrayList<>();
    libraries.add(library);
    config.setAppEngineLibraries(libraries);
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    assertAppEngineApiSdkOnClasspath();
  }

  private void assertAppEngineApiSdkOnClasspath() throws CoreException {
    IJavaProject javaProject = JavaCore.create(project);
    Matcher<IClasspathEntry> masterLibraryEntryMatcher =
        new CustomTypeSafeMatcher<IClasspathEntry>("has master container") {
      @Override
      protected boolean matchesSafely(IClasspathEntry entry) {
        return entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && entry.getPath().toString()
            .equals("com.google.cloud.tools.eclipse.appengine.libraries/master-container");
      }};
    Matcher<IClasspathEntry> appEngineSdkMatcher =
        new CustomTypeSafeMatcher<IClasspathEntry>("has appengine-api-1.0-sdk") {
          @Override
          protected boolean matchesSafely(IClasspathEntry entry) {
            return entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY
                && entry.getPath().toString().contains("appengine-api-1.0-sdk");
          }
        };
    assertThat(Arrays.asList(javaProject.getRawClasspath()), hasItem(masterLibraryEntryMatcher));
    assertThat(Arrays.asList(javaProject.getResolvedClasspath(true)), hasItem(appEngineSdkMatcher));
  }

  @Test
  public void testNullConfig() {
    try {
      new CreateAppEngineStandardWtpProject(null, mock(IAdaptable.class),
          mock(ILibraryRepositoryService.class));
      Assert.fail("allowed null config");
    } catch (NullPointerException ex) {
      // success
    }
  }
}
