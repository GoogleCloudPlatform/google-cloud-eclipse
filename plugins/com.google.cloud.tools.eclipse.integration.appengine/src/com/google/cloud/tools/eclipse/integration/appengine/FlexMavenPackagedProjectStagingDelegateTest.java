/*
 * Copyright 2017 Google LLC
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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexMavenPackagedProjectStagingDelegate;
import com.google.cloud.tools.eclipse.test.util.project.JavaRuntimeUtils;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.io.IOException;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class FlexMavenPackagedProjectStagingDelegateTest {

  // flaking on Photon: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/pull/3256
  @Ignore
  @Test
  public void testStage_springBoot() throws IOException, CoreException {
    Assume.assumeTrue("Only for JavaSE-8", JavaRuntimeUtils.hasJavaSE8());

    Map<String, IProject> projects =
        ProjectUtils.importProjects(
            getClass(), "test-projects/spring-boot-test.zip", false /* checkBuildErrors */, null);
    assertEquals(1, projects.size());

    IProject project = projects.values().iterator().next();
    IPath safeWorkDirectory = project.getFolder("safe-work-directory").getLocation();
    IPath stagingDirectory = project.getFolder("staging-result").getLocation();
    IPath appEngineDirectory = project.getFolder("src/main/appengine").getLocation();

    StagingDelegate delegate =
        new FlexMavenPackagedProjectStagingDelegate(project, appEngineDirectory);
    IStatus status = delegate.stage(stagingDirectory, safeWorkDirectory,
        null, null, new NullProgressMonitor());
    assertTrue(status.isOK());

    project.refreshLocal(IResource.DEPTH_INFINITE, null);
    assertTrue(project.getFolder("target").exists());
    assertTrue(project.getFile("target/customized-final-artifact-name.jar").exists());
    assertTrue(stagingDirectory.append("app.yaml").toFile().exists());
    assertTrue(stagingDirectory.append("customized-final-artifact-name.jar").toFile().exists());

    project.delete(true, null);
  }
}
