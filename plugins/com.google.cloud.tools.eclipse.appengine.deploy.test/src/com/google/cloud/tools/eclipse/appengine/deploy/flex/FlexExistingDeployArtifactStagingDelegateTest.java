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

import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.ByteArrayInputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FlexExistingDeployArtifactStagingDelegateTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  private IPath stagingDirectory;
  private IPath appEngineDirectory;
  private IFile deployArtifact;

  @Before
  public void setUp() throws  CoreException {
    IProject project = projectCreator.getProject();
    stagingDirectory = project.getFolder("stagingDirectory").getLocation();

    IFolder appEngineFolder = project.getFolder("appEngineDirectory");
    appEngineFolder.create(true, true, null);
    appEngineFolder.getFile("app.yaml").create(new ByteArrayInputStream(new byte[0]), true, null);
    appEngineDirectory = appEngineFolder.getLocation();

    deployArtifact = project.getFile("my-app.war");
    deployArtifact.create(new ByteArrayInputStream(new byte[0]), true, null);
  }

  @Test
  public void testStage() {
    StagingDelegate delegate = new FlexExistingDeployArtifactStagingDelegate(
        deployArtifact, appEngineDirectory);
    IStatus status = delegate.stage(stagingDirectory, null, null, null, null);

    assertTrue(stagingDirectory.append("my-app.war").toFile().exists());
    assertTrue(stagingDirectory.append("app.yaml").toFile().exists());
    assertTrue(status.isOK());
  }
}
