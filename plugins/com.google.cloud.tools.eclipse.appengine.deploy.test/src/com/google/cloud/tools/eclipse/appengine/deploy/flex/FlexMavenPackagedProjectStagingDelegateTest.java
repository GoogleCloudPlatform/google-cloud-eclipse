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

package com.google.cloud.tools.eclipse.appengine.deploy.flex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.junit.Rule;
import org.junit.Test;

public class FlexMavenPackagedProjectStagingDelegateTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator().withFacets(
      JavaFacet.VERSION_1_7);

  @Test
  public void testJreContainerPath() throws CoreException {
    IPath jreContainerPath =
        FlexMavenPackagedProjectStagingDelegate.getJreContainerPath(projectCreator.getProject());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER/"
        + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.7",
        jreContainerPath.toString());
  }

  @Test
  public void testCreateMavenPackagingLaunchConfiguration() throws CoreException {
    IProject project = projectCreator.getProject();

    ILaunchConfiguration launchConfig =
        FlexMavenPackagedProjectStagingDelegate.createMavenPackagingLaunchConfiguration(project);

    boolean privateConfig = launchConfig.getAttribute(ILaunchManager.ATTR_PRIVATE, false);
    assertTrue(privateConfig);

    boolean launchInBackground = launchConfig.getAttribute(
        "org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND", false);
    assertTrue(launchInBackground);

    String jreContainerPath = launchConfig.getAttribute(
        IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, "");
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER/"
        + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.7", jreContainerPath);

    String pomDirectory = launchConfig.getAttribute(MavenLaunchConstants.ATTR_POM_DIR, "");
    assertEquals(project.getLocation().toString(), pomDirectory);

    String mavenGoals = launchConfig.getAttribute(MavenLaunchConstants.ATTR_GOALS, "");
    assertEquals("package", mavenGoals);
  }

  @Test
  public void testWaitUntilLaunchTerminates_cancaledMonitor()
      throws DebugException, InterruptedException {
    IProgressMonitor monitor = new NullProgressMonitor();
    monitor.setCanceled(true);
    assertTrue(monitor.isCanceled());

    ILaunch launch = mock(ILaunch.class);
    assertFalse(launch.isTerminated());

    boolean normalExit = FlexMavenPackagedProjectStagingDelegate.waitUntilLaunchTerminates(
        launch, monitor);
    assertFalse(normalExit);
    verify(launch).terminate();
  }

  @Test
  public void testWaitUntilLaunchTerminates_normalExit()
      throws DebugException, InterruptedException {
    IProcess process = mock(IProcess.class);
    when(process.getExitValue()).thenReturn(0);

    ILaunch launch = mock(ILaunch.class);
    when(launch.isTerminated()).thenReturn(true);
    when(launch.getProcesses()).thenReturn(new IProcess[] {process, process});

    boolean normalExit = FlexMavenPackagedProjectStagingDelegate.waitUntilLaunchTerminates(
        launch, new NullProgressMonitor());
    assertTrue(normalExit);
  }

  @Test
  public void testWaitUntilLaunchTerminates_atLeastOneNonZeroExit()
      throws DebugException, InterruptedException {
    IProcess process = mock(IProcess.class);
    when(process.getExitValue()).thenReturn(0);
    IProcess nonZeroExitProcess = mock(IProcess.class);
    when(nonZeroExitProcess.getExitValue()).thenReturn(1);

    ILaunch launch = mock(ILaunch.class);
    when(launch.isTerminated()).thenReturn(true);
    when(launch.getProcesses()).thenReturn(new IProcess[] {process, nonZeroExitProcess});

    boolean normalExit = FlexMavenPackagedProjectStagingDelegate.waitUntilLaunchTerminates(
        launch, new NullProgressMonitor());
    assertFalse(normalExit);
  }
}
