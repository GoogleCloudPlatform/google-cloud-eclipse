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

package com.google.cloud.tools.eclipse.appengine.facets.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.ui.AppEngineStandardProjectConvertCommandHandler.MessageDialogWrapper;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.internal.builder.DependencyGraphImpl;
import org.eclipse.wst.common.componentcore.internal.builder.IDependencyGraph;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineStandardProjectConvertCommandHandlerTest {
  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Rule public final TestProjectCreator projectCreator = new TestProjectCreator();

  @Mock private MessageDialogWrapper mockDialogWrapper;

  private IFacetedProject facetedProject;

  private AppEngineStandardProjectConvertCommandHandler commandHandler =
      new AppEngineStandardProjectConvertCommandHandler();

  @Before
  public void setUp() throws CoreException, OperationCanceledException, InterruptedException {
    facetedProject = ProjectFacetsManager.create(projectCreator.getProject());

    // Workaround deadlock bug described in Eclipse bug (https://bugs.eclipse.org/511793).
    // There are graph update jobs triggered by the completion of the CreateProjectOperation
    // above (from resource notifications) and from other resource changes from modifying the
    // project facets. So we force the dependency graph to defer updates.
    IDependencyGraph.INSTANCE.preUpdate();
    Job.getJobManager().join(DependencyGraphImpl.GRAPH_UPDATE_JOB_FAMILY, null);
  }

  @After
  public void tearDown() {
    IDependencyGraph.INSTANCE.postUpdate();
  }

  @Test
  public void testCheckFacetCompatibility_noFacetsInstalled() {
    assertFalse(facetedProject.hasProjectFacet(JavaFacet.FACET));
    assertFalse(facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET));

    assertTrue(commandHandler.checkFacetCompatibility(facetedProject, mockDialogWrapper));
    verify(mockDialogWrapper, never()).openInformation(anyString(), anyString());
  }

  @Test
  public void testCheckFacetCompatibility_java1_7FacetIsCompatible() throws CoreException {
    facetedProject.installProjectFacet(JavaFacet.VERSION_1_7, null, null);
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.VERSION_1_7));

    assertTrue(commandHandler.checkFacetCompatibility(facetedProject, mockDialogWrapper));
    verify(mockDialogWrapper, never()).openInformation(anyString(), anyString());
  }

  @Test
  public void testCheckFacetCompatibility_web2_5FacetIsCompatible() throws CoreException {
    facetedProject.installProjectFacet(JavaFacet.VERSION_1_7, null, null);
    facetedProject.installProjectFacet(WebFacetUtils.WEB_25, null, null);
    assertTrue(facetedProject.hasProjectFacet(WebFacetUtils.WEB_25));

    assertTrue(commandHandler.checkFacetCompatibility(facetedProject, mockDialogWrapper));
    verify(mockDialogWrapper, never()).openInformation(anyString(), anyString());
  }

  @Test
  public void testCheckFacetCompatibility_java1_6FacetIsIncompatible() throws CoreException {
    facetedProject.installProjectFacet(JavaFacet.VERSION_1_6, null, null);
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.VERSION_1_6));

    assertFalse(commandHandler.checkFacetCompatibility(facetedProject, mockDialogWrapper));
    verify(mockDialogWrapper).openInformation(anyString(), anyString());
  }

  @Test
  public void testCheckFacetCompatibility_java1_8FacetIsIncompatible() throws CoreException {
    Assume.assumeTrue("AppEngine Standard Java 8 runtime support is not present",
        AppEngineStandardFacet.FACET_VERSION.conflictsWith(JavaFacet.VERSION_1_8));
    facetedProject.installProjectFacet(JavaFacet.VERSION_1_8, null, null);
    assertTrue(facetedProject.hasProjectFacet(JavaFacet.VERSION_1_8));

    assertFalse(commandHandler.checkFacetCompatibility(facetedProject, mockDialogWrapper));
    verify(mockDialogWrapper).openInformation(anyString(), anyString());
  }

  @Test
  public void testCheckFacetCompatibility_web2_4FacetIsIncompatible() throws CoreException {
    facetedProject.installProjectFacet(JavaFacet.VERSION_1_7, null, null);
    facetedProject.installProjectFacet(WebFacetUtils.WEB_24, null, null);
    assertTrue(facetedProject.hasProjectFacet(WebFacetUtils.WEB_24));

    assertFalse(commandHandler.checkFacetCompatibility(facetedProject, mockDialogWrapper));
    verify(mockDialogWrapper).openInformation(anyString(), anyString());
  }

  @Test
  public void testCheckFacetCompatibility_web3_0FacetIsIncompatible() throws CoreException {
    Assume.assumeTrue("AppEngine Standard Java 8 runtime support is not present",
        AppEngineStandardFacet.FACET_VERSION.conflictsWith(WebFacetUtils.WEB_30));
    facetedProject.installProjectFacet(JavaFacet.VERSION_1_7, null, null);
    facetedProject.installProjectFacet(WebFacetUtils.WEB_30, null, null);
    assertTrue(facetedProject.hasProjectFacet(WebFacetUtils.WEB_30));

    assertFalse(commandHandler.checkFacetCompatibility(facetedProject, mockDialogWrapper));
    verify(mockDialogWrapper).openInformation(anyString(), anyString());
  }
}
