/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatastoreIndexUpdateDataTest {
  @Rule public TemporaryFolder root = new TemporaryFolder();

  @Mock private ILaunchConfiguration launchConfiguration;
  @Mock private IServer server;
  @Mock private LocalAppEngineServerBehaviour serverBehaviour;
  @Mock private IModule defaultService;
  @Mock private IProject project;

  private File webInf;

  @Before
  public void setUp() throws IOException {
    when(server.getAdapter(ServerBehaviourDelegate.class)).thenReturn(serverBehaviour);
    when(server.loadAdapter(eq(LocalAppEngineServerBehaviour.class), any(IProgressMonitor.class)))
        .thenReturn(serverBehaviour);
    when(defaultService.getProject()).thenReturn(project);
    doReturn(new Path(root.getRoot().getPath()))
        .when(serverBehaviour)
        .getModuleDeployDirectory(defaultService);
    webInf = root.newFolder("WEB-INF");
  }

  @Test
  public void testNoUpdateWithNoFile() {
    DatastoreIndexUpdateData update =
        DatastoreIndexUpdateData.detect(launchConfiguration, server, defaultService);
    assertNull(update);
  }
  
  @Test
  public void testNoUpdateWithEmptyFile() throws IOException {
    createEmptyDatastoreIndexesAutoXml();
    
    DatastoreIndexUpdateData update =
        DatastoreIndexUpdateData.detect(launchConfiguration, server, defaultService);
    assertNull(update);
  }

  @Test
  public void testNoDatastoreIndexesXml() throws IOException {
    createDatastoreIndexesAutoXml();

    DatastoreIndexUpdateData update =
        DatastoreIndexUpdateData.detect(launchConfiguration, server, defaultService);
    assertNotNull(update);
    assertEquals(launchConfiguration, update.configuration);
    assertEquals(server, update.server);
    assertEquals(defaultService, update.module);
    assertNull(update.datastoreIndexesXml);
    assertNotNull(update.datastoreIndexesAutoXml);
  }

  @Test
  public void testExistingDatastoreIndexesXml_updated() throws IOException {
    createDatastoreIndexesAutoXml();

    IFolder projectWebContent = mock(IFolder.class, "WebContent");
    IFolder projectWebInf = mock(IFolder.class, "WEB-INF");
    IFile projectDatastoreIndexXml = mock(IFile.class, "datastore-indexes.xml");
    when(project.getFolder("WebContent")).thenReturn(projectWebContent);
    when(projectWebContent.exists()).thenReturn(true);
    when(projectWebContent.getFolder("WEB-INF")).thenReturn(projectWebInf);
    when(projectWebInf.exists()).thenReturn(true);
    when(projectWebInf.getFile("datastore-indexes.xml")).thenReturn(projectDatastoreIndexXml);
    when(projectWebInf.getFile(new Path("datastore-indexes.xml")))
        .thenReturn(projectDatastoreIndexXml);
    when(projectDatastoreIndexXml.exists()).thenReturn(true);

    // updated, so the original should be older than generated
    when(projectDatastoreIndexXml.getLocalTimeStamp()).thenReturn(0L);

    DatastoreIndexUpdateData update =
        DatastoreIndexUpdateData.detect(launchConfiguration, server, defaultService);
    assertNotNull(update);
    assertEquals(launchConfiguration, update.configuration);
    assertEquals(server, update.server);
    assertEquals(defaultService, update.module);
    assertEquals(projectDatastoreIndexXml, update.datastoreIndexesXml);
    assertNotNull(update.datastoreIndexesAutoXml);
  }

  @Test
  public void testExistingDatastoreIndexesXml_noUpdate() throws IOException {
    createDatastoreIndexesAutoXml();

    IFolder projectWebContent = mock(IFolder.class, "WebContent");
    IFolder projectWebInf = mock(IFolder.class, "WEB-INF");
    IFile projectDatastoreIndexXml = mock(IFile.class, "datastore-indexes.xml");
    when(project.getFolder("WebContent")).thenReturn(projectWebContent);
    when(projectWebContent.exists()).thenReturn(true);
    when(projectWebContent.getFolder("WEB-INF")).thenReturn(projectWebInf);
    when(projectWebInf.exists()).thenReturn(true);
    when(projectWebInf.getFile("datastore-indexes.xml")).thenReturn(projectDatastoreIndexXml);
    when(projectWebInf.getFile(new Path("datastore-indexes.xml")))
        .thenReturn(projectDatastoreIndexXml);
    when(projectDatastoreIndexXml.exists()).thenReturn(true);

    // no updates, so original should be newer than generated
    when(projectDatastoreIndexXml.getLocalTimeStamp()).thenReturn(System.currentTimeMillis() + 1);

    DatastoreIndexUpdateData update =
        DatastoreIndexUpdateData.detect(launchConfiguration, server, defaultService);
    assertNull(update);
  }

  private void createDatastoreIndexesAutoXml() throws IOException {    
    try (Writer out = openDatastoreIndexesAutoFile()) {
      out.write("<datastore-indexes>");
      out.write("<datastore-index kind='Employee' ancestor='false'>");
      out.write("<property name='lastName' direction='asc' />");
      out.write("</datastore-index>");
      out.write("</datastore-indexes>");
      out.flush();
    }
  }

  private Writer openDatastoreIndexesAutoFile() throws IOException {
    File appengineGenerated = new File(webInf, "appengine-generated");
    File datastoreIndexesAutoXml = new File(appengineGenerated, "datastore-indexes-auto.xml");
    assertTrue(appengineGenerated.mkdirs());
    assertTrue(datastoreIndexesAutoXml.createNewFile());
    
    return new OutputStreamWriter(
        Files.newOutputStream(datastoreIndexesAutoXml.toPath()),
        StandardCharsets.UTF_8);
  }

  private void createEmptyDatastoreIndexesAutoXml() throws IOException {
    try (Writer out = openDatastoreIndexesAutoFile()) {
      out.write("<datastore-indexes/>");
      out.flush();
    }
  }
}
