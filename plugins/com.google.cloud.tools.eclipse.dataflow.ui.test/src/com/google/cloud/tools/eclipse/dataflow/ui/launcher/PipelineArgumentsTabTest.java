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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineConfigurationAttr;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineLaunchConfiguration;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowDependencyManager;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider.State;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.common.collect.Lists;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  PipelineArgumentsTabTest.TabTest.class,
  PipelineArgumentsTabTest.RunButtonCheckedTest.class
})
public class PipelineArgumentsTabTest {

  public static class TabTest {

    @Rule public ShellTestResource shellResource = new ShellTestResource();

    private IWorkspaceRoot workspaceRoot;
    private DataflowDependencyManager dependencyManager;
    private PipelineArgumentsTab pipelineArgumentsTab;

    private IProject project1;
    private IJavaProject javaProject1;
    private ILaunchConfigurationWorkingCopy configuration1;
    private IProject project2;
    private IJavaProject javaProject2;
    private ILaunchConfigurationWorkingCopy configuration2;

    @Before
    public void setUp() throws CoreException, InvocationTargetException, InterruptedException {
      TestAccountProvider.setAsDefaultProvider();
      TestAccountProvider.setProviderState(State.NOT_LOGGED_IN);
      workspaceRoot = mock(IWorkspaceRoot.class);
      dependencyManager = mock(DataflowDependencyManager.class);

      project1 = mockProject("project1");
      javaProject1 = mock(IJavaProject.class);
      when(workspaceRoot.getProject("project1")).thenReturn(project1);
      when(project1.getAdapter(IJavaElement.class)).thenReturn(javaProject1);
      when(javaProject1.getJavaProject()).thenReturn(javaProject1);

      configuration1 = mockLaunchConfiguration();
      when(configuration1.getAttribute(eq(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME),
          anyString())).thenReturn("project1");
      doReturn(MajorVersion.ONE).when(dependencyManager).getProjectMajorVersion(project1);

      project2 = mockProject("project2");
      javaProject2 = mock(IJavaProject.class);
      when(project2.getAdapter(IJavaElement.class)).thenReturn(javaProject2);
      when(javaProject2.getJavaProject()).thenReturn(javaProject2);

      when(workspaceRoot.getProject("project2")).thenReturn(project2);
      configuration2 = mockLaunchConfiguration();
      when(configuration2.getAttribute(eq(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME),
          anyString())).thenReturn("project2");
      doReturn(MajorVersion.TWO).when(dependencyManager).getProjectMajorVersion(project2);

      ILaunchConfigurationDialog dialog = mock(ILaunchConfigurationDialog.class);
      doAnswer(new SynchronousIRunnableContextExecutor()).when(dialog).run(anyBoolean(),
          anyBoolean(), any(IRunnableWithProgress.class));

      pipelineArgumentsTab = new PipelineArgumentsTab(workspaceRoot, dependencyManager);
      pipelineArgumentsTab.setLaunchConfigurationDialog(dialog);
      pipelineArgumentsTab.createControl(shellResource.getShell());
    }

    @Test
    public void testGetName() {
      Assert.assertEquals("Pipeline Arguments", pipelineArgumentsTab.getName());
    }

    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/3048
    @Test
    public void testValidatePage_doesNotClearErrorSetByChildren() {
      String errorMessage;
      Combo emailKey =
          CompositeUtil.findControlAfterLabel(shellResource.getShell(), Combo.class, "&Account:");
      if (emailKey.getText().isEmpty()) {
        errorMessage = "No Google account selected for this launch.";
      } else {
        Text serviceAccountKey = CompositeUtil.findControlAfterLabel(shellResource.getShell(),
            Text.class, "Service account key:");
        serviceAccountKey.setText("/non/existing/file");
        errorMessage = "/non/existing/file does not exist.";
      }
      assertEquals(errorMessage, pipelineArgumentsTab.getErrorMessage());

      pipelineArgumentsTab.isValid(configuration1);
      assertEquals(errorMessage, pipelineArgumentsTab.getErrorMessage());
    }

    @Test
    public void testSetDefaults() {
      ILaunchConfigurationWorkingCopy configuration = mock(ILaunchConfigurationWorkingCopy.class);
      pipelineArgumentsTab.setDefaults(configuration);
      // we don't set any defaults
      Mockito.verifyZeroInteractions(configuration);
    }

