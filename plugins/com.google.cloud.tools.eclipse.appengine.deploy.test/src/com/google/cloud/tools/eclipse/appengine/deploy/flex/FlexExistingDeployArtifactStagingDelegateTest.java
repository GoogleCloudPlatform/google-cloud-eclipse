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
import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.service.prefs.BackingStoreException;

public class FlexExistingDeployArtifactStagingDelegateTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final FlexExistingArtifactDeployPreferences preferences =
      new FlexExistingArtifactDeployPreferences();

  private IPath stagingDirectory;
  private IPath appEngineDirectory;
  private File deployArtifact;

  @Before
  public void setUp() throws IOException, BackingStoreException {
    stagingDirectory = new Path(tempFolder.getRoot().getAbsolutePath()).append("stagingDirectory");
    appEngineDirectory = new Path(tempFolder.newFolder("appEngineDirectory").getAbsolutePath());
    appEngineDirectory.append("app.yaml").toFile().createNewFile();
    deployArtifact = tempFolder.newFile("my-app.war");
    preferences.setDeployArtifactPath(deployArtifact.getAbsolutePath());
    preferences.save();
  }

  @After
  public void tearDown() throws BackingStoreException {
    preferences.resetToDefaults();
    preferences.save();
  }

  @Test
  public void testStage() {
    StagingDelegate delegate = new FlexExistingDeployArtifactStagingDelegate(appEngineDirectory);
    IStatus status = delegate.stage(null, stagingDirectory, null, null, null, null);

    assertTrue(stagingDirectory.append("my-app.war").toFile().exists());
    assertTrue(stagingDirectory.append("app.yaml").toFile().exists());
    assertTrue(status.isOK());
  }
}
