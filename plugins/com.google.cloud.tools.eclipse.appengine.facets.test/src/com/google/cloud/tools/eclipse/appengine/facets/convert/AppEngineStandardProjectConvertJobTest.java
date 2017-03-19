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

package com.google.cloud.tools.eclipse.appengine.facets.convert;

import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineStandardProjectConvertJobTest {
  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Rule
  public final TestProjectCreator projectCreator =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7);

  @Test
  public void testAppEngineFacetAdded() throws CoreException, InterruptedException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(projectCreator.getProject());
    Job convertJob = new AppEngineStandardProjectConvertJob(facetedProject);
    convertJob.schedule();
    convertJob.join();
    assertTrue(AppEngineStandardFacet.hasAppEngineFacet(facetedProject));
  }
}
