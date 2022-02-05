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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexWarFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.Collections;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineProjectElementTest {
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testCreation_appEngineFlexibleWar() throws AppEngineException {
    projectCreator.withFacets(
        AppEngineFlexWarFacet.FACET_VERSION, WebFacetUtils.WEB_31, JavaFacet.VERSION_1_8);
    IProject project = projectCreator.getProject();

    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    assertNotNull(projectElement);
    assertNotNull(projectElement.getDescriptorFile());
    assertEquals("app.yaml", projectElement.getDescriptorFile().getName());
  }

  @Test
  public void testEnvironmentRuntime_xml_noRuntime() throws CoreException, AppEngineException {
    IProject project = projectCreator.getProject();
    String contents = "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'/>";
    AppEngineConfigurationUtil.createConfigurationFile(
        project, new Path("appengine-web.xml"), contents, true, null);

    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    assertNotNull(projectElement);
    assertEquals("standard", projectElement.getEnvironmentType());
    assertEquals("java7", projectElement.getRuntime());
  }

  @Test
  public void testEnvironmentRuntime_xml_java8Runtime() throws CoreException, AppEngineException {
    IProject project = projectCreator.getProject();
    String contents =
        "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
            + "<runtime>java8</runtime>"
            + "</appengine-web-app>";
    AppEngineConfigurationUtil.createConfigurationFile(
        project, new Path("appengine-web.xml"), contents, true, null);

    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    assertNotNull(projectElement);
    assertEquals("standard", projectElement.getEnvironmentType());
    assertEquals("java8", projectElement.getRuntime());
  }

  @Test
  public void testEnvironmentRuntime_yaml_python27() throws CoreException, AppEngineException {
    IProject project = projectCreator.getProject();
    String contents = "runtime: python27";
    AppEngineConfigurationUtil.createConfigurationFile(
        project, new Path("app.yaml"), contents, true, null);

    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    assertNotNull(projectElement);
    assertEquals("standard", projectElement.getEnvironmentType());
    assertEquals("python27", projectElement.getRuntime());
  }

  @Test
  public void testEnvironmentRuntime_yaml_python27_flex() throws CoreException, AppEngineException {
    IProject project = projectCreator.getProject();
    String contents = "runtime: python27\nenv: flex";
    AppEngineConfigurationUtil.createConfigurationFile(
        project, new Path("app.yaml"), contents, true, null);

    AppEngineProjectElement projectElement = AppEngineProjectElement.create(project);
    assertNotNull(projectElement);
    assertEquals("flex", projectElement.getEnvironmentType());
    assertEquals("python27", projectElement.getRuntime());
  }

  @Test
  public void testGetAdapter() throws AppEngineException {
    projectCreator.withFacets(
        AppEngineStandardFacet.JRE7,
        WebFacetUtils.WEB_25,
        JavaFacet.VERSION_1_7);
    IProject project = projectCreator.getProject();

    AppEngineProjectElement projectElement =
        AppEngineProjectElement.create(project);
    assertNotNull(projectElement);
    assertNotNull(projectElement.getAdapter(IFile.class));
  }

  @Test
  public void testHasLayoutChanged_normalFile() {
    IFile file = mock(IFile.class);
    when(file.getProjectRelativePath()).thenReturn(new Path("foo"));
    assertFalse(AppEngineProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }

  @Test
  public void testHasLayoutChanged_otherComponentsFile() {
    IFile file = mock(IFile.class);
    // not in .settings
    when(file.getProjectRelativePath()).thenReturn(new Path("org.eclipse.wst.common.component"));
    assertFalse(AppEngineProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }

  @Test
  public void testHasLayoutChanged_settingsFile() {
    IFile file = mock(IFile.class);
    when(file.getProjectRelativePath()).thenReturn(new Path(".settings/foo"));
    assertFalse(AppEngineProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }

  @Test
  public void testHasLayoutChanged_wtpComponentsFile() {
    IFile file = mock(IFile.class);
    when(file.getProjectRelativePath())
        .thenReturn(new Path(".settings/org.eclipse.wst.common.component"));
    assertTrue(AppEngineProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }

  @Test
  public void testHasLayoutChanged_wtpFacetsFile() {
    IFile file = mock(IFile.class);
    when(file.getProjectRelativePath())
        .thenReturn(new Path(".settings/org.eclipse.wst.common.project.facet.core.xml"));
    assertTrue(AppEngineProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }

  @Test
  public void testHasAppEngineDescriptor_normalFile() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("org.eclipse.wst.common.project.facet.core.xml");
    assertFalse(AppEngineProjectElement.hasAppEngineDescriptor(Collections.singleton(file)));
  }

  @Test
  public void testHasAppEngineDescriptor_appEngineWebXml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("appengine-web.xml");
    assertTrue(AppEngineProjectElement.hasAppEngineDescriptor(Collections.singleton(file)));
  }

  @Test
  public void testHasAppEngineDescriptor_appYaml() {
    IFile file = mock(IFile.class);
    when(file.getName()).thenReturn("app.yaml");
    assertTrue(AppEngineProjectElement.hasAppEngineDescriptor(Collections.singleton(file)));
  }
}
