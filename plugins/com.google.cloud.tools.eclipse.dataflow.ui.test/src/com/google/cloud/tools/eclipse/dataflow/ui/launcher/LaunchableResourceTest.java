/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link LaunchableResource}.
 */
@RunWith(JUnit4.class)
public class LaunchableResourceTest {
  @Mock
  private IMethod mainMethod;
  @Mock
  private IType primaryType;
  
  @Mock
  private IResource resource;
  @Mock
  private IProject project;
  private String projectName = "my_test_project";
  
  @Rule public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(resource.getProject()).thenReturn(project);
    when(project.getName()).thenReturn(projectName);
  }
  
  @Test
  public void testMethodAndPrimaryTypeSpecified() {
    when(mainMethod.exists()).thenReturn(true);
    when(primaryType.getElementName()).thenReturn("primaryType");
    LaunchableResource launchable = new LaunchableResource(resource, mainMethod, primaryType);
    assertEquals(mainMethod, launchable.getMainMethod());
    assertEquals(projectName + "_primaryType", launchable.getLaunchName());
    assertEquals(projectName, launchable.getProjectName());
  }
  
  @Test
  public void testMethodAndPrimaryTypeUnspecified() {
    LaunchableResource launchable = new LaunchableResource(resource);

    assertEquals(projectName, launchable.getLaunchName());
    assertEquals(projectName, launchable.getProjectName());
    assertNull(launchable.getMainMethod());
  }
  
  @Test
  public void testMethodUnspecifiedWithPrimaryType() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("mainMethod is null");
    thrown.expectMessage("primaryType is not null");
    new LaunchableResource(resource, null, primaryType);
  }
  
  @Test
  public void testMethodSpecifiedWithoutPrimaryType() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("mainMethod is not null");
    thrown.expectMessage("primaryType is null");
    new LaunchableResource(resource, mainMethod, null);    
  }
}
