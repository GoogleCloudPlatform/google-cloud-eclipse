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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.appengine.AppEngineDescriptor;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
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
 * Test changing the Java Facet to/from Java 8 results in {@code <runtime>} changes in the
 * {@code appengine-web.xml}.
 */
public class JavaFacetChangeTests {
  @Rule
  public TestProjectCreator testProject = new TestProjectCreator()
      .withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_31,
          AppEngineStandardFacet.FACET_VERSION);

  @Test
  public void testChangeToAndFrom_1_8() throws CoreException, IOException, SAXException {
    IFacetedProject project = testProject.getFacetedProject();
    IFile descriptorFile =
        WebProjectUtil.findInWebInf(project.getProject(), new Path("appengine-web.xml"));
    assertTrue(descriptorFile.exists());

    try (InputStream is = descriptorFile.getContents()) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(is);
      assertFalse(descriptor.isJava8());
    }

    Set<Action> actions = new HashSet<>();
    actions.add(new Action(Action.Type.VERSION_CHANGE, JavaFacet.VERSION_1_8, null));
    project.modify(actions, null);
    ProjectUtils.waitForProjects(project.getProject());

    try (InputStream is = descriptorFile.getContents()) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(is);
      assertTrue(descriptor.isJava8());
    }

    actions = new HashSet<>();
    actions.add(new Action(Action.Type.VERSION_CHANGE, JavaFacet.VERSION_1_7, null));
    project.modify(actions, null);
    ProjectUtils.waitForProjects(project.getProject());

    try (InputStream is = descriptorFile.getContents()) {
      AppEngineDescriptor descriptor = AppEngineDescriptor.parse(is);
      assertFalse(descriptor.isJava8());
    }
  }

}
