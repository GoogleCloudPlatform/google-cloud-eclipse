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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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

  @Before
  public void setUp() throws IOException {
    stagingDirectory = new Path(tempFolder.getRoot().toString());
    tempFolder.newFile("app.yaml");
  }

  @Test
  public void testConfigFilesFile() {
    assertArrayEquals(
        new String[] {"cron.yaml", "dispatch.yaml", "dos.yaml", "index.yaml", "queue.yaml"},
        AppEngineProjectDeployer.APP_ENGINE_CONFIG_FILES.toArray(new String[0]));
  }

  @Test
  public void testComputeDeployables_noConfigFilesAndNoConfigDeploy() {
    List<File> deployables =
        AppEngineProjectDeployer.computeDeployables(stagingDirectory, false /* configDeploy */);
    assertEquals(1, deployables.size());
    assertEquals(stagingDirectory.append("app.yaml").toFile(), deployables.get(0));
  }

  @Test
  public void testComputeDeployables_noConfigFilesAndConfigDeploy() {
    List<File> deployables =
        AppEngineProjectDeployer.computeDeployables(stagingDirectory, true /* configDeploy */);
    assertEquals(1, deployables.size());
    assertEquals(stagingDirectory.append("app.yaml").toFile(), deployables.get(0));
  }

  @Test
  public void testComputeDeployables_configFilesExistAndNoConfigDeploy() throws IOException {
    createFakeConfigFiles();

    List<File> deployables =
        AppEngineProjectDeployer.computeDeployables(stagingDirectory, false /* configDeploy */);
    assertEquals(1, deployables.size());
    assertEquals(stagingDirectory.append("app.yaml").toFile(), deployables.get(0));
  }

  @Test
  public void testComputeDeployables_configFilesExistAndConfigDeploy() throws IOException {
    createFakeConfigFiles();

    List<File> deployables =
        AppEngineProjectDeployer.computeDeployables(stagingDirectory, true /* configDeploy */);

    File[] expectedFiles = new File[] {
        stagingDirectory.append("app.yaml").toFile(),
        stagingDirectory.append("WEB-INF/appengine-generated/cron.yaml").toFile(),
        stagingDirectory.append("WEB-INF/appengine-generated/dispatch.yaml").toFile(),
        stagingDirectory.append("WEB-INF/appengine-generated/dos.yaml").toFile(),
        stagingDirectory.append("WEB-INF/appengine-generated/index.yaml").toFile(),
        stagingDirectory.append("WEB-INF/appengine-generated/queue.yaml").toFile() };
    assertArrayEquals(expectedFiles, deployables.toArray(new File[0]));
  }

  private void createFakeConfigFiles() throws IOException {
    tempFolder.newFolder("WEB-INF", "appengine-generated");
    tempFolder.newFile("WEB-INF/appengine-generated/cron.yaml");
    tempFolder.newFile("WEB-INF/appengine-generated/index.yaml");
    tempFolder.newFile("WEB-INF/appengine-generated/dispatch.yaml");
    tempFolder.newFile("WEB-INF/appengine-generated/dos.yaml");
    tempFolder.newFile("WEB-INF/appengine-generated/queue.yaml");
  }
}
