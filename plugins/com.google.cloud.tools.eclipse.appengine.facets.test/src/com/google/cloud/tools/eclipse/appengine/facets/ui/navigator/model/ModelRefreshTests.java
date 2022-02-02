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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.ConfigurationFileUtils;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.componentcore.J2EEModuleVirtualComponent;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

/** Test creation of AppEngineProjectElement and its sub-elements. */
public class ModelRefreshTests {
  @Rule
  public TestProjectCreator projectCreator =
      new TestProjectCreator()
          .withFacets(
              AppEngineStandardFacet.FACET.getVersion("JRE8"),
              WebFacetUtils.WEB_31,
              JavaFacet.VERSION_1_8);

  /** Verify that the content block is configured for initial configuration files. */
  @Test
  public void testAppEngineProjectElementCreate_initial() throws AppEngineException {
    IProject project = projectCreator.getProject();
    IFile cronXml = ConfigurationFileUtils.createEmptyCronXml(project);
    IFile datastoreIndexesXml =
        ConfigurationFileUtils.createEmptyDatastoreIndexesXml(project);
    IFile dispatchXml = ConfigurationFileUtils.createEmptyDispatchXml(project);
    IFile dosXml = ConfigurationFileUtils.createEmptyDosXml(project);
    IFile queueXml = ConfigurationFileUtils.createEmptyQueueXml(project);
    
    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    AppEngineResourceElement[] subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(5, subElements.length);
    
    assertThat(subElements, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertEquals(cronXml, findInstance(subElements, CronDescriptor.class).getFile());

    assertThat(subElements, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));
    assertEquals(
        datastoreIndexesXml, findInstance(subElements, DatastoreIndexesDescriptor.class).getFile());

    assertThat(subElements, hasItemInArray(instanceOf(DispatchRoutingDescriptor.class)));
    assertEquals(dispatchXml, findInstance(subElements, DispatchRoutingDescriptor.class).getFile());

    assertThat(subElements, hasItemInArray(instanceOf(DenialOfServiceDescriptor.class)));
    assertEquals(dosXml, findInstance(subElements, DenialOfServiceDescriptor.class).getFile());

    assertThat(subElements, hasItemInArray(instanceOf(TaskQueuesDescriptor.class)));
    assertEquals(queueXml, findInstance(subElements, TaskQueuesDescriptor.class).getFile());
  }

  /** Verify that the content block is progressively updated as configuration files are added. */
  @Test
  public void testAppEngineProjectElementCreate_staggered() throws AppEngineException {
    IProject project = projectCreator.getProject();
    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    AppEngineResourceElement[] subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(0, subElements.length);

    IFile cronXml = ConfigurationFileUtils.createEmptyCronXml(project);
    projectElement.resourcesChanged(Collections.singleton(cronXml));
    subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(1, subElements.length);
    CronDescriptor cron = findInstance(subElements, CronDescriptor.class);
    assertThat(subElements, hasItemInArray(cron));

    IFile datastoreIndexesXml = ConfigurationFileUtils.createEmptyDatastoreIndexesXml(project);
    projectElement.resourcesChanged(Collections.singleton(datastoreIndexesXml));
    subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(2, subElements.length);
    DatastoreIndexesDescriptor datastoreIndexes =
        findInstance(subElements, DatastoreIndexesDescriptor.class);
    assertThat(subElements, hasItemInArray(cron));
    assertThat(subElements, hasItemInArray(datastoreIndexes));

    IFile dispatchXml = ConfigurationFileUtils.createEmptyDispatchXml(project);
    projectElement.resourcesChanged(Collections.singleton(dispatchXml));
    subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(3, subElements.length);
    DispatchRoutingDescriptor dispatch = findInstance(subElements, DispatchRoutingDescriptor.class);
    assertThat(subElements, hasItemInArray(cron));
    assertThat(subElements, hasItemInArray(datastoreIndexes));
    assertThat(subElements, hasItemInArray(dispatch));

    IFile dosXml = ConfigurationFileUtils.createEmptyDosXml(project);
    projectElement.resourcesChanged(Collections.singleton(dosXml));
    subElements = projectElement.getConfigurations();
    assertNotNull(subElements);
    assertEquals(4, subElements.length);
    DenialOfServiceDescriptor dos = findInstance(subElements, DenialOfServiceDescriptor.class);
    assertThat(subElements, hasItemInArray(cron));
    assertThat(subElements, hasItemInArray(datastoreIndexes));
    assertThat(subElements, hasItemInArray(dispatch));
    assertThat(subElements, hasItemInArray(dos));

    IFile queueXml = ConfigurationFileUtils.createEmptyQueueXml(project);
    projectElement.resourcesChanged(Collections.singleton(queueXml));
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
  public void testChangeToDefaultPreservesConfigurationElements() throws AppEngineException {
    IProject project = projectCreator.getProject();
    ConfigurationFileUtils.createEmptyCronXml(project);
    ConfigurationFileUtils.createEmptyDatastoreIndexesXml(project);
    ConfigurationFileUtils.createEmptyDispatchXml(project);
    ConfigurationFileUtils.createEmptyDosXml(project);
    ConfigurationFileUtils.createEmptyQueueXml(project);
    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    AppEngineResourceElement[] subElements = projectElement.getConfigurations();
    assertEquals(5, subElements.length);
    assertThat(subElements, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DispatchRoutingDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DenialOfServiceDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(TaskQueuesDescriptor.class)));

    // a change, but should have no effect
    ConfigurationFileUtils.createAppEngineWebXml(project, "default");

    // check that all configuration elements are still present
    assertEquals(subElements.length, projectElement.getConfigurations().length);
    for (AppEngineResourceElement element : subElements) {
      assertThat(projectElement.getConfigurations(), hasItemInArray(element));
    }
  }

