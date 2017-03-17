/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SourceAttacherJobTest {

  private SourceAttacherJob attacherJob;

  @Before
  public void setUp() {
    IPath path = mock(IPath.class);
    IJavaProject javaProject = mock(IJavaProject.class);
    when(javaProject.getProject()).thenReturn(mock(IProject.class));
    attacherJob = new SourceAttacherJob(javaProject, path, path, mock(Callable.class));
  }

  @Test
  public void testAttachSource_normalExecutionOnLibraryClasspathContainer() throws Exception {
    LibraryClasspathContainer validContainer = mock(LibraryClasspathContainer.class);
    when(validContainer.getClasspathEntries()).thenReturn(new IClasspathEntry[0]);
    when(validContainer.copyWithNewEntries(any(List.class))).thenReturn(validContainer);

    LibraryClasspathContainer newContainer = attacherJob.attachSource(validContainer);
    assertNotNull(newContainer);
  }

  @Test
  public void testAttachSource_shortCircuitOnGenericClasspathContainer() throws Exception {
    IClasspathContainer invalidContainer = mock(IClasspathContainer.class);

    LibraryClasspathContainer newContainer = attacherJob.attachSource(invalidContainer);
    assertNull(newContainer);
  }
}
