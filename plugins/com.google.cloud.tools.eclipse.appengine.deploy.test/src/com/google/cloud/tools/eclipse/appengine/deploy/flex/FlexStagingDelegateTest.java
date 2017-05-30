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
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FlexStagingDelegateTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator().withFacetVersions(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, AppEngineFlexFacet.FACET_VERSION);

  private IProject project;
  private IPath safeWorkDirectory;
  private IPath stagingDirectory;
  private IPath appEngineDirectory;

  @Before
  public void setUp() {
    project = projectCreator.getProject();
    safeWorkDirectory = project.getFolder("safe-work-directory").getLocation();
    stagingDirectory = project.getFolder("staging-result").getLocation();
    appEngineDirectory = project.getFolder("src/main/appengine").getLocation();
  }

  @Test
  public void testStage() throws CoreException {
    StagingDelegate delegate = new FlexStagingDelegate(appEngineDirectory);
    IStatus status = delegate.stage(project, stagingDirectory, safeWorkDirectory, 
        null /* cloudSdk */, new NullProgressMonitor());

    assertTrue(stagingDirectory.append("app-to-deploy.war").toFile().exists());
    assertTrue(stagingDirectory.append("app.yaml").toFile().exists());
    assertTrue(status.isOK());
  }

  @Test
  public void testGetOptionalConfigurationFilesDirectory() {
    StagingDelegate delegate = new FlexStagingDelegate(appEngineDirectory);

    assertEquals(appEngineDirectory, delegate.getOptionalConfigurationFilesDirectory());
  }
}
