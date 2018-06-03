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

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.CronDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexesDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DenialOfServiceDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DispatchRoutingDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.TaskQueuesDescriptor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ResourceManager;
import org.junit.Test;

public class AppEngineLabelProviderTest {
  private final AppEngineLabelProvider fixture =
      new AppEngineLabelProvider(mock(ResourceManager.class));

  @Test
  public void testProjectText_noAppEngineWebXml() {
    // The text for an App Engine standard project is really derived from the appengine-web.xml
    // descriptor, which is generated from getVersionTuple(). This test ensures that
    // we don't generate a text string when no appengine-web.xml is found.
    IProject project = mock(IProject.class);
    assertNull(AppEngineLabelProvider.getAppEngineStandardProjectText(project));
  }

  @Test
  public void testAppEngineVersionTuple_null() throws AppEngineException {
    String result = AppEngineLabelProvider.getVersionTuple(null);
    assertNotNull(result);
    assertEquals(0, result.length());
  }

  @Test
  public void testAppEngineVersionTuple_noData() throws AppEngineException {
    AppEngineDescriptor descriptor = mock(AppEngineDescriptor.class);
    doReturn(null).when(descriptor).getProjectId();
    doReturn(null).when(descriptor).getServiceId();
    doReturn(null).when(descriptor).getProjectVersion();

    String result = AppEngineLabelProvider.getVersionTuple(descriptor);
    assertNotNull(result);
    assertEquals(0, result.length());
  }

  @Test
  public void testAppEngineVersionTuple_project() throws AppEngineException {
    AppEngineDescriptor descriptor = mock(AppEngineDescriptor.class);
    doReturn("project").when(descriptor).getProjectId();
    doReturn(null).when(descriptor).getServiceId();
    doReturn(null).when(descriptor).getProjectVersion();

    String result = AppEngineLabelProvider.getVersionTuple(descriptor);
    assertEquals("project", result);
  }

  @Test
  public void testAppEngineVersionTuple_service() throws AppEngineException {
    AppEngineDescriptor descriptor = mock(AppEngineDescriptor.class);
    doReturn(null).when(descriptor).getProjectId();
    doReturn("service").when(descriptor).getServiceId();
    doReturn(null).when(descriptor).getProjectVersion();

    String result = AppEngineLabelProvider.getVersionTuple(descriptor);
    assertEquals("service", result);
  }

  @Test
  public void testAppEngineVersionTuple_version() throws AppEngineException {
    AppEngineDescriptor descriptor = mock(AppEngineDescriptor.class);
    doReturn(null).when(descriptor).getProjectId();
    doReturn(null).when(descriptor).getServiceId();
    doReturn("version").when(descriptor).getProjectVersion();

    String result = AppEngineLabelProvider.getVersionTuple(descriptor);
    assertEquals("version", result);
  }

  @Test
  public void testAppEngineVersionTuple_projectVersion() throws AppEngineException {
    AppEngineDescriptor descriptor = mock(AppEngineDescriptor.class);
    doReturn("project").when(descriptor).getProjectId();
    doReturn(null).when(descriptor).getServiceId();
    doReturn("version").when(descriptor).getProjectVersion();

    String result = AppEngineLabelProvider.getVersionTuple(descriptor);
    assertEquals("project:version", result);
  }

  @Test
  public void testAppEngineVersionTuple_projectService() throws AppEngineException {
    AppEngineDescriptor descriptor = mock(AppEngineDescriptor.class);
    doReturn("project").when(descriptor).getProjectId();
    doReturn("service").when(descriptor).getServiceId();
    doReturn(null).when(descriptor).getProjectVersion();

    String result = AppEngineLabelProvider.getVersionTuple(descriptor);
    assertEquals("project:service", result);
  }

  @Test
  public void testAppEngineVersionTuple_versionService() throws AppEngineException {
    AppEngineDescriptor descriptor = mock(AppEngineDescriptor.class);
    doReturn(null).when(descriptor).getProjectId();
    doReturn("service").when(descriptor).getServiceId();
    doReturn("version").when(descriptor).getProjectVersion();

    String result = AppEngineLabelProvider.getVersionTuple(descriptor);
    assertEquals("service:version", result);
  }

  @Test
  public void testAppEngineVersionTuple_projectVersionService()
      throws AppEngineException {
    AppEngineDescriptor descriptor = mock(AppEngineDescriptor.class);
    doReturn("project").when(descriptor).getProjectId();
    doReturn("service").when(descriptor).getServiceId();
    doReturn("version").when(descriptor).getProjectVersion();

    String result = AppEngineLabelProvider.getVersionTuple(descriptor);
    assertEquals("project:service:version", result);
  }

  @Test
  public void testCronDescriptor() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("cron.xml");
    when(file.exists()).thenReturn(true);
    CronDescriptor element = new CronDescriptor(mock(IProject.class), file);
    assertEquals("Scheduled Tasks", fixture.getText(element));
  }

  @Test
  public void testDatastoreIndexesDescriptor() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("datastore-indexes.xml");
    when(file.exists()).thenReturn(true);
    DatastoreIndexesDescriptor element = new DatastoreIndexesDescriptor(mock(IProject.class), file);
    assertEquals("Datastore Indexes", fixture.getText(element));
  }

  @Test
  public void testDenialOfServiceDescriptor() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("dos.xml");
    when(file.exists()).thenReturn(true);
    DenialOfServiceDescriptor element = new DenialOfServiceDescriptor(mock(IProject.class), file);
    assertEquals("Denial of Service Protection", fixture.getText(element));
  }

  @Test
  public void testDispatchRoutingDescriptor() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("dispatch.xml");
    when(file.exists()).thenReturn(true);
    DispatchRoutingDescriptor element = new DispatchRoutingDescriptor(mock(IProject.class), file);
    assertEquals("Dispatch Routing Rules", fixture.getText(element));
  }

  @Test
  public void testTaskQueuesDescriptor() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("queue.xml");
    when(file.exists()).thenReturn(true);
    TaskQueuesDescriptor element = new TaskQueuesDescriptor(mock(IProject.class), file);
    assertEquals("Task Queue Definitions", fixture.getText(element));
  }
}
