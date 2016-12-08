/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.Test;

public class DeployPropertyPageTest extends AbstractProjectTests {
  @Test
  public void testPropertyPageTitle_standardProject() throws CoreException {
    String projectName = "foo";
    project = SwtBotAppEngineActions.createNativeWebAppProject(bot, projectName, null, null);
    IFacetedProject facetedProject = new FacetedProjectHelper().getFacetedProject(project);
    assertNotNull("Native App Engine projects should be faceted", facetedProject);
    assertTrue(
        new FacetedProjectHelper().projectHasFacet(facetedProject, AppEngineStandardFacet.ID));

    SwtBotProjectActions.openProjectProperties(bot, projectName);
    bot.tree().expandNode("Google Cloud Platform").select("App Engine Deployment");
    bot.textWithLabel("App Engine Deployment - Standard Environment");
  }

}
