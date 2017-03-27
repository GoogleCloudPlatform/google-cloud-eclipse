/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link ProjectPreferenceStore}.
 */
@RunWith(JUnit4.class)
public class ProjectPreferenceStoreTest {
  @Mock
  private IProject project;

  private ProjectPreferenceStore prefs;

  @Before
  public void testSetup() {
    MockitoAnnotations.initMocks(this);

    prefs = new ProjectPreferenceStore(project);
  }

  @Test
  public void testGetOptionAfterSetReturnsSetValue() {
    prefs.setOption("spam", "foo");

    assertEquals("foo", prefs.getOption("spam"));
  }

  @Test
  public void testGetOptionWithoutSetReturnsProjectValue() throws Exception {
    when(project.getPersistentProperty(qualifiedName("ham"))).thenReturn("bar");

    assertEquals("bar", prefs.getOption("ham"));
  }

  @Test
  public void testGetOptionWithNullReturnsEmptyOptional() throws Exception {
    when(project.getPersistentProperty(qualifiedName("eggs"))).thenReturn(null);

    assertNull(prefs.getOption("eggs"));
  }

  @Test
  public void testGetOptionThrowsCoreExceptionReturnsEmptyOptional() throws Exception {
    doThrow(CoreException.class).when(project).getPersistentProperty(qualifiedName("foooo"));

    assertNull(prefs.getOption("foooo"));
  }

  @Test
  public void testSaveWithSetProjectWritesToProject() throws Exception {
    prefs.setOption("bam", "myProject");
    prefs.save();

    verify(project).setPersistentProperty(qualifiedName("bam"), "myProject");
  }

  @Test
  public void testSetProjectWithoutSaveDoesNotWrite() throws Exception {
    prefs.setOption("dang", "foo");

    verify(project, never())
        .setPersistentProperty(Mockito.eq(qualifiedName("dang")), Mockito.<String>any());
  }

  private QualifiedName qualifiedName(String localName) {
    return new QualifiedName(DataflowCorePlugin.PLUGIN_ID, localName);
  }
}

