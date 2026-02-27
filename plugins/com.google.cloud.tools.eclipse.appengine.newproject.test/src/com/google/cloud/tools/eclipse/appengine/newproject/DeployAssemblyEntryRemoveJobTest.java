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

package com.google.cloud.tools.eclipse.appengine.newproject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.junit.Rule;
import org.junit.Test;

public class DeployAssemblyEntryRemoveJobTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator()
      .withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25);

  @Test
  public void testRun_entryRemoved() throws OperationCanceledException, InterruptedException {
    IProject project = projectCreator.getProject();
    ProjectUtils.waitForProjects(project);
    assertTrue(hasSourcePathInDeployAssembly(project, new Path("src")));

    Job job = new DeployAssemblyEntryRemoveJob(project, new Path("src"));
    job.schedule();
    job.join();
    assertFalse(hasSourcePathInDeployAssembly(project, new Path("src")));
  }

  static boolean hasSourcePathInDeployAssembly(IProject project, IPath sourcePath) {
    StructureEdit core = StructureEdit.getStructureEditForRead(project);
    try {
      WorkbenchComponent component = core.getComponent();
      return component.findResourcesBySourcePath(sourcePath, 0).length > 0;
    } finally {
      core.dispose();
    }
  }

}
