/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class BuildPathTest {

  private final List<Library> libraries = new ArrayList<>();
  private final IJavaProject project = Mockito.mock(IJavaProject.class);

  @Test
  public void testAddLibraries_emptyList() throws CoreException {
    IProject project = null;
    BuildPath.addLibraries(project, libraries, new NullProgressMonitor());
  }

  @Test
  public void testAddLibraries() throws CoreException {
    IClasspathEntry[] rawClasspath = new IClasspathEntry[0];
    Mockito.when(project.getRawClasspath()).thenReturn(rawClasspath);

    Library library = new Library("libraryId");
    libraries.add(library);
    IClasspathEntry[] result =
        BuildPath.addLibraries(project, libraries, new NullProgressMonitor());
    Assert.assertEquals(1, result.length);

    Mockito.verify(project)
        .setRawClasspath(Mockito.any(IClasspathEntry[].class), Mockito.any(IProgressMonitor.class));
  }

  @Test
  public void testListAdditionalLibraries() throws CoreException {
    IClasspathEntry[] rawClasspath = new IClasspathEntry[0];
    Mockito.when(project.getRawClasspath()).thenReturn(rawClasspath);

    Library library = new Library("libraryId");
    libraries.add(library);
    IClasspathEntry[] result =
        BuildPath.listAdditionalLibraries(project, libraries, new NullProgressMonitor());
    Assert.assertEquals(1, result.length);

    Mockito.verify(project, Mockito.never())
        .setRawClasspath(Mockito.any(IClasspathEntry[].class), Mockito.any(IProgressMonitor.class));
  }

  @Test
  public void testAddLibraries_noDuplicates() throws CoreException {
    Library library = new Library("libraryId");
    IClasspathEntry entry = BuildPath.makeClasspathEntry(library);
    IClasspathEntry[] rawClasspath = {entry};
    Mockito.when(project.getRawClasspath()).thenReturn(rawClasspath);

    libraries.add(library);
    IClasspathEntry[] result =
        BuildPath.addLibraries(project, libraries, new NullProgressMonitor());
    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testAddLibraries_withDuplicates() throws CoreException {
    Library library1 = new Library("library1");
    Library library2 = new Library("library2");
    IClasspathEntry entry = BuildPath.makeClasspathEntry(library1);

    IClasspathEntry[] rawClasspath = {entry};
    Mockito.when(project.getRawClasspath()).thenReturn(rawClasspath);

    libraries.add(library1);
    libraries.add(library2);
    IClasspathEntry[] result =
        BuildPath.addLibraries(project, libraries, new NullProgressMonitor());
    Assert.assertEquals(1, result.length);
    Assert.assertTrue(result[0].getPath().toString()
        .endsWith("com.google.cloud.tools.eclipse.appengine.libraries/library2"));
  }

}
