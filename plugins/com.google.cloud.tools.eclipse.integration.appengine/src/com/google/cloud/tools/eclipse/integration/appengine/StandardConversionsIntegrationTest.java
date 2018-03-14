/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.Rule;
import org.junit.Test;

/** Tests for conversion of various App Engine Standard project types. */
public class StandardConversionsIntegrationTest extends BaseProjectTest {
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testReconfigureForJava8() {
    IProject project =
        projectCreator
            .withFacets(AppEngineStandardFacet.JRE7, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25)
            .getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    selected.contextMenu("Configure").menu("Reconfigure for App Engine Java 8 runtime").click();

    IFacetedProject facetedProject = projectCreator.getFacetedProject();
    ProjectUtils.waitForProjects(project);
    assertNotNull(facetedProject.getProjectFacetVersion(AppEngineStandardFacet.FACET));
    assertEquals(
        "JRE8",
        facetedProject.getProjectFacetVersion(AppEngineStandardFacet.FACET).getVersionString());
  }
}