    @Test
    public void testReload() {
      assertTrue(pipelineArgumentsTab.reload(configuration1));
      assertFalse("cache should be ok", pipelineArgumentsTab.reload(configuration1)); //$NON-NLS-1$
      configuration1.setAttribute(PipelineConfigurationAttr.USER_OPTIONS_NAME.toString(), "bar"); //$NON-NLS-1$
      assertTrue("config changed, cache should be discarded", //$NON-NLS-1$
          pipelineArgumentsTab.reload(configuration1));
      assertTrue("different config; cache should be discarded", //$NON-NLS-1$
          pipelineArgumentsTab.reload(configuration2));
      assertTrue("config changed, cache should be discarded", //$NON-NLS-1$
          pipelineArgumentsTab.reload(configuration1));
    }

    @Test
    public void testInitializeFrom_accessibleProject() {
      pipelineArgumentsTab.initializeFrom(configuration1); // Should not throw NPE.
      // DirectPipelineRunner, DataflowPipelineRunner, BlockingDataflowPipelineRunner
      assertEquals(3, pipelineArgumentsTab.runnerButtons.size());
      assertTrue(pipelineArgumentsTab.defaultOptionsComponent.isEnabled());
      assertTrue(pipelineArgumentsTab.userOptionsSelector.isEnabled());

      ProjectUtils.waitForProjects(); // Suppress some non-terminated-job error logs
    }

    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2165
    @Test
    public void testInitializeFrom_noExceptionForNonAccessibleProject() {
      when(project1.isAccessible()).thenReturn(false);

      pipelineArgumentsTab.initializeFrom(configuration1); // Should not throw NPE.
      // verify tab contents are disabled
      assertEquals(0, pipelineArgumentsTab.runnerButtons.size());
      assertFalse(pipelineArgumentsTab.defaultOptionsComponent.isEnabled());
      assertFalse(pipelineArgumentsTab.userOptionsSelector.isEnabled());

      ProjectUtils.waitForProjects(); // Suppress some non-terminated-job error logs
    }

    @Test
    public void testIsValid_errorOnNonDataflowProject() {
      doReturn(null).when(dependencyManager).getProjectMajorVersion(project1);

      pipelineArgumentsTab.isValid(configuration1);
      assertEquals("Project is not configured for Dataflow",
          pipelineArgumentsTab.getErrorMessage());
    }

    @Test
    public void testIsValid_errorOnNonProject() {
      ILaunchConfiguration configuration = mock(ILaunchConfiguration.class);

      pipelineArgumentsTab.isValid(configuration);
      assertEquals("Project is not configured for Dataflow",
          pipelineArgumentsTab.getErrorMessage());
    }

    @Test
    public void testSuppressingUpdates() {
      IWorkspaceRoot workspaceRoot = mock(IWorkspaceRoot.class);
      when(workspaceRoot.getProject(anyString())).thenReturn(mock(IProject.class));
      ILaunchConfigurationDialog dialog = mock(ILaunchConfigurationDialog.class);

      PipelineArgumentsTab tab = new PipelineArgumentsTab(workspaceRoot, dependencyManager);
      tab.setLaunchConfigurationDialog(dialog);
      tab.suppressDialogUpdates = true;
      tab.dialogChangedListener.run();
      verifyZeroInteractions(dialog);
    }
  }

