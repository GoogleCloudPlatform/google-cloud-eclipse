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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  PipelineArgumentsTabTest.TabTest.class,
  PipelineArgumentsTabTest.RunButtonCheckedTest.class
})
public class PipelineArgumentsTabTest {

  @RunWith(MockitoJUnitRunner.class)
  public static class TabTest {

    @Rule public ShellTestResource shellResource = new ShellTestResource();

    @Mock public IWorkspaceRoot workspaceRoot;

    private PipelineArgumentsTab tab;

    @Before
    public void setUp() {
      tab = new PipelineArgumentsTab(workspaceRoot);
    }

    @Test
    public void testGetName() {
      Assert.assertEquals("Pipeline Arguments", tab.getName());
    }

    @Test
    public void testInitializeForm_projectNotAccessible() {
      IProject project = mock(IProject.class);
      when(workspaceRoot.getProject(anyString())).thenReturn(project);
      when(project.isAccessible()).thenReturn(false);

      tab.createControl(shellResource.getShell());
      tab.updateRunnerButtons(MajorVersion.ONE);

      ILaunchConfiguration configuration = mock(ILaunchConfiguration.class);
      tab.initializeFrom(configuration);
    }
  }

  @RunWith(Parameterized.class)
  public static class RunButtonCheckedTest {

    private static class Parameter {
      private final MajorVersion majorVersion;
      private final String expectedButtonText;

      private Parameter(MajorVersion majorVersion, String expectedButtonText) {
        this.majorVersion = majorVersion;
        this.expectedButtonText = expectedButtonText;
      }
    }

    @Parameters
    public static Iterable<? extends Object> parameters() {
      return Lists.newArrayList(
          new Parameter(MajorVersion.ONE, "DirectPipelineRunner"),
          new Parameter(MajorVersion.TWO, "DirectRunner"),
          new Parameter(MajorVersion.QUALIFIED_TWO, "DirectRunner"),
          new Parameter(MajorVersion.THREE_PLUS, "DirectRunner"),
          new Parameter(MajorVersion.ALL, "DirectPipelineRunner"));
    }

    @Rule public ShellTestResource shellResource = new ShellTestResource();

    private final Parameter testParameter;
    public RunButtonCheckedTest(Parameter parameter) {
      testParameter = parameter;
    }

    @Test
    public void assertRunnerButtonChecked() {
      Shell shell = shellResource.getShell();
      PipelineArgumentsTab tab = new PipelineArgumentsTab();
      tab.createControl(shell);

      tab.updateRunnerButtons(testParameter.majorVersion);
      Button runnerButton = getCheckedRunnerButton(shell);
      assertNotNull(runnerButton);
      assertEquals(testParameter.expectedButtonText, runnerButton.getText());
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
}
