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

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import java.util.Collections;
import java.util.HashMap;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class ServletClasspathProviderTest {

  private ServletClasspathProvider provider;
  @Mock private ILibraryRepositoryService repositoryService;

  @Before
  public void setUp() {
    Library servletApi = getMockApi("servlet-api");
    Library jspApi = getMockApi("jsp-api");
    HashMap<String, Library> libraries = new HashMap<>();
    libraries.put("servlet-api", servletApi);
    libraries.put("jsp-api", jspApi);
    provider = new ServletClasspathProvider();
  }

  @Test
  public void testResolveClasspathContainer() {
    IClasspathEntry[] result = provider.resolveClasspathContainer(null, null);
    Assert.assertTrue(result[0].getPath().lastSegment().equals("servlet-api.jar"));
    Assert.assertTrue(result[1].getPath().lastSegment().equals("jsp-api.jar"));
  }

  @Test
  public void testResolveClasspathContainerThrowsError() {
//    when(repositoryService.getLibraryClasspathEntry(any(IJavaProject.class), any(LibraryFile.class),
//                                                    any(IProgressMonitor.class)))
//      .thenThrow(new LibraryRepositoryServiceException("test exception"));
    assertNull(provider.resolveClasspathContainer(null, null));
  }

  @Test
  public void testResolveClasspathContainer_mavenProject() throws CoreException {
    IProject project = Mockito.mock(IProject.class);
    Mockito.when(project.hasNature("org.eclipse.m2e.core.maven2Nature")).thenReturn(true);
    Mockito.when(project.isAccessible()).thenReturn(true);
    Assert.assertNull(provider.resolveClasspathContainer(project, null));
  }

  private Library getMockApi(String libraryId) {
    Library servletApi = new Library(libraryId);
    LibraryFile libraryFile = mock(LibraryFile.class);
    servletApi.setLibraryFiles(Collections.singletonList(libraryFile));
    IClasspathEntry classpathEntry = mock(IClasspathEntry.class);
    when(classpathEntry.getPath()).thenReturn(new Path("/path/to/" + libraryId + ".jar"));
//    when(repositoryService.getLibraryClasspathEntry(any(IJavaProject.class), eq(libraryFile),
//                                                    any(IProgressMonitor.class))).thenReturn(classpathEntry);
    return servletApi;
  }
}
