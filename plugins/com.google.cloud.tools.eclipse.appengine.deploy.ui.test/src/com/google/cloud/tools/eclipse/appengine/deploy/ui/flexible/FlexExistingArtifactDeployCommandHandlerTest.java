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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.deploy.flex.FlexExistingArtifactDeployPreferences;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.ByteArrayInputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class FlexExistingArtifactDeployCommandHandlerTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  private static final FlexExistingArtifactDeployCommandHandler handler =
      new FlexExistingArtifactDeployCommandHandler();

  private static final FlexExistingArtifactDeployPreferences preferences =
      new FlexExistingArtifactDeployPreferences();

  @After
  public void tearDown() throws BackingStoreException {
    preferences.resetToDefaults();
    preferences.save();
  }

  @Test
  public void testGetStagingDelegate_relativeAppYamlPathDoesNotExist()
      throws BackingStoreException {
    try {
      setUpDeployPreferences(true /* asRelativePath */);
      handler.getStagingDelegate(projectCreator.getProject());
      fail();
    } catch (CoreException e) {
      assertThat(e.getMessage(), endsWith("app.yaml does not exist."));
    }
  }

  @Test
  public void testGetStagingDelegate_relativeJarPathDoesNotExist() throws BackingStoreException {
    try {
      setUpDeployPreferences(true /* asRelativePath */);
      createFileInProject("app.yaml");

      handler.getStagingDelegate(projectCreator.getProject());
      fail();
    } catch (CoreException e) {
      assertThat(e.getMessage(), endsWith("my-app.jar does not exist."));
    }
  }

  @Test
  public void testGetStagingDelegate_relativePathsNoException()
      throws CoreException, BackingStoreException {
    setUpDeployPreferences(true /* asRelativePath */);
    createFileInProject("app.yaml");
    createFileInProject("my-app.jar");
    handler.getStagingDelegate(projectCreator.getProject());
  }

  @Test
  public void testGetStagingDelegate_absoluteAppYamlPathDoesNotExist()
      throws BackingStoreException {
    try {
      setUpDeployPreferences(false /* asRelativePath */);
      handler.getStagingDelegate(projectCreator.getProject());
      fail();
    } catch (CoreException e) {
      assertThat(e.getMessage(), endsWith("app.yaml does not exist."));
    }
  }

  @Test
  public void testGetStagingDelegate_absoluteJarPathDoesNotExist() throws BackingStoreException {
    try {
      setUpDeployPreferences(false /* asRelativePath */);
      createFileInProject("app.yaml");

      handler.getStagingDelegate(projectCreator.getProject());
      fail();
    } catch (CoreException e) {
      assertThat(e.getMessage(), endsWith("my-app.jar does not exist."));
    }
  }

  @Test
  public void testGetStagingDelegate_absolautePathsNoException()
      throws CoreException, BackingStoreException {
    setUpDeployPreferences(false /* asRelativePath */);
    createFileInProject("app.yaml");
    createFileInProject("my-app.jar");
    handler.getStagingDelegate(projectCreator.getProject());
  }

  private IPath createFileInProject(String filename) throws CoreException {
    IFile file = projectCreator.getProject().getFile(filename);
    file.create(new ByteArrayInputStream(new byte[0]), true, null);
    return file.getLocation();
  }

  private void setUpDeployPreferences(boolean asRelativePath) throws BackingStoreException {
    IPath workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    IPath appYaml = projectCreator.getProject().getFile("app.yaml").getLocation();
    IPath jar = projectCreator.getProject().getFile("my-app.jar").getLocation();

    if (asRelativePath) {
      appYaml = appYaml.makeRelativeTo(workspace);
      jar = jar.makeRelativeTo(workspace);
    }

    preferences.setAppYamlPath(appYaml.toString());
    preferences.setDeployArtifactPath(jar.toString());
    preferences.save();
  }
}