  private static ILaunchConfigurationWorkingCopy mockLaunchConfiguration() throws CoreException {
    final Map<String, Object> attributes = new HashMap<>();
    Answer<Object> getAttributes = new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String key = invocation.getArgumentAt(0, String.class);
        return attributes.containsKey(key) ? attributes.get(key) : invocation.getArgumentAt(1, Object.class);
      }};
    Answer<Void> setAttributes = new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        attributes.put(invocation.getArgumentAt(0, String.class),
            invocation.getArgumentAt(1, Object.class));
        return null;
      }};
    ILaunchConfigurationWorkingCopy launch = mock(ILaunchConfigurationWorkingCopy.class);
    doReturn(attributes).when(launch).getAttributes();
    doAnswer(getAttributes).when(launch).getAttribute(anyString(), anyString());
    doAnswer(getAttributes).when(launch).getAttribute(anyString(), anyInt());
    doAnswer(getAttributes).when(launch).getAttribute(anyString(), anyBoolean());
    doAnswer(getAttributes).when(launch)
        .getAttribute(anyString(), anyMapOf(String.class, String.class));
    doAnswer(setAttributes).when(launch).setAttribute(anyString(), anyString());
    doAnswer(setAttributes).when(launch).setAttribute(anyString(), anyInt());
    doAnswer(setAttributes).when(launch).setAttribute(anyString(), anyBoolean());
    doAnswer(setAttributes).when(launch)
        .setAttribute(anyString(), anyMapOf(String.class, String.class));
    return launch;
  }

  private static IProject mockProject(String projectName) {
    IProject project = mock(IProject.class, projectName);
    when(project.getName()).thenReturn(projectName);
    when(project.isAccessible()).thenReturn(true);
    when(project.isOpen()).thenReturn(true);
    return project;
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
          new Parameter(MajorVersion.ALL, "DirectRunner"));
    }

    @Rule public ShellTestResource shellResource = new ShellTestResource();

    private final Parameter testParameter;
    public RunButtonCheckedTest(Parameter parameter) {
      testParameter = parameter;
    }

    @Test
    public void assertRunnerButtonChecked() throws CoreException {
      ILaunchConfigurationDialog dialog = mock(ILaunchConfigurationDialog.class);
      Shell shell = shellResource.getShell();
      PipelineArgumentsTab tab = new PipelineArgumentsTab();
      tab.setLaunchConfigurationDialog(dialog);
      ScrolledComposite scrolledComposite =
          new ScrolledComposite(shellResource.getShell(), SWT.V_SCROLL | SWT.H_SCROLL);
      scrolledComposite.setExpandHorizontal(true);
      scrolledComposite.setExpandVertical(true);
      tab.createControl(scrolledComposite);

      PipelineLaunchConfiguration launchConfig = PipelineLaunchConfiguration
          .fromLaunchConfiguration(testParameter.majorVersion, mock(ILaunchConfiguration.class));
      tab.updateRunnerButtons(launchConfig);
      Button runnerButton = getCheckedRunnerButton(shell);
      assertNotNull(runnerButton);
      assertEquals(testParameter.expectedButtonText, runnerButton.getText());
      assertTrue(runnerButton.getSelection());

      // Should not throw IllegalStateException:
      // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2136
      tab.getSelectedRunner();
      testScrollbar(tab);
    }

    public void testScrollbar(PipelineArgumentsTab pipelineArgumentsTab) {
      Composite composite = pipelineArgumentsTab.internalComposite;
      assertNotNull(composite);
      Composite parent = pipelineArgumentsTab.internalComposite.getParent();
      if (parent instanceof ScrolledComposite) {
        pipelineArgumentsTab.handleLayoutChange();
        Point compositeSize = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        ScrolledComposite scrollComposite = (ScrolledComposite) parent;
        Point scrollSize = new Point(scrollComposite.getMinWidth(), scrollComposite.getMinHeight());
        if (compositeSize.equals(scrollSize)) {
          return;
        }
        fail("Scrollbar is not working");
      }
      fail("Did not find the Scroll composite");
    }

    private static Button getCheckedRunnerButton(Composite composite) {
      Group runnerGroup = CompositeUtil.findControl(composite, Group.class);
      assertEquals("Runner:", runnerGroup.getText());

      return (Button) CompositeUtil.findControl(runnerGroup,
          control -> control instanceof Button && ((Button) control).getSelection());
    }
  }

  /**
   * Intended to mock {@link IRunnableContext#run(boolean, boolean, IRunnableWithProgress)} to run
   * the given {@link IRunnableWithProgress} synchronously. The mock is incomplete and not general
   * in that it ignores other parameters and runs the code synchronously in the caller's thread.
   */
  private static class SynchronousIRunnableContextExecutor implements Answer<Void> {
    @Override
    public Void answer(InvocationOnMock invocation)
        throws InvocationTargetException, InterruptedException {
      IRunnableWithProgress runnable = invocation.getArgumentAt(2, IRunnableWithProgress.class);
      runnable.run(new NullProgressMonitor());
      return null;
    }
  }
}
