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

package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AppEngineProjectDeployerTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private IPath stagingDirectory;
  private IPath optionalConfigurationFilesDirectory;

  @Before
  public void setUp() throws IOException {
    stagingDirectory = new Path(tempFolder.getRoot().toString());
    tempFolder.newFile("app.yaml");

    optionalConfigurationFilesDirectory = stagingDirectory.append("WEB-INF/appengine-generated");
    tempFolder.newFolder("WEB-INF", "appengine-generated");
  }

  @Test
  public void testConfigFilesFile() {
    assertEquals(5, AppEngineProjectDeployer.APP_ENGINE_CONFIG_FILES.size());
    assertTrue(AppEngineProjectDeployer.APP_ENGINE_CONFIG_FILES.contains("cron.yaml"));
    assertTrue(AppEngineProjectDeployer.APP_ENGINE_CONFIG_FILES.contains("dispatch.yaml"));
    assertTrue(AppEngineProjectDeployer.APP_ENGINE_CONFIG_FILES.contains("dos.yaml"));
    assertTrue(AppEngineProjectDeployer.APP_ENGINE_CONFIG_FILES.contains("index.yaml"));
    assertTrue(AppEngineProjectDeployer.APP_ENGINE_CONFIG_FILES.contains("queue.yaml"));
  }

  @Test
  public void testComputeDeployables_nullConfigDirectoryPath() {
    List<File> deployables = AppEngineProjectDeployer.computeDeployables(
        stagingDirectory, null /* don't include files */);
    assertEquals(1, deployables.size());
    assertEquals(stagingDirectory.append("app.yaml").toFile(), deployables.get(0));
  }

  @Test
  public void testComputeDeployables_noConfigFilesInConfigDirectory() {
    List<File> deployables = AppEngineProjectDeployer.computeDeployables(
        stagingDirectory, optionalConfigurationFilesDirectory);
    assertEquals(1, deployables.size());
    assertEquals(stagingDirectory.append("app.yaml").toFile(), deployables.get(0));
  }

  @Test
  public void testComputeDeployables_configFilesExistButNullConfigDirectoryPath()
      throws IOException {
    createFakeConfigFiles();

    List<File> deployables = AppEngineProjectDeployer.computeDeployables(
        stagingDirectory, null /* don't include files */);
    assertEquals(1, deployables.size());
    assertEquals(stagingDirectory.append("app.yaml").toFile(), deployables.get(0));
  }

  @Test
  public void testComputeDeployables_configFilesExist() throws IOException {
    createFakeConfigFiles();

    List<File> deployables = AppEngineProjectDeployer.computeDeployables(
        stagingDirectory, optionalConfigurationFilesDirectory);
    assertEquals(6, deployables.size());
    assertTrue(deployables.contains(stagingDirectory.append("app.yaml").toFile()));
    assertTrue(deployables.contains(stagingDirectory.append(
        "WEB-INF/appengine-generated/cron.yaml").toFile()));
    assertTrue(deployables.contains(stagingDirectory.append(
        "WEB-INF/appengine-generated/index.yaml").toFile()));
    assertTrue(deployables.contains(stagingDirectory.append(
        "WEB-INF/appengine-generated/dispatch.yaml").toFile()));
    assertTrue(deployables.contains(stagingDirectory.append(
        "WEB-INF/appengine-generated/dos.yaml").toFile()));
    assertTrue(deployables.contains(stagingDirectory.append(
        "WEB-INF/appengine-generated/queue.yaml").toFile()));
  }

  private void createFakeConfigFiles() throws IOException {
    tempFolder.newFile("WEB-INF/appengine-generated/cron.yaml");
    tempFolder.newFile("WEB-INF/appengine-generated/index.yaml");
    tempFolder.newFile("WEB-INF/appengine-generated/dispatch.yaml");
    tempFolder.newFile("WEB-INF/appengine-generated/dos.yaml");
    tempFolder.newFile("WEB-INF/appengine-generated/queue.yaml");
  }
}
