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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineProjectElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.CronDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexesDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DenialOfServiceDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DispatchRoutingDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.TaskQueuesDescriptor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceManager;
import org.junit.Test;

public class AppEngineLabelProviderTest {
  private final ResourceManager resourceManager = mock(ResourceManager.class);
  private final AppEngineLabelProvider fixture = new AppEngineLabelProvider(resourceManager);
  private final AppEngineProjectElement programElement = mock(AppEngineProjectElement.class);

  @Test
  public void testProjectText_noAppEngineWebXml() {
    // The text for an App Engine standard project is really derived from the appengine-web.xml
    // descriptor, which is generated from getVersionTuple(). This test ensures that
    // we don't generate a text string when no appengine-web.xml is found.
    IProject project = mock(IProject.class);
    assertNull(AppEngineLabelProvider.getAppEngineProjectText(project));
  }

  @Test
  public void testAppEngineVersionTuple_nulls() {
    String result = AppEngineLabelProvider.getVersionTuple(programElement);
    assertNotNull(result);
    assertEquals(0, result.length());
  }

  @Test
  public void testAppEngineVersionTuple_project() {
    when(programElement.getProjectId()).thenReturn("project");
    String result = AppEngineLabelProvider.getVersionTuple(programElement);
    assertEquals("project", result);
  }

  @Test
  public void testAppEngineVersionTuple_version() {
    when(programElement.getProjectVersion()).thenReturn("version");
    String result = AppEngineLabelProvider.getVersionTuple(programElement);
    assertEquals("version", result);
  }

  @Test
  public void testAppEngineVersionTuple_service() {
    when(programElement.getServiceId()).thenReturn("service");
    String result = AppEngineLabelProvider.getVersionTuple(programElement);
    assertEquals("service", result);
  }

  @Test
  public void testAppEngineVersionTuple_project_version() {
    when(programElement.getProjectId()).thenReturn("project");
    when(programElement.getProjectVersion()).thenReturn("version");
    String result = AppEngineLabelProvider.getVersionTuple(programElement);
    assertEquals("project:version", result);
  }

  @Test
  public void testAppEngineVersionTuple_project_service() {
    when(programElement.getProjectId()).thenReturn("project");
    when(programElement.getServiceId()).thenReturn("service");
    String result = AppEngineLabelProvider.getVersionTuple(programElement);
    assertEquals("project:service", result);
  }

  @Test
  public void testAppEngineVersionTuple_version_service() {
    when(programElement.getProjectVersion()).thenReturn("version");
    when(programElement.getServiceId()).thenReturn("service");
    String result = AppEngineLabelProvider.getVersionTuple(programElement);
    assertEquals("service:version", result);
  }

  @Test
  public void testAppEngineVersionTuple_project_version_service() {
    when(programElement.getProjectId()).thenReturn("project");
    when(programElement.getProjectVersion()).thenReturn("version");
    when(programElement.getServiceId()).thenReturn("service");
    String result = AppEngineLabelProvider.getVersionTuple(programElement);
    assertEquals("project:service:version", result);
  }

  @Test
  public void testGetText_cronDescriptor_xml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("cron.xml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    CronDescriptor element = new CronDescriptor(file);
    assertEquals("Scheduled Tasks - cron.xml", fixture.getText(element));
  }

  @Test
  public void testGetText_datastoreIndexesDescriptor_xml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("datastore-indexes.xml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    DatastoreIndexesDescriptor element = new DatastoreIndexesDescriptor(file);
    assertEquals("Datastore Indexes - datastore-indexes.xml", fixture.getText(element));
  }

  @Test
  public void testGetText_denialOfServiceDescriptor_xml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("dos.xml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    DenialOfServiceDescriptor element = new DenialOfServiceDescriptor(file);
    assertEquals("Denial of Service Protection - dos.xml", fixture.getText(element));
  }

  @Test
  public void testGetText_dispatchRoutingDescriptor_xml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("dispatch.xml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    DispatchRoutingDescriptor element = new DispatchRoutingDescriptor(file);
    assertEquals("Dispatch Routing Rules - dispatch.xml", fixture.getText(element));
  }

  @Test
  public void testGetText_taskQueuesDescriptor_xml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("queue.xml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    TaskQueuesDescriptor element = new TaskQueuesDescriptor(file);
    assertEquals("Task Queue Definitions - queue.xml", fixture.getText(element));
  }

  @Test
  public void testGetText_cronDescriptor_yaml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("cron.yaml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    CronDescriptor element = new CronDescriptor(file);
    assertEquals("Scheduled Tasks - cron.yaml", fixture.getText(element));
  }

  @Test
  public void testGetText_datastoreIndexesDescriptor_yaml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("index.yaml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    DatastoreIndexesDescriptor element = new DatastoreIndexesDescriptor(file);
    assertEquals("Datastore Indexes - index.yaml", fixture.getText(element));
  }

  @Test
  public void testGetText_denialOfServiceDescriptor_yaml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("dos.yaml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    DenialOfServiceDescriptor element = new DenialOfServiceDescriptor(file);
    assertEquals("Denial of Service Protection - dos.yaml", fixture.getText(element));
  }

  @Test
  public void testGetText_dispatchRoutingDescriptor_yaml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("dispatch.yaml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    DispatchRoutingDescriptor element = new DispatchRoutingDescriptor(file);
    assertEquals("Dispatch Routing Rules - dispatch.yaml", fixture.getText(element));
  }

  @Test
  public void testGetText_taskQueuesDescriptor_yaml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("queue.yaml");
    when(file.exists()).thenReturn(true);
    when(file.getProject()).thenReturn(mock(IProject.class));
    TaskQueuesDescriptor element = new TaskQueuesDescriptor(file);
    assertEquals("Task Queue Definitions - queue.yaml", fixture.getText(element));
  }

  @Test
  public void testGetImage_appEngineProjectElement() {
    fixture.getImage(mock(AppEngineProjectElement.class));
    verify(resourceManager).create(any(ImageDescriptor.class));
  }

  @Test
  public void testGetImage_cronDescriptor() {
    fixture.getImage(mock(CronDescriptor.class));
    verify(resourceManager).create(any(ImageDescriptor.class));
  }

  @Test
  public void testGetImage_datastoreIndexesDescriptor() {
    fixture.getImage(mock(DatastoreIndexesDescriptor.class));
    verify(resourceManager).create(any(ImageDescriptor.class));
  }

  @Test
  public void testGetImage_denialOfServiceDescriptor() {
    fixture.getImage(mock(DenialOfServiceDescriptor.class));
    verify(resourceManager).create(any(ImageDescriptor.class));
  }

  @Test
  public void testGetImage_dispatchRoutingDescriptor() {
    fixture.getImage(mock(DispatchRoutingDescriptor.class));
    verify(resourceManager).create(any(ImageDescriptor.class));
  }

  @Test
  public void testGetImage_taskQueueDescriptor() {
    fixture.getImage(mock(TaskQueuesDescriptor.class));
    verify(resourceManager).create(any(ImageDescriptor.class));
  }
}
