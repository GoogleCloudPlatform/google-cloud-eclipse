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

package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.api.deploy.AppEngineStandardStaging;
import com.google.cloud.tools.appengine.cloudsdk.AppCfg;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.process.LegacyProcessHandler;
import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.util.CloudSdkProcessWrapper;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.ui.console.MessageConsoleStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StandardStagingDelegateTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator().withFacets(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, AppEngineStandardFacet.JRE7);

  @Mock private CloudSdkProcessWrapper cloudSdkWrapper;

  private int cloudSdkExitCode = -1;
  private IProject project;
  private IPath safeWorkDirectory;
  private IPath stagingDirectory;

  @Before
  public void setUp() throws CloudSdkNotFoundException {
    project = projectCreator.getProject();
    safeWorkDirectory = project.getFolder("safe-work-directory").getLocation();
    stagingDirectory = project.getFolder("staging-result").getLocation();

    CloudSdk cloudSdk = new CloudSdk.Builder().build();
    LegacyProcessHandler processHandler = LegacyProcessHandler.builder()
        .addStdOutLineListener(line -> { System.out.println("    [Cloud SDK] " + line); })
        .addStdErrLineListener(line -> { System.out.println("    [Cloud SDK] " + line); })
        .setExitListener(exitCode -> { cloudSdkExitCode = exitCode; })
        .build();
    AppEngineStandardStaging staging = AppCfg.builder(cloudSdk).build().newStaging(processHandler);

    when(cloudSdkWrapper.getAppEngineStandardStaging(
        any(Path.class), any(MessageConsoleStream.class), any(MessageConsoleStream.class)))
        .thenReturn(staging);
  }

  @After
  public void tearDown() {
    assertEquals(0, cloudSdkExitCode);
  }

  @Test
  public void testStage() {
    StagingDelegate delegate = new StandardStagingDelegate(project, null, cloudSdkWrapper);
    delegate.stage(stagingDirectory, safeWorkDirectory, null, null,
        new NullProgressMonitor());

    assertTrue(stagingDirectory.append("WEB-INF").toFile().exists());
    assertTrue(stagingDirectory.append("WEB-INF/appengine-generated").toFile().exists());
    assertTrue(stagingDirectory.append("META-INF").toFile().exists());
    assertTrue(stagingDirectory.append("app.yaml").toFile().exists());
  }

  @Test
  public void testGetOptionalConfigurationFilesDirectory() {
    StagingDelegate delegate = new StandardStagingDelegate(project, null, cloudSdkWrapper);
    delegate.stage(stagingDirectory, safeWorkDirectory, null, null,
        new NullProgressMonitor());

    assertEquals(stagingDirectory.append("WEB-INF/appengine-generated"),
        delegate.getOptionalConfigurationFilesDirectory());
  }

  @Test
  public void testSetJavaHome() throws CloudSdkNotFoundException {
    Path javaHome = Paths.get("/some/path");
    StagingDelegate delegate = new StandardStagingDelegate(project, javaHome, cloudSdkWrapper);
    delegate.stage(stagingDirectory, safeWorkDirectory, null, null,
        new NullProgressMonitor());

    verify(cloudSdkWrapper).getAppEngineStandardStaging(
        eq(javaHome), any(MessageConsoleStream.class), any(MessageConsoleStream.class));
  }
}
