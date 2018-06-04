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

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineResourceElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineStandardProjectElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.CronDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexesDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DenialOfServiceDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DispatchRoutingDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.TaskQueuesDescriptor;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.Collection;
import java.util.function.Consumer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineContentProviderTest {

  @Rule
  public TestProjectCreator projectCreator =
      new TestProjectCreator().withFacets(JavaFacet.VERSION_1_7);

  private AppEngineContentProvider fixture;

  /** Called by {@link #fixture} when elements require updating. */
  @Mock private Consumer<Collection<Object>> refreshHandler;

  @Before
  public void setUp() {
    fixture = new AppEngineContentProvider(refreshHandler);
  }

  @After
  public void tearDown() {
    fixture.dispose();
  }

  @Test
  public void testGetChildren_AppEngineStandardProject() {
    projectCreator.withFacets(AppEngineStandardFacet.JRE7, WebFacetUtils.WEB_25);

    ConfigurationFileUtils.createAppEngineWebXml(projectCreator.getProject(), null);
    Object[] children = fixture.getChildren(projectCreator.getFacetedProject());
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof AppEngineStandardProjectElement);
  }

  @Test
  public void testGetChildren_AppEngineStandardProjectElement_noService_noChildren()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(projectCreator.getProject(), null);
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(0, children.length);
  }

  @Test
  public void testGetChildren_AppEngineStandardProjectElement_noDefault_cronIgnored()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "service"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject());
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(0, children.length);
  }

  @Test
  public void testGetChildren_AppEngineStandardProjectElement_default_cron()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject());
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof CronDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testGetChildren_AppEngineStandardProjectElement_default_dispatch()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyDispatchXml(projectCreator.getProject());
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof DispatchRoutingDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testGetChildren_AppEngineStandardProjectElement_default_datastoreIndexes()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyDatastoreIndexesXml(projectCreator.getProject());
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof DatastoreIndexesDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testGetChildren_AppEngineStandardProjectElement_default_denialOfService()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyDosXml(projectCreator.getProject());
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof DenialOfServiceDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testGetChildren_AppEngineStandardProjectElement_default_taskQueue()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyQueueXml(projectCreator.getProject());
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof TaskQueuesDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testHasChildren_AppEngineStandardProject() {
    ConfigurationFileUtils.createAppEngineWebXml(projectCreator.getProject(), null);
    assertTrue(fixture.hasChildren(projectCreator.getProject()));
    assertTrue(fixture.hasChildren(projectCreator.getFacetedProject()));
  }

  @Test
  public void testHasChildren_AppEngineStandardProjectElement_noConfigs() {
    AppEngineStandardProjectElement projectElement = mock(AppEngineStandardProjectElement.class);
    when(projectElement.getConfigurations()).thenReturn(new AppEngineResourceElement[0]);
    assertFalse(fixture.hasChildren(projectElement));
  }

  @Test
  public void testHasChildren_AppEngineStandardProjectElement_withConfigs() {
    AppEngineStandardProjectElement projectElement = mock(AppEngineStandardProjectElement.class);
    when(projectElement.getConfigurations()).thenReturn(new AppEngineResourceElement[1]);
    assertTrue(fixture.hasChildren(projectElement));
  }

  @Test
  public void testDynamicChanges() throws CoreException {
    projectCreator.withFacets(AppEngineStandardFacet.JRE7, WebFacetUtils.WEB_25);
    StructuredViewer viewer = mock(StructuredViewer.class);
    fixture.inputChanged(viewer, null, null); // installs resource-changed listener

    ConfigurationFileUtils.createAppEngineWebXml(projectCreator.getProject(), null);
    verify(refreshHandler, atLeastOnce()).accept(anyObject());
    Object[] children = fixture.getChildren(projectCreator.getFacetedProject());
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof AppEngineStandardProjectElement);
    AppEngineStandardProjectElement projectElement = (AppEngineStandardProjectElement) children[0];
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(0, children.length);

    IFile cronXml = ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject());
    verify(refreshHandler, atLeastOnce()).accept(anyObject());
    children = fixture.getChildren(projectCreator.getFacetedProject());
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] == projectElement);
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertThat(children, hasItemInArray(instanceOf(CronDescriptor.class)));

    ConfigurationFileUtils.createEmptyDatastoreIndexesXml(projectCreator.getProject());
    verify(refreshHandler, atLeastOnce()).accept(anyObject());
    children = fixture.getChildren(projectCreator.getFacetedProject());
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] == projectElement);
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(2, children.length);
    assertThat(children, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertThat(children, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));

    cronXml.delete(true, null);
    verify(refreshHandler, atLeastOnce()).accept(anyObject());
    children = fixture.getChildren(projectCreator.getFacetedProject());
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] == projectElement);
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertThat(children, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));
  }
}
