/*
 * Copyright 2016 Google Inc.
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
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.api.deploy.AppEngineStandardStaging;
import com.google.cloud.tools.appengine.cloudsdk.AppCfg;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.process.LegacyProcessHandler;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessHandler;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkStagingHelperTest {

  private static final String APP_YAML = "runtime: java\nenv: flex";
  private static final String APPENGINE_WEB_XML =
      "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
      + "<threadsafe>true</threadsafe></appengine-web-app>";
  private static final String WEB_XML = "<web-app/>";

  private static final String CRON_XML = "<cronentries/>";
  private static final String DATASTORE_INDEXES_XML =
      "<datastore-indexes autoGenerate='true'><datastore-index kind='Person'/></datastore-indexes>";
  private static final String DISPATCH_XML = "<dispatch-entries/>";
  private static final String DOS_XML = "<blacklistentries/>";
  private static final String QUEUE_XML = "<queue-entries/>";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @Mock private IProgressMonitor monitor;

  private IPath stagingDirectory;
  private IProject project;

  @Before
  public void setUp() {
    project = projectCreator.getProject();
    stagingDirectory = new Path(tempFolder.getRoot().toString());
  }

  @Test
  public void testStage_cancelled() throws AppEngineException {
    when(monitor.isCanceled()).thenReturn(true);
    try {
      CloudSdkStagingHelper.stageStandard(null, null, null, monitor);
    } catch (OperationCanceledException ex) {
      assertEquals("canceled early", ex.getMessage());
    }
  }

  @Test
  public void testStageStandard() throws AppEngineException, CoreException {
    AppEngineStandardStaging staging = setUpAppEngineStaging();

    IPath explodedWarDirectory = project.getFolder("WebContent").getLocation();
    CloudSdkStagingHelper.stageStandard(explodedWarDirectory, stagingDirectory, staging, monitor);

    assertTrue(stagingDirectory.append("WEB-INF/web.xml").toFile().exists());
    assertTrue(stagingDirectory.append("META-INF/MANIFEST.MF").toFile().exists());
  }

  @Test
  public void testStageFlexible() throws CoreException, AppEngineException {
    createFile("src/main/appengine/app.yaml", APP_YAML);

    IFolder appEngineDirectory = project.getFolder("src/main/appengine");
    IPath deployArtifact = createFile("my-app.war", "fake WAR").getLocation();

    CloudSdkStagingHelper.stageFlexible(
        appEngineDirectory.getLocation(), deployArtifact, stagingDirectory, monitor);

    assertTrue(stagingDirectory.append("app.yaml").toFile().exists());
    assertTrue(stagingDirectory.append("my-app.war").toFile().exists());
  }

  @Test
  public void testCloudSdkStaging_xmlConfigFilesConvertedToYaml()
      throws CoreException, AppEngineException {
    AppEngineStandardStaging staging = setUpAppEngineStaging();

    createConfigFile("cron.xml", CRON_XML);
    createConfigFile("datastore-indexes.xml", DATASTORE_INDEXES_XML);
    createConfigFile("dispatch.xml", DISPATCH_XML);
    createConfigFile("dos.xml", DOS_XML);
    createConfigFile("queue.xml", QUEUE_XML);

    IPath explodedWarDirectory = project.getFolder("WebContent").getLocation();
    CloudSdkStagingHelper.stageStandard(explodedWarDirectory, stagingDirectory, staging, monitor);

    IPath stagingGenerated = stagingDirectory.append(
        CloudSdkStagingHelper.STANDARD_STAGING_GENERATED_FILES_DIRECTORY);
    assertTrue(stagingGenerated.toFile().isDirectory());
    assertTrue(stagingGenerated.append("cron.yaml").toFile().exists());
    assertTrue(stagingGenerated.append("index.yaml").toFile().exists());
    assertTrue(stagingGenerated.append("dispatch.yaml").toFile().exists());
    assertTrue(stagingGenerated.append("dos.yaml").toFile().exists());
    assertTrue(stagingGenerated.append("queue.yaml").toFile().exists());
  }

  private AppEngineStandardStaging setUpAppEngineStaging()
      throws CloudSdkNotFoundException, CoreException {
    createFile("WebContent/WEB-INF/appengine-web.xml", APPENGINE_WEB_XML);
    createFile("WebContent/WEB-INF/web.xml", WEB_XML);
    createFile("WebContent/META-INF/MANIFEST.MF", "");

    CloudSdk cloudSdk = new CloudSdk.Builder().build();
    AppCfg appCfg = AppCfg.builder(cloudSdk).build();
    ProcessHandler processHandler = LegacyProcessHandler.builder().async(false).build();
    return appCfg.newStaging(processHandler);
  }

  private void createConfigFile(String filename, String content) throws CoreException {
    createFile("WebContent/WEB-INF/" + filename, content);
  }

  private IFile createFile(String path, String content) throws CoreException {
    InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    IFile file = project.getFile(path);
    ResourceUtils.createFolders(file.getParent(), null);
    file.create(in, true, null);
    return file;
  }
}
