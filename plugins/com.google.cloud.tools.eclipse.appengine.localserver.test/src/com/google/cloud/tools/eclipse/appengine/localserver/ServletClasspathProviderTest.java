/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.localserver;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.cloud.tools.eclipse.appengine.libraries.ILibraryClasspathContainerResolverService;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.server.core.IRuntime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServletClasspathProviderTest {
  @Rule
  public TestProjectCreator projectCreator =
      new TestProjectCreator().withFacets(WebFacetUtils.WEB_31, JavaFacet.VERSION_1_8);

  @Mock public ILibraryClasspathContainerResolverService resolver;

  private ServletClasspathProvider fixture;

  @Test
  public void testApiLibraryIds_web31() {
    assertArrayEquals(
        new String[] {"servlet-api-3.1", "jsp-api-2.3"},
        ServletClasspathProvider.getApiLibraryIds(WebFacetUtils.WEB_31));
  }

  @Test
  public void testApiLibraryIds_web30() {
    assertArrayEquals(
        new String[] {"servlet-api-3.1", "jsp-api-2.3"},
        ServletClasspathProvider.getApiLibraryIds(WebFacetUtils.WEB_30));
  }

  @Test
  public void testApiLibraryIds_web25() {
    assertArrayEquals(
        new String[] {"servlet-api-2.5", "jsp-api-2.1"},
        ServletClasspathProvider.getApiLibraryIds(WebFacetUtils.WEB_25));
  }

  @Test
  public void testUncached_callsResolver() throws CoreException, InterruptedException {
    IRuntime runtime = mock(IRuntime.class);
    IClasspathEntry servletApi = mock(IClasspathEntry.class);
    IClasspathEntry jspApi = mock(IClasspathEntry.class);
    IClasspathEntry[] servletClasspath = new IClasspathEntry[] {servletApi, jspApi};
    doReturn(servletClasspath)
        .when(resolver)
        .resolveLibrariesAttachSources("servlet-api-3.1", "jsp-api-2.3");

    // subclass ServletClasspathProvider to ignore the actual update request
    AtomicReference<IClasspathEntry[]> requestedUpdate = new AtomicReference<>(null);
    fixture =
        new ServletClasspathProvider(resolver) {
          @Override
          protected void requestClasspathContainerUpdate(
              IProject project, IRuntime runtime, IClasspathEntry[] entries) {
            requestedUpdate.set(entries);
          }
        };

    IClasspathEntry[] result =
        fixture.resolveClasspathContainer(projectCreator.getProject(), runtime);
    assertNull(result); // since not cached
    waitForJobs();
    verify(resolver).getSchedulingRule(); // for background job
    verify(resolver).resolveLibrariesAttachSources("servlet-api-3.1", "jsp-api-2.3");
    assertNotNull(requestedUpdate.get());
    assertSame(servletClasspath, requestedUpdate.get());
    verifyNoMoreInteractions(resolver);
  }

  @Test
  public void testCached_noResolver() {
    IRuntime runtime = mock(IRuntime.class);
    IClasspathEntry[] entries = new IClasspathEntry[0];

    AtomicReference<IClasspathEntry[]> requestedUpdate = new AtomicReference<>(null);
    fixture =
        new ServletClasspathProvider(resolver) {
          @Override
          protected void requestClasspathContainerUpdate(
              IProject project, IRuntime runtime, IClasspathEntry[] entries) {
            requestedUpdate.set(entries);
          }
        };
    fixture.libraryEntries.put(WebFacetUtils.WEB_31, entries);

    IClasspathEntry[] result =
        fixture.resolveClasspathContainer(projectCreator.getProject(), runtime);
    assertSame(entries, result);
    assertNull(requestedUpdate.get());
    verifyNoMoreInteractions(resolver);
  }

  private void waitForJobs() throws InterruptedException {
    Job.getJobManager().join(fixture, null);
  }
}
