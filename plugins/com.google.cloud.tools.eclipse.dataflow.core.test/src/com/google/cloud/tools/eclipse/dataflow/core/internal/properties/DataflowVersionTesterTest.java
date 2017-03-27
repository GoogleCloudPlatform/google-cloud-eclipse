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

package com.google.cloud.tools.eclipse.dataflow.core.internal.properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowDependencyManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link DataflowVersionTester}.
 */
@RunWith(JUnit4.class)
public class DataflowVersionTesterTest {
  @Mock DataflowDependencyManager dependencyManager;
  private DataflowVersionTester tester;

  @Mock
  private IAdaptable adaptable;

  @Mock
  private IResource resource;

  @Mock
  private IProject project;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    tester = new DataflowVersionTester(dependencyManager);
    when(adaptable.getAdapter(IResource.class)).thenReturn(resource);
    when(resource.getProject()).thenReturn(project);
  }

  @Test
  public void testHasPinnedDataflowDependencyFalse() {
    when(dependencyManager.hasPinnedDataflowDependency(project)).thenReturn(false);
    assertFalse(
        tester.test(adaptable, DataflowVersionTester.PINNED_DATAFLOW_VERSION_PROPERTY, null, null));
  }

  @Test
  public void testHasPinnedDataflowDependencyTrue() {
    when(dependencyManager.hasPinnedDataflowDependency(project)).thenReturn(true);
    assertTrue(
        tester.test(adaptable, DataflowVersionTester.PINNED_DATAFLOW_VERSION_PROPERTY, null, null));
  }

  @Test
  public void testHasTrackedDataflowDependencyTrue() {
    when(dependencyManager.hasTrackedDataflowDependency(project)).thenReturn(true);
    assertTrue(
        tester.test(adaptable, DataflowVersionTester.TRACKS_DATAFLOW_VERSION_PROPERTY, null, null));
  }

  @Test
  public void testHasTrackedDataflowDependencyFalse() {
    when(dependencyManager.hasTrackedDataflowDependency(project)).thenReturn(false);
    assertFalse(
        tester.test(adaptable, DataflowVersionTester.TRACKS_DATAFLOW_VERSION_PROPERTY, null, null));
  }
}