  /**
   * A Service ID change from {@code default} to non-default should toss configuration child
   * elements
   */
  @Test
  public void testChangeToNonDefaultDiscardsConfigurationElements() throws AppEngineException {
    IProject project = projectCreator.getProject();
    ConfigurationFileUtils.createEmptyCronXml(project);
    ConfigurationFileUtils.createEmptyDatastoreIndexesXml(project);
    ConfigurationFileUtils.createEmptyDispatchXml(project);
    ConfigurationFileUtils.createEmptyDosXml(project);
    ConfigurationFileUtils.createEmptyQueueXml(project);
    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    AppEngineResourceElement[] subElements = projectElement.getConfigurations();
    assertEquals(5, subElements.length);
    assertThat(subElements, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DatastoreIndexesDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DispatchRoutingDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(DenialOfServiceDescriptor.class)));
    assertThat(subElements, hasItemInArray(instanceOf(TaskQueuesDescriptor.class)));

    // a change, and should discard all configuration elements
    IFile appEngineWebXml = ConfigurationFileUtils.createAppEngineWebXml(project, "non-default");
    assertTrue(projectElement.resourcesChanged(Collections.singleton(appEngineWebXml)));

    // check that all configuration elements are gone
    assertEquals(0, projectElement.getConfigurations().length);
  }

  /**
   * None of our {@link AppEngineResourceElement#reload()} currently return a different instance, so
   * a configuration file change should not result in a change.
   */
  @Test
  public void testChildElementPreservedOnChange() throws AppEngineException {
    List<IFile> files = new ArrayList<>();
    IProject project = projectCreator.getProject();
    files.add(ConfigurationFileUtils.createEmptyCronXml(project));
    files.add(ConfigurationFileUtils.createEmptyDatastoreIndexesXml(project));
    files.add(ConfigurationFileUtils.createEmptyDispatchXml(project));
    files.add(ConfigurationFileUtils.createEmptyDosXml(project));
    files.add(ConfigurationFileUtils.createEmptyQueueXml(project));

    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    files.add(projectElement.getDescriptorFile());
    AppEngineResourceElement[] subElements = projectElement.getConfigurations();

    for (IFile file : files) {
      boolean changed = projectElement.resourcesChanged(Collections.singleton(file));
      assertTrue(changed);
      AppEngineResourceElement[] newSubElements = projectElement.getConfigurations();
      Set<Object> difference =
          Sets.symmetricDifference(Sets.newHashSet(subElements), Sets.newHashSet(newSubElements));
      assertThat("all elements should have been preserved", difference, Matchers.hasSize(0));
    }
  }

  /**
   * Ensure that the content block does not add new configuration files to a non-default service.
   */
  @Test
  public void testNonDefaultServiceIgnoresNewFiles() throws AppEngineException {
    IProject project = projectCreator.getProject();
    ConfigurationFileUtils.createAppEngineWebXml(project, "non-default");

    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    AppEngineResourceElement[] subElements = projectElement.getConfigurations();
    assertEquals(0, subElements.length);

    IFile cronXml = ConfigurationFileUtils.createEmptyCronXml(project);
    assertFalse(projectElement.resourcesChanged(Collections.singleton(cronXml)));
    subElements = projectElement.getConfigurations();
    assertEquals(0, subElements.length);
  }


  /**
   * Verify that the model is updated after altering the deployment assembly model to favour a
   * different WEB-INF folder.
   */
  @Test
  public void testRejigDeploymentAssembly() throws AppEngineException, CoreException {
    IProject project = projectCreator.getProject();
    // verify the new files are not picked up yet
    IFile oldCronXml = ConfigurationFileUtils.createEmptyCronXml(project);
    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    AppEngineResourceElement[] oldElements = projectElement.getConfigurations();
    assertEquals(1, oldElements.length);
    assertThat(oldElements, hasItemInArray(instanceOf(CronDescriptor.class)));

    // create the new WEB-INF location and populate it
    IFolder newWebRoot = project.getFolder("newWebRoot");
    IFolder newWebInf = newWebRoot.getFolder("WEB-INF");
    ResourceUtils.createFolders(newWebInf, null);
    IFile newDispatchXml = newWebInf.getFile("dispatch.xml");
    newDispatchXml.create(
        new ByteArrayInputStream("<dispatch-entries/>".getBytes(StandardCharsets.UTF_8)),
        true,
        null);
    assertTrue("error creating new dispatch.xml", newDispatchXml.exists());

    // now link in the new WEB-INF into the overlay
    IWorkspace workspace = project.getWorkspace();
    Set<IFile> changed =
        recordChangedFilesDuring(
            workspace,
            monitor -> {
              // link in the newWebRoot
              IVirtualFolder webroot = ComponentCore.createComponent(project).getRootFolder();
              webroot.createLink(newWebRoot.getProjectRelativePath(), 0, null);
              J2EEModuleVirtualComponent.setDefaultDeploymentDescriptorFolder(
                  webroot, newWebRoot.getProjectRelativePath(), null);
              ProjectUtils.waitForProjects(project);
            });
    assertThat(changed,
        Matchers.hasItem(project.getFile(".settings/org.eclipse.wst.common.component")));
    assertTrue(projectElement.resourcesChanged(changed));

    AppEngineResourceElement[] newElements = projectElement.getConfigurations();
    assertEquals(2, newElements.length);
    assertThat(newElements, hasItemInArray(instanceOf(CronDescriptor.class)));
    assertThat(newElements, hasItemInArray(instanceOf(DispatchRoutingDescriptor.class)));
    CronDescriptor newCron = findInstance(newElements, CronDescriptor.class);
    assertEquals(oldCronXml, newCron.getFile());
    DispatchRoutingDescriptor newDispatch =
        findInstance(newElements, DispatchRoutingDescriptor.class);
    assertEquals(newDispatchXml, newDispatch.getFile());
  }

  /** Record and return the set of files altered when running the provided block. */
  private static Set<IFile> recordChangedFilesDuring(IWorkspace workspace, ICoreRunnable block)
      throws CoreException {
    Set<IFile> changed = new LinkedHashSet<>();
    IResourceChangeListener listener =
        event -> {
          try {
            changed.addAll(ResourceUtils.getAffectedFiles(event.getDelta()).values());
          } catch (CoreException ex) {
            throw new RuntimeException(ex);
          }
        };
    workspace.addResourceChangeListener(listener);
    try {
      block.run(new NullProgressMonitor());
    } finally {
      workspace.removeResourceChangeListener(listener);
    }
    return changed;
  }

  private <S, T extends S> T findInstance(S[] array, Class<T> classT) {
    return classT.cast(Iterables.find(Arrays.asList(array), classT::isInstance));
  }
}
