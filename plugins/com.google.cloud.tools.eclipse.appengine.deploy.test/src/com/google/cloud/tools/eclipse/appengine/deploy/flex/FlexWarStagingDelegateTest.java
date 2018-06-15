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

import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexWarFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.junit.Rule;
import org.junit.Test;

public class FlexWarStagingDelegateTest {

  @Rule public TestProjectCreator projectCreator =
      new TestProjectCreator().withFacets(JavaFacet.VERSION_1_7);

  private IProject project;
  private IPath safeWorkDirectory;
  private IPath stagingDirectory;
  private IPath appEngineDirectory;

  private void setUpProject(IProjectFacetVersion... facetVersions) {
    project = projectCreator.withFacets(facetVersions).getProject();
    safeWorkDirectory = project.getFolder("safe-work-directory").getLocation();
    stagingDirectory = project.getFolder("staging-result").getLocation();
    appEngineDirectory = project.getFolder("src/main/appengine").getLocation();
  }

  @Test
  public void testStage() {
    setUpProject(WebFacetUtils.WEB_25, AppEngineFlexWarFacet.FACET_VERSION);
    StagingDelegate delegate = new FlexWarStagingDelegate(project, appEngineDirectory);
    IStatus status = delegate.stage(stagingDirectory, safeWorkDirectory,
        null, null, new NullProgressMonitor());

    assertTrue(getStatusAsString(status), status.isOK());
    assertTrue(stagingDirectory.append("app-to-deploy.war").toFile().exists());
    assertTrue(stagingDirectory.append("app.yaml").toFile().exists());
  }

  @Test
  public void testStage_errorStatusReported() {
    setUpProject();
    StagingDelegate delegate = new FlexWarStagingDelegate(project, appEngineDirectory);
    IStatus status = delegate.stage(stagingDirectory, safeWorkDirectory,
        null, null, new NullProgressMonitor());

    assertFalse(getStatusAsString(status), status.isOK());
    assertEquals(
        "Staging failed. Check the error message in the Console View.", status.getMessage());
  }

  private static String getStatusAsString(IStatus status) {
    String stringStatus = status.getSeverity() + ": " + status.getMessage();
    if (status.getException() != null) {
      stringStatus += "\n==== start of IStatus exception stack trace ====\n";
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter, true);
      status.getException().printStackTrace(printWriter);
      stringStatus += stringWriter.toString();
      stringStatus += "==== end of IStatus exception stack trace ====";
    }
    return stringStatus;
  }

  @Test
  public void testGetOptionalConfigurationFilesDirectory() {
    setUpProject();
    StagingDelegate delegate = new FlexWarStagingDelegate(project, appEngineDirectory);

    assertEquals(appEngineDirectory, delegate.getOptionalConfigurationFilesDirectory());
  }
}
