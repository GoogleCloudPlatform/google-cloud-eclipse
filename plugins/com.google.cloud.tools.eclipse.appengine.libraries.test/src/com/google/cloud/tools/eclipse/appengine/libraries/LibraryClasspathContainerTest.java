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

package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Before;
import org.junit.Test;

public class LibraryClasspathContainerTest {

  private List<IClasspathEntry> mockClasspathEntry = Arrays.asList(mock(IClasspathEntry.class));

  private LibraryClasspathContainer classpathContainer;

  @Before
  public void setUp() {
    classpathContainer = new LibraryClasspathContainer(new Path("container/path"),
                                                       "description",
                                                       mockClasspathEntry);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_nullPath() {
    new LibraryClasspathContainer(null, "description", mockClasspathEntry);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_nullDescription() {
    new LibraryClasspathContainer(new Path("container/path"), null, mockClasspathEntry);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_emptyDescription() {
    new LibraryClasspathContainer(new Path("container/path"), "", mockClasspathEntry);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_nullClasspathEntries() {
    new LibraryClasspathContainer(new Path("container/path"), "description", null);
  }

  @Test
  public void testGetPath() {
    assertThat(classpathContainer.getPath(), is((IPath) new Path("container/path")));
  }

  @Test
  public void testGetDescription() {
    assertThat(classpathContainer.getDescription(), is("description"));
  }

  @Test
  public void testGetKind_returnsApplication() throws Exception {
    assertThat(classpathContainer.getKind(), is(IClasspathContainer.K_APPLICATION));
  }

  @Test
  public void testGetClasspathEntries() {
    IClasspathEntry[] classpathEntries = classpathContainer.getClasspathEntries();
    assertNotNull(classpathEntries);
    assertThat(classpathEntries.length, is(1));
    assertSame(mockClasspathEntry.get(0), classpathEntries[0]);
  }
}
