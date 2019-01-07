/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.validation;

import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineWebXmlValidationTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator().withFacets(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, AppEngineStandardFacet.JRE7);

  @Test
  public void testValidElementsInAppEngineWebXml() throws CoreException {
    IProject project = projectCreator.getProject();
    IFile appEngineWebXml =
        AppEngineConfigurationUtil.findConfigurationFile(project, new Path("appengine-web.xml"));
    assertTrue(appEngineWebXml.exists());

    String contents = "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>\n"
        + "  <staging>\n"
        + "    <enable-jar-classes>true</enable-jar-classes>\n"
        + "  </staging>\n"
        + "  <automatic-scaling>\n"
        + "    <min-instances>0</min-instances>\n"
        + "  </automatic-scaling>\n"
        + "</appengine-web-app>";
    InputStream in = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
    appEngineWebXml.setContents(in, true, false, null);

    ProjectUtils.waitForProjects(project);
    assertTrue(ProjectUtils.waitUntilNoBuildErrors(project));
  }
}
