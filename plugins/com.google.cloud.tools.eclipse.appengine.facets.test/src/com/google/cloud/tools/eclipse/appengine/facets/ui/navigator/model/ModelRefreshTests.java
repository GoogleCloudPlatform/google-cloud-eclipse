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

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.ConfigurationFileUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Rule;
import org.junit.Test;

/** Test creation of AppEngineStandardProjectElement and its sub-elements. */
public class ModelRefreshTests {
  @Rule
  public TestProjectCreator projectCreator =
      new TestProjectCreator()
          .withFacets(
              AppEngineStandardFacet.FACET.getVersion("JRE8"),
              WebFacetUtils.WEB_31,
              JavaFacet.VERSION_1_8);

  @Test
  public void testAppEngineStandardProjectElementCreate() throws AppEngineException {
    IFile cronXml = ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject());
    IFile datastoreIndexesXml =
        ConfigurationFileUtils.createEmptyDatastoreIndexesXml(projectCreator.getProject());
    IFile dispatchXml = ConfigurationFileUtils.createEmptyDispatchXml(projectCreator.getProject());
    IFile dosXml = ConfigurationFileUtils.createEmptyDosXml(projectCreator.getProject());
    IFile queueXml = ConfigurationFileUtils.createEmptyQueueXml(projectCreator.getProject());
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());
    AppEngineResourceElement[] subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(5, subElements.length);
    assertThat(subElements, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DispatchRoutingDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DenialOfServiceDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(TaskQueuesDescriptor.class)));

    assertEquals(cronXml, findInstance(subElements, CronDescriptor.class).getFile());
    assertEquals(
        datastoreIndexesXml, findInstance(subElements, DatastoreIndexesDescriptor.class).getFile());
    assertEquals(dispatchXml, findInstance(subElements, DispatchRoutingDescriptor.class).getFile());
    assertEquals(dosXml, findInstance(subElements, DenialOfServiceDescriptor.class).getFile());
    assertEquals(queueXml, findInstance(subElements, TaskQueuesDescriptor.class).getFile());
  }

  @Test
  public void testAppEngineStandardProjectElementStaggered() throws AppEngineException {
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());
    AppEngineResourceElement[] subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(0, subElements.length);

    IFile cronXml = ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject());
    projectElement.resourceChanged(cronXml);
    subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(1, subElements.length);
    CronDescriptor cron = findInstance(subElements, CronDescriptor.class);
    assertThat(subElements, hasItemInArray(cron));

    IFile datastoreIndexesXml =
        ConfigurationFileUtils.createEmptyDatastoreIndexesXml(projectCreator.getProject());
    projectElement.resourceChanged(datastoreIndexesXml);
    subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(2, subElements.length);
    DatastoreIndexesDescriptor datastoreIndexes =
        findInstance(subElements, DatastoreIndexesDescriptor.class);
    assertThat(subElements, hasItemInArray(cron));
    assertThat(subElements, hasItemInArray(datastoreIndexes));

    IFile dispatchXml = ConfigurationFileUtils.createEmptyDispatchXml(projectCreator.getProject());
    projectElement.resourceChanged(dispatchXml);
    subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(3, subElements.length);
    DispatchRoutingDescriptor dispatch = findInstance(subElements, DispatchRoutingDescriptor.class);
    assertThat(subElements, hasItemInArray(cron));
    assertThat(subElements, hasItemInArray(datastoreIndexes));
    assertThat(subElements, hasItemInArray(dispatch));

    IFile dosXml = ConfigurationFileUtils.createEmptyDosXml(projectCreator.getProject());
    projectElement.resourceChanged(dosXml);
    subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(4, subElements.length);
    DenialOfServiceDescriptor dos = findInstance(subElements, DenialOfServiceDescriptor.class);
    assertThat(subElements, hasItemInArray(cron));
    assertThat(subElements, hasItemInArray(datastoreIndexes));
    assertThat(subElements, hasItemInArray(dispatch));
    assertThat(subElements, hasItemInArray(dos));

    IFile queueXml = ConfigurationFileUtils.createEmptyQueueXml(projectCreator.getProject());
    projectElement.resourceChanged(queueXml);
    subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(5, subElements.length);
    TaskQueuesDescriptor queue = findInstance(subElements, TaskQueuesDescriptor.class);
    assertThat(subElements, hasItemInArray(cron));
    assertThat(subElements, hasItemInArray(datastoreIndexes));
    assertThat(subElements, hasItemInArray(dispatch));
    assertThat(subElements, hasItemInArray(dos));
    assertThat(subElements, hasItemInArray(queue));

    assertEquals(cronXml, cron.getFile());
    assertEquals(datastoreIndexesXml, datastoreIndexes.getFile());
    assertEquals(dispatchXml, dispatch.getFile());
    assertEquals(dosXml, dos.getFile());
    assertEquals(queueXml, queue.getFile());
  }

  /**
   * A non-service change to appengine-web.xml should preserve same configuration child elements.
   */
  @Test
  public void testConfigurationElementsPreserved() throws AppEngineException {
    ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject());
    ConfigurationFileUtils.createEmptyDatastoreIndexesXml(projectCreator.getProject());
    ConfigurationFileUtils.createEmptyDispatchXml(projectCreator.getProject());
    ConfigurationFileUtils.createEmptyDosXml(projectCreator.getProject());
    ConfigurationFileUtils.createEmptyQueueXml(projectCreator.getProject());
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());
    final AppEngineResourceElement[] subElements = projectElement.getConfigurations();
    assertEquals(5, subElements.length);
    assertThat(subElements, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DispatchRoutingDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DenialOfServiceDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(TaskQueuesDescriptor.class)));

    // a change, but should have no effect
    ConfigurationFileUtils.createAppEngineWebXml(projectCreator.getProject(), "default");

    // check that all configuration elements are still present
    assertEquals(subElements.length, projectElement.getConfigurations().length);
    for (AppEngineResourceElement element : subElements) {
      assertThat(projectElement.getConfigurations(), hasItemInArray(element));
    }
  }

  /** A Service ID change to appengine-web.xml should toss configuration child elements */
  @Test
  public void testConfigurationElementsLost() throws AppEngineException {
    ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject());
    ConfigurationFileUtils.createEmptyDatastoreIndexesXml(projectCreator.getProject());
    ConfigurationFileUtils.createEmptyDispatchXml(projectCreator.getProject());
    ConfigurationFileUtils.createEmptyDosXml(projectCreator.getProject());
    ConfigurationFileUtils.createEmptyQueueXml(projectCreator.getProject());
    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());
    final AppEngineResourceElement[] subElements = projectElement.getConfigurations();
    assertEquals(5, subElements.length);
    assertThat(subElements, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DispatchRoutingDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DenialOfServiceDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(TaskQueuesDescriptor.class)));

    // a change, but should have no effect
    IFile appEngineWebXml =
        ConfigurationFileUtils.createAppEngineWebXml(projectCreator.getProject(), "non-default");
    projectElement.resourceChanged(appEngineWebXml);

    // check that all configuration elements are gone
    assertEquals(0, projectElement.getConfigurations().length);
  }

  @Test
  public void testChildElementPreservedOnChange() throws AppEngineException {
    List<IFile> configurationFiles = new ArrayList<>();
    configurationFiles.add(ConfigurationFileUtils.createEmptyCronXml(projectCreator.getProject()));
    configurationFiles.add(
        ConfigurationFileUtils.createEmptyDatastoreIndexesXml(projectCreator.getProject()));
    configurationFiles.add(
        ConfigurationFileUtils.createEmptyDispatchXml(projectCreator.getProject()));
    configurationFiles.add(ConfigurationFileUtils.createEmptyDosXml(projectCreator.getProject()));
    configurationFiles.add(ConfigurationFileUtils.createEmptyQueueXml(projectCreator.getProject()));

    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(projectCreator.getProject());
    final AppEngineResourceElement[] subElements = projectElement.getConfigurations();

    for (IFile configurationFile : configurationFiles) {
      Object handle = projectElement.resourceChanged(configurationFile);
      AppEngineResourceElement[] newSubElements = projectElement.getConfigurations();
      assertEquals(subElements.length, newSubElements.length);
      for (AppEngineResourceElement element : subElements) {
        assertThat(newSubElements, hasItemInArray(element));
      }
      Object descriptor =
          Iterables.find(
              Arrays.asList(subElements),
              element -> element != null && configurationFile.equals(element.getFile()));
      assertEquals(handle, descriptor);
    }

    // reloading the project element should return the project itself, as there may be
    // some label information that's changed
    Object handle = projectElement.resourceChanged(projectElement.getFile());
    assertEquals(projectElement.getProject(), handle);
  }

  private <S, T extends S> T findInstance(S[] array, Class<T> classT) {
    //    return Stream.of(array).filter(classT::isInstance).map(classT::cast).findAny().get();
    return classT.cast(Iterables.find(Arrays.asList(array), classT::isInstance));
  }
}
