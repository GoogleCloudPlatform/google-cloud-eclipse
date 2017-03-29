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

package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StandardProjectStagingTest {

  private static final String cronXml = "<cronentries/>";
  private static final String datastoreIndexesXml =
      "<datastore-indexes autoGenerate='true'><datastore-index kind='Person'/></datastore-indexes>";
  private static final String dispatchXml = "<dispatch-entries/>";
  private static final String dosXml = "<blacklistentries/>";
  private static final String queueXml = "<queue-entries/>";

  private static final IProjectFacetVersion APP_ENGINE_STANDARD_FACET_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");

  @Rule
  public TestProjectCreator projectCreator = new TestProjectCreator().withFacetVersions(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, APP_ENGINE_STANDARD_FACET_1);

  @Mock private IPath warDirectory;
  @Mock private IPath stagingDirectory;
  @Mock private CloudSdk cloudSdk;
  @Mock private IProgressMonitor monitor;

  private IProject project;

  @Before
  public void setUp() {
    project = projectCreator.getProject();
  }

  @Test(expected = OperationCanceledException.class)
  public void testStage_cancelled() {
    when(monitor.isCanceled()).thenReturn(true);
    new StandardProjectStaging().stage(warDirectory, stagingDirectory, cloudSdk, monitor);
  }

  @Test
  public void test() throws CoreException {
    IProject project = projectCreator.getProject();
    createConfigFile("cron.xml", cronXml);
    createConfigFile("datastore-indexes.xml", datastoreIndexesXml);
    createConfigFile("dispatch.xml", dispatchXml);
    createConfigFile("dos.xml", dosXml);
    createConfigFile("queue.xml", queueXml);
    Assert.fail();
  }

  private void createConfigFile(String filename, String content) throws CoreException {
    InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    IFile file = project.getFile("src/main/webapp/WEB-INF/" + filename);
    file.setContents(in, IResource.FORCE, null);
  }
}
