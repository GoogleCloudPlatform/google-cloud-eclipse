/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.standard.java8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Tests that changing the App Engine Standard Facet results in appropriate {@code <runtime>}
 * changes in the {@code appengine-web.xml}.
 */
public class AppEngineStandardFacetVersionChangeTest {
  @Rule
  public TestProjectCreator jre7Project = new TestProjectCreator()
      .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, AppEngineStandardFacet.JRE7);

  @Rule
  public TestProjectCreator jre8Project = new TestProjectCreator()
      .withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25,
          AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8);

  @Test
  public void testJre8FacetVersionId() {
    assertEquals("JRE8",
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8.getVersionString());
  }

  /** Fail changing an App Engine Standard JRE7 project to JRE8 with no other changes. */
  @Test
  public void testChange_AESJ7_AESJ8() throws CoreException, IOException, SAXException, AppEngineException {
    IFacetedProject project = jre7Project.getFacetedProject();
    assertDescriptorRuntimeIsJre7(project);

    try {
      Set<Action> actions = new HashSet<>();
      actions.add(new Action(Action.Type.VERSION_CHANGE,
          AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8, null));
      project.modify(actions, null);
      fail("should fail as still has Java 7 facet");
    } catch (CoreException ex) {
      // expected
      assertDescriptorRuntimeIsJre7(project);
    }
  }

  /** Fail changing AppEngine Standard JRE8 to JRE7 with no other changes. */
  @Test
  public void testChange_AESJ8_AESJ7() throws CoreException, IOException, SAXException, AppEngineException {
    IFacetedProject project = jre8Project.getFacetedProject();
    assertDescriptorRuntimeIsJre8(project);

    try {
      Set<Action> actions = new HashSet<>();
      actions.add(new Action(Action.Type.VERSION_CHANGE, AppEngineStandardFacet.JRE7, null));
      project.modify(actions, null);
      fail("should fail as still has Java 8 facet");
    } catch (CoreException ex) {
      // expected
      assertDescriptorRuntimeIsJre8(project);
    }
  }

  /**
   * Changing AppEngine Standard JRE8 to JRE7+Java7 facet should be reverted: we only accept changes
   * via the appengine-web.xml.
   */
  @Test
  public void testChange_AESJ8_AESJ7andJava7() throws CoreException, IOException, SAXException, AppEngineException {
    IFacetedProject project = jre8Project.getFacetedProject();
    assertDescriptorRuntimeIsJre8(project);

    Set<Action> actions = new HashSet<>();
    actions.add(new Action(Action.Type.VERSION_CHANGE, AppEngineStandardFacet.JRE7, null));
    actions.add(new Action(Action.Type.VERSION_CHANGE, JavaFacet.VERSION_1_7, null));
    project.modify(actions, null);

    // the autobuilder should revert the change
    ProjectUtils.waitForProjects(project.getProject());
    assertEquals(
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        project.getProjectFacetVersion(AppEngineStandardFacet.FACET));
    assertEquals(JavaFacet.VERSION_1_8, project.getProjectFacetVersion(JavaFacet.FACET));
    assertDescriptorRuntimeIsJre8(project);
  }

  private static AppEngineDescriptor parseDescriptor(IFacetedProject project)
      throws IOException, CoreException, SAXException {
    IFile descriptorFile =
        AppEngineConfigurationUtil.findConfigurationFile(
            project.getProject(), new Path("appengine-web.xml"));
    assertNotNull("appengine-web.xml not found", descriptorFile);
    assertTrue("appengine-web.xml does not exist", descriptorFile.exists());

    try (InputStream is = descriptorFile.getContents()) {
      return AppEngineDescriptor.parse(is);
    }
  }

  private static void assertDescriptorRuntimeIsJre7(IFacetedProject project)
      throws IOException, CoreException, SAXException, AppEngineException {
    AppEngineDescriptor descriptor = parseDescriptor(project);
    assertNotNull(descriptor);
    assertEquals("should report java7 runtime", "java7", descriptor.getRuntime());
  }

  private static void assertDescriptorRuntimeIsJre8(IFacetedProject project)
      throws IOException, CoreException, SAXException, AppEngineException {
    AppEngineDescriptor descriptor = parseDescriptor(project);
    assertNotNull(descriptor);
    assertTrue("missing <runtime>java8</runtime>", descriptor.isJava8());
  }

}
