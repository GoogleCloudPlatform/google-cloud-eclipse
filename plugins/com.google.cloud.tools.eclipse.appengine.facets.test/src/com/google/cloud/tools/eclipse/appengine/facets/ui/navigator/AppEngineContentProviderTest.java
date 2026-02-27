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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineProjectElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineResourceElement;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.CronDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexesDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DenialOfServiceDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DispatchRoutingDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.TaskQueuesDescriptor;
import com.google.cloud.tools.eclipse.test.util.project.ConfigurationFileUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.common.io.ByteSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.BiConsumer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
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
  @Mock private BiConsumer<Collection<Object>, Collection<Object>> refreshHandler;

  @Before
  public void setUp() {
    fixture = new AppEngineContentProvider(refreshHandler);
  }

  @After
  public void tearDown() {
    fixture.dispose();
  }

  @Test
  public void testGetChildren_nonAppEngineProject() {
    Object[] children = fixture.getChildren(projectCreator.getProject());
    assertNotNull(children);
    assertEquals(0, children.length);
  }

  @Test
  public void testGetChildren_appEngineStandardProject() {
    projectCreator.withFacets(AppEngineStandardFacet.JRE7, WebFacetUtils.WEB_25);

    ConfigurationFileUtils.createAppEngineWebXml(projectCreator.getProject(), null);
    Object[] children = fixture.getChildren(projectCreator.getFacetedProject());
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof AppEngineProjectElement);
  }

  @Test
  public void testGetChildren_appEngineStandardProjectElement_noService_noChildren()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(projectCreator.getProject(), null);
    AppEngineProjectElement projectElement =
        AppEngineProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(0, children.length);
  }

  @Test
  public void testGetChildren_appEngineStandardProjectElement_noDefault_cronIgnored()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "service"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject());
    AppEngineProjectElement projectElement =
        AppEngineProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(0, children.length);
  }

  @Test
  public void testGetChildren_appEngineStandardProjectElement_default_cron()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject());
    AppEngineProjectElement projectElement =
        AppEngineProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof CronDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testGetChildren_appEngineStandardProjectElement_default_dispatch()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyDispatchXml(projectCreator.getProject());
    AppEngineProjectElement projectElement =
        AppEngineProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof DispatchRoutingDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testGetChildren_appEngineStandardProjectElement_default_datastoreIndexes()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyDatastoreIndexesXml(projectCreator.getProject());
    AppEngineProjectElement projectElement =
        AppEngineProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof DatastoreIndexesDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testGetChildren_appEngineStandardProjectElement_default_denialOfService()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyDosXml(projectCreator.getProject());
    AppEngineProjectElement projectElement =
        AppEngineProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof DenialOfServiceDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testGetChildren_appEngineStandardProjectElement_default_taskQueue()
      throws AppEngineException {
    ConfigurationFileUtils.createAppEngineWebXml(
        projectCreator.getProject(), "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyQueueXml(projectCreator.getProject());
    AppEngineProjectElement projectElement =
        AppEngineProjectElement.create(projectCreator.getProject());

    Object[] children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof TaskQueuesDescriptor);

    Object[] grandchildren = fixture.getChildren(children[0]);
    assertNotNull(grandchildren);
    assertEquals("should have no grandchildren", 0, grandchildren.length);
  }

  @Test
  public void testHasChildren_appEngineStandardProject() {
    ConfigurationFileUtils.createAppEngineWebXml(projectCreator.getProject(), null);
    assertTrue(fixture.hasChildren(projectCreator.getProject()));
    assertTrue(fixture.hasChildren(projectCreator.getFacetedProject()));
  }

  @Test
  public void testHasChildren_appEngineStandardProjectElement_noConfigs() {
    AppEngineProjectElement projectElement = mock(AppEngineProjectElement.class);
    when(projectElement.getConfigurations()).thenReturn(new AppEngineResourceElement[0]);
    assertFalse(fixture.hasChildren(projectElement));
  }

  @Test
  public void testHasChildren_appEngineStandardProjectElement_withConfigs() {
    AppEngineProjectElement projectElement = mock(AppEngineProjectElement.class);
    when(projectElement.getConfigurations()).thenReturn(new AppEngineResourceElement[1]);
    assertTrue(fixture.hasChildren(projectElement));
  }

  @Test
  public void testDynamicChanges_appEngineStandardJava7() throws CoreException {
    projectCreator.withFacets(AppEngineStandardFacet.JRE7, WebFacetUtils.WEB_25);
    StructuredViewer viewer = mock(StructuredViewer.class);
    fixture.inputChanged(viewer, null, null); // installs resource-changed listener

    IProject project = projectCreator.getProject();
    ConfigurationFileUtils.createAppEngineWebXml(project, null);
    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    Object[] children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof AppEngineProjectElement);
    AppEngineProjectElement projectElement = (AppEngineProjectElement) children[0];
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(0, children.length);

    IFile cronXml = ConfigurationFileUtils.createEmptyCronXml(project);
    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] == projectElement);
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertThat(children, hasItemInArray(instanceOf(CronDescriptor.class)));

    ConfigurationFileUtils.createEmptyDatastoreIndexesXml(project);
    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] == projectElement);
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(2, children.length);
    assertThat(children, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertThat(children, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));

    cronXml.delete(true, null);
    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] == projectElement);
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertThat(children, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));
  }

  @Test
  public void testDynamicChanges_appYaml() throws CoreException, IOException {
    projectCreator.withFacets(AppEngineStandardFacet.JRE7, WebFacetUtils.WEB_25);
    IProject project = projectCreator.getProject();

    StructuredViewer viewer = mock(StructuredViewer.class);
    fixture.inputChanged(viewer, null, null); // installs resource-changed listener

    ByteArrayInputStream stream =
        new ByteArrayInputStream("runtime: java\n".getBytes(StandardCharsets.UTF_8));
    AppEngineConfigurationUtil.createConfigurationFile(
        project, new Path("app.yaml"), stream, true, null);

    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    Object[] children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof AppEngineProjectElement);
    AppEngineProjectElement projectElement = (AppEngineProjectElement) children[0];
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(0, children.length);

    IFile cronYaml =
        AppEngineConfigurationUtil.createConfigurationFile(
            project, new Path("cron.yaml"), ByteSource.empty().openStream(), true, null);
    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] == projectElement);
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertThat(children, hasItemInArray(instanceOf(CronDescriptor.class)));

    AppEngineConfigurationUtil.createConfigurationFile(
        project, new Path("index.yaml"), ByteSource.empty().openStream(), true, null);
    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] == projectElement);
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(2, children.length);
    assertThat(children, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertThat(children, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));

    cronYaml.delete(true, null);
    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] == projectElement);
    children = fixture.getChildren(projectElement);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertThat(children, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));
  }

  /**
   * Test that a newly-created {@code appengine-web.xml} replaces the {@code app.yaml} for the
   * {@link AppEngineProjectElement}.
   */
  @Test
  public void testDynamicChanges_appEngineWebXml_appYaml() throws CoreException {
    projectCreator.withFacets(AppEngineStandardFacet.JRE7, WebFacetUtils.WEB_25);
    IProject project = projectCreator.getProject();

    StructuredViewer viewer = mock(StructuredViewer.class);
    fixture.inputChanged(viewer, null, null); // installs resource-changed listener

    IFile appEngineWebXml =
        AppEngineConfigurationUtil.findConfigurationFile(project, new Path("appengine-web.xml"));
    assertTrue(appEngineWebXml != null && appEngineWebXml.exists());
    Object[] children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof AppEngineProjectElement);
    final AppEngineProjectElement projectElement = (AppEngineProjectElement) children[0];
    assertEquals(appEngineWebXml, projectElement.getDescriptorFile());

    // appengine-web.xml should still win
    ByteArrayInputStream stream =
        new ByteArrayInputStream("runtime: java\n".getBytes(StandardCharsets.UTF_8));
    IFile appYaml =
        AppEngineConfigurationUtil.createConfigurationFile(
            project, new Path("app.yaml"), stream, true, null);
    verifyZeroInteractions(refreshHandler); // appengine-web.xml still wins
    children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertSame("the project element instance shouldn't change", projectElement, children[0]);
    assertEquals(
        "appengine.web.xml should still win", appEngineWebXml, projectElement.getDescriptorFile());

    // app.yaml should now win
    appEngineWebXml.delete(true, null);
    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertSame("the project element instance shouldn't change", projectElement, children[0]);
    assertEquals("app.yaml should win", appYaml, projectElement.getDescriptorFile());

    // appengine-web.xml should win again
    appEngineWebXml = ConfigurationFileUtils.createAppEngineWebXml(project, null);
    verify(refreshHandler, atLeastOnce()).accept(anyObject(), anyObject());
    children = fixture.getChildren(project);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertSame("the project element instance shouldn't change", projectElement, children[0]);
    assertEquals("back to appengine-web.xml", appEngineWebXml, projectElement.getDescriptorFile());
  }

  @Test
  public void testGetParent() {
    projectCreator.withFacets(AppEngineStandardFacet.JRE7, WebFacetUtils.WEB_25);
    IProject project = projectCreator.getProject();
    ConfigurationFileUtils.createAppEngineWebXml(project, "default"); // $NON-NLS-1$
    ConfigurationFileUtils.createEmptyQueueXml(project);

    // must populate the model via the AppEngineContentProvider
    Object[] children = fixture.getChildren(project);
    assertEquals(1, children.length);
    AppEngineProjectElement projectElement = (AppEngineProjectElement) children[0];
    children = fixture.getChildren(projectElement);
    assertEquals(1, children.length);
    TaskQueuesDescriptor queueDescriptor = (TaskQueuesDescriptor) children[0];

    assertSame(project, fixture.getParent(projectElement));
    assertSame(projectElement, fixture.getParent(queueDescriptor));
  }
}
