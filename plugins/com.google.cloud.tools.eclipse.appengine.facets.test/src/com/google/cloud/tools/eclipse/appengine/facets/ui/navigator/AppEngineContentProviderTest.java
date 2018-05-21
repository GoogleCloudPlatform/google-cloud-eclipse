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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.AppEngineWebDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.CronDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DatastoreIndexesDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DenialOfServiceDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.DispatchRoutingDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model.TaskQueuesDescriptor;
import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.SAXException;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineContentProviderTest {

  private final MockWorkspace mockWorkspace = new MockWorkspace();
  private final AppEngineContentProvider fixture = new AppEngineContentProvider();
  private IFacetedProject facetedProject;

  @Before
  public void setUp() throws CoreException {
    facetedProject =
        mockWorkspace.createFacetedProject(
            "foo", // $NON-NLS-1$
            AppEngineStandardFacet.JRE7,
            WebFacetUtils.WEB_25,
            JavaFacet.VERSION_1_7);
  }

  private IFile createAppEngineWebXml(String serviceId) {
    String contents =
        Strings.isNullOrEmpty(serviceId)
            ? "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'/>" // $NON-NLS-1$
            : "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'><service>" // $NON-NLS-1$
                + serviceId
                + "</service></appengine-web-app>"; // $NON-NLS-1$
    return mockWorkspace.createFile(
        facetedProject.getProject(),
        new Path("WebContent/WEB-INF/appengine-web.xml"), // $NON-NLS-1$
        contents);
  }

  private IFile createEmptyCronXml() {
    return mockWorkspace.createFile(
        facetedProject.getProject(),
        new Path("WebContent/WEB-INF/cron.xml"), // $NON-NLS-1$
        "<cronentries/>"); // $NON-NLS-1$
  }

  private IFile createEmptyDispatchXml() {
    return mockWorkspace.createFile(
        facetedProject.getProject(),
        new Path("WebContent/WEB-INF/dispatch.xml"), // $NON-NLS-1$
        "<dispatch-entries/>"); // $NON-NLS-1$
  }

  private IFile createEmptyDosXml() {
    return mockWorkspace.createFile(
        facetedProject.getProject(),
        new Path("WebContent/WEB-INF/dos.xml"), // $NON-NLS-1$
        "<blacklistentries/>"); // $NON-NLS-1$
  }

  private IFile createEmptyQueueXml() {
    return mockWorkspace.createFile(
        facetedProject.getProject(),
        new Path("WebContent/WEB-INF/queue.xml"), // $NON-NLS-1$
        "<queue-entries/>"); // $NON-NLS-1$
  }

  private IFile createEmptyDatastoreIndexesXml() {
    return mockWorkspace.createFile(
        facetedProject.getProject(),
        new Path("WebContent/WEB-INF/datastore-indexes.xml"), // $NON-NLS-1$
        "<datastore-indexes/>"); // $NON-NLS-1$
  }

  @Test
  public void testGetChildren_AppEngineStandardProject() {
    createAppEngineWebXml(null);
    Object[] children = fixture.getChildren(facetedProject);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof AppEngineWebDescriptor);
  }

  @Test
  public void testGetChildren_AppEngineWebDescriptor_noService_noChildren()
      throws IOException, SAXException, CoreException {
    IFile appEngineWebXml = createAppEngineWebXml(null);
    AppEngineDescriptor descriptor;
    try (InputStream contents = appEngineWebXml.getContents()) {
      descriptor = AppEngineDescriptor.parse(contents);
    }
    AppEngineWebDescriptor webDescriptor =
        new AppEngineWebDescriptor(facetedProject, appEngineWebXml, descriptor);

    Object[] children = fixture.getChildren(webDescriptor);
    assertNotNull(children);
    assertEquals(0, children.length);
  }

  @Test
  public void testGetChildren_AppEngineWebDescriptor_noDefault_cronIgnored()
      throws IOException, SAXException, CoreException {
    IFile appEngineWebXml = createAppEngineWebXml("service"); // $NON-NLS-1$
    createEmptyCronXml();
    AppEngineDescriptor descriptor;
    try (InputStream contents = appEngineWebXml.getContents()) {
      descriptor = AppEngineDescriptor.parse(contents);
    }
    AppEngineWebDescriptor webDescriptor =
        new AppEngineWebDescriptor(facetedProject, appEngineWebXml, descriptor);

    Object[] children = fixture.getChildren(webDescriptor);
    assertNotNull(children);
    assertEquals(0, children.length);
  }

  @Test
  public void testGetChildren_AppEngineWebDescriptor_default_cron()
      throws IOException, SAXException, CoreException {
    IFile appEngineWebXml = createAppEngineWebXml("default"); // $NON-NLS-1$
    createEmptyCronXml();
    AppEngineDescriptor descriptor;
    try (InputStream contents = appEngineWebXml.getContents()) {
      descriptor = AppEngineDescriptor.parse(contents);
    }
    AppEngineWebDescriptor webDescriptor =
        new AppEngineWebDescriptor(facetedProject, appEngineWebXml, descriptor);

    Object[] children = fixture.getChildren(webDescriptor);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof CronDescriptor);
  }

  @Test
  public void testGetChildren_AppEngineWebDescriptor_default_dispatch()
      throws IOException, SAXException, CoreException {
    IFile appEngineWebXml = createAppEngineWebXml("default"); // $NON-NLS-1$
    createEmptyDispatchXml();
    AppEngineDescriptor descriptor;
    try (InputStream contents = appEngineWebXml.getContents()) {
      descriptor = AppEngineDescriptor.parse(contents);
    }
    AppEngineWebDescriptor webDescriptor =
        new AppEngineWebDescriptor(facetedProject, appEngineWebXml, descriptor);

    Object[] children = fixture.getChildren(webDescriptor);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof DispatchRoutingDescriptor);
  }

  @Test
  public void testGetChildren_AppEngineWebDescriptor_default_datastoreIndexes()
      throws IOException, SAXException, CoreException {
    IFile appEngineWebXml = createAppEngineWebXml("default"); // $NON-NLS-1$
    createEmptyDatastoreIndexesXml();
    AppEngineDescriptor descriptor;
    try (InputStream contents = appEngineWebXml.getContents()) {
      descriptor = AppEngineDescriptor.parse(contents);
    }
    AppEngineWebDescriptor webDescriptor =
        new AppEngineWebDescriptor(facetedProject, appEngineWebXml, descriptor);

    Object[] children = fixture.getChildren(webDescriptor);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof DatastoreIndexesDescriptor);
  }

  @Test
  public void testGetChildren_AppEngineWebDescriptor_default_denialOfService()
      throws IOException, SAXException, CoreException {
    IFile appEngineWebXml = createAppEngineWebXml("default"); // $NON-NLS-1$
    createEmptyDosXml();
    AppEngineDescriptor descriptor;
    try (InputStream contents = appEngineWebXml.getContents()) {
      descriptor = AppEngineDescriptor.parse(contents);
    }
    AppEngineWebDescriptor webDescriptor =
        new AppEngineWebDescriptor(facetedProject, appEngineWebXml, descriptor);

    Object[] children = fixture.getChildren(webDescriptor);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof DenialOfServiceDescriptor);
  }

  @Test
  public void testGetChildren_AppEngineWebDescriptor_default_taskQueue()
      throws IOException, SAXException, CoreException {
    IFile appEngineWebXml = createAppEngineWebXml("default"); // $NON-NLS-1$
    createEmptyQueueXml();
    AppEngineDescriptor descriptor;
    try (InputStream contents = appEngineWebXml.getContents()) {
      descriptor = AppEngineDescriptor.parse(contents);
    }
    AppEngineWebDescriptor webDescriptor =
        new AppEngineWebDescriptor(facetedProject, appEngineWebXml, descriptor);

    Object[] children = fixture.getChildren(webDescriptor);
    assertNotNull(children);
    assertEquals(1, children.length);
    assertTrue(children[0] instanceof TaskQueuesDescriptor);
  }

  @Test
  public void testHasChildren_AppEngineStandardProject() {
    createAppEngineWebXml(null);
    assertTrue(fixture.hasChildren(facetedProject));
  }

  @Test
  public void testHasChildren_AppEngineWebDescriptor() {
    assertTrue(fixture.hasChildren(mock(AppEngineWebDescriptor.class)));
  }
}
