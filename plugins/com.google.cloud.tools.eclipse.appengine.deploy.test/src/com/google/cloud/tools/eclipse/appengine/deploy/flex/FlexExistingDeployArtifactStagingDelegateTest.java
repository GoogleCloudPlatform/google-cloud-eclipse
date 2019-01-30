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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FlexExistingDeployArtifactStagingDelegateTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private IPath stagingDirectory;
  private IPath appEngineDirectory;

  @Before
  public void setUp() throws  CoreException {
    IProject project = projectCreator.getProject();
    stagingDirectory = project.getFolder("stagingDirectory").getLocation();

    IFolder appEngineFolder = project.getFolder("appEngineDirectory");
    appEngineFolder.create(true, true, null);
    String appYaml = "runtime: java\n" + 
        "env: flex\n";
    appEngineFolder.getFile("app.yaml")
        .create(new ByteArrayInputStream(appYaml.getBytes(Charsets.UTF_8)), true, null);
    appEngineDirectory = appEngineFolder.getLocation();

  }

  @Test
  public void testStage_artifactOutOfWorkspace() throws IOException {
    IPath deployArtifact = new Path(tempFolder.newFile("not-in-workspace.war").getAbsolutePath());

    StagingDelegate delegate = new FlexExistingDeployArtifactStagingDelegate(
        deployArtifact, appEngineDirectory);
    IStatus status = delegate.stage(stagingDirectory, null, null, null, null);

    assertTrue(status.toString(), status.isOK());
    assertTrue(stagingDirectory.append("not-in-workspace.war").toFile().exists());
    assertTrue(stagingDirectory.append("app.yaml").toFile().exists());
  }

  @Test
  public void testStage_artifactInWorkspace() throws CoreException {
    IPath deployArtifact = createFileInProject("in-workspace.war");

    StagingDelegate delegate = new FlexExistingDeployArtifactStagingDelegate(
        deployArtifact, appEngineDirectory);
    IStatus status = delegate.stage(stagingDirectory, null, null, null, null);

    assertTrue(status.toString(), status.isOK());
    assertTrue(stagingDirectory.append("in-workspace.war").toFile().exists());
    assertTrue(stagingDirectory.append("app.yaml").toFile().exists());
  }

  @Test
  public void testSchedulingRule_artifactOutOfWorkspace() throws IOException {
    IPath deployArtifact = new Path(tempFolder.newFile("not-in-workspace.war").getAbsolutePath());

    StagingDelegate delegate = new FlexExistingDeployArtifactStagingDelegate(
        deployArtifact, appEngineDirectory);
    assertNull(delegate.getSchedulingRule());
  }

  @Test
  public void testSchedulingRule_artifactInWorkspace() throws CoreException {
    IPath deployArtifact = createFileInProject("in-workspace.war");

    StagingDelegate delegate = new FlexExistingDeployArtifactStagingDelegate(
        deployArtifact, appEngineDirectory);

    ISchedulingRule rule = delegate.getSchedulingRule();
    assertTrue(rule instanceof IFile);
    IFile file = (IFile) rule;
    assertTrue(file.exists());
  }

  private IPath createFileInProject(String filename) throws CoreException {
    IFile file = projectCreator.getProject().getFile(filename);
    file.create(new ByteArrayInputStream(new byte[0]), true, null);
    return file.getLocation();
  }
}
