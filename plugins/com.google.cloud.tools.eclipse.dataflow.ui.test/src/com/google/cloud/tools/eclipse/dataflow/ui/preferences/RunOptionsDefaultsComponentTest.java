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

package com.google.cloud.tools.eclipse.dataflow.ui.preferences;

import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.ui.page.MessageTarget;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RunOptionsDefaultsComponentTest {
  
  @Rule public ShellTestResource shellResource = new ShellTestResource();
  
  @Mock private DataflowPreferences preferences;
  @Mock private MessageTarget messageTarget;

  private RunOptionsDefaultsComponent component;
  private Composite composite;

  @Before
  public void setUp() {
    Shell shell = shellResource.getShell();
    composite = new Composite(shell, SWT.NONE);
    component = new RunOptionsDefaultsComponent(composite, 3, messageTarget, preferences);
  }

  @Test
  public void testConstructor_testGrid() {
    try {
      new RunOptionsDefaultsComponent(null, 0, null, null);
      Assert.fail("didn't check grid");
    } catch (IllegalArgumentException ex) {
      Assert.assertNotNull(ex.getMessage());
    }
  }
  
  @Test
  public void testStagingLocation() {
    component.setStagingLocationText("foobar");
    Assert.assertEquals("gs://foobar", component.getStagingLocation());
  }
  
  @Test
  public void testCloudProjectText() {
    Assert.assertEquals("", component.getProject());
    component.setCloudProjectText("foo");
    Assert.assertEquals("foo", component.getProject());
  }

  @Test
  public void testGetControl() {
    Assert.assertSame(composite, component.getControl());
  }
}
