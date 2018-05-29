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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.swtbot.SwtBotTreeUtilities;
import com.google.cloud.tools.eclipse.swtbot.SwtBotWorkbenchActions;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IPageLayout;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests the App Engine content block for the Project Explorer. */
public class ProjectExplorerAppEngineBlockTest extends BaseProjectTest {
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testAppEngineStandardJava8() {
    IProject project =
        projectCreator
            .withFacets(
                AppEngineStandardFacet.FACET.getVersion("JRE8"),
                JavaFacet.VERSION_1_8,
                WebFacetUtils.WEB_31)
            .getProject();
    SWTBotView explorer = SwtBotWorkbenchActions.openViewById(bot, IPageLayout.ID_PROJECT_EXPLORER);
    assertNotNull(explorer);
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, explorer, project.getName());
    assertNotNull(selected);
    selected.expand();

    SwtBotTreeUtilities.waitUntilTreeHasItems(bot, selected);
    SWTBotTreeItem appEngineNode = selected.getNode(0);
    SwtBotTreeUtilities.waitUntilTreeTextMatches(
        bot, appEngineNode, startsWith("App Engine [standard: java8]"));
    appEngineNode.expand();
    // no children since we have no additional config files
    assertThat(appEngineNode.getNodes(), hasSize(0));

    // ensure App Engine content block does not appear in Deployment Descriptor
    SWTBotTreeItem deploymentDescriptorNode = selected.getNode(1);
    SwtBotTreeUtilities.waitUntilTreeTextMatches(
        bot, deploymentDescriptorNode, startsWith("Deployment Descriptor:"));
    deploymentDescriptorNode.expand();
    SwtBotTreeUtilities.waitUntilTreeHasItems(bot, deploymentDescriptorNode);
    SWTBotTreeItem firstNode = deploymentDescriptorNode.getNode(0);
    SwtBotTreeUtilities.waitUntilTreeTextMatches(bot, firstNode, not(startsWith("App Engine")));
  }
}
