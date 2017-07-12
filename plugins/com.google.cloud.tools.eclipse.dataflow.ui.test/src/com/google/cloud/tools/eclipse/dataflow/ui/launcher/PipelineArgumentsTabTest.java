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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.common.base.Predicate;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class PipelineArgumentsTabTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  private PipelineArgumentsTab tab = new PipelineArgumentsTab();

  @Test
  public void testGetName() {
    Assert.assertEquals("Pipeline Arguments", tab.getName());
  }

  @Test
  public void testRunnerButtonChecked_majorVersionOne() {
    assertRunnerButtonChecked(MajorVersion.ONE, "DirectPipelineRunner");
  }

  @Test
  public void testRunnerButtonChecked_majorVersionTwo() {
    assertRunnerButtonChecked(MajorVersion.TWO, "DirectRunner");
  }

  @Test
  public void testRunnerButtonChecked_majorVersionQualifiedTwo() {
    assertRunnerButtonChecked(MajorVersion.QUALIFIED_TWO, "DirectRunner");
  }

  @Test
  public void testRunnerButtonChecked_majorVersionThreePlus() {
    assertRunnerButtonChecked(MajorVersion.THREE_PLUS, "DirectRunner");
  }

  @Test
  public void testRunnerButtonChecked_majorVersionAll() {
    assertRunnerButtonChecked(MajorVersion.ALL, "DirectPipelineRunner");
  }

  private void assertRunnerButtonChecked(MajorVersion majorVersion, String expectedButtonText) {
    Shell shell = shellResource.getShell();
    tab.createControl(shell);

    tab.updateRunnerButtons(majorVersion);
    Button runnerButton = getCheckedRunnerButton(shell);
    assertNotNull(runnerButton);
    assertEquals(expectedButtonText, runnerButton.getText());
    assertTrue(runnerButton.getSelection());

    // Should not throw IllegalStateException:
    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2136
    tab.getSelectedRunner();
  }

  private static Button getCheckedRunnerButton(Composite composite) {
    Group runnerGroup = CompositeUtil.findControl(composite, Group.class);
    assertEquals("Runner:", runnerGroup.getText());

    return (Button) CompositeUtil.findControl(runnerGroup, new Predicate<Control>() {
      @Override
      public boolean apply(Control control) {
        return control instanceof Button && ((Button) control).getSelection();
      }
    });
  }
}
