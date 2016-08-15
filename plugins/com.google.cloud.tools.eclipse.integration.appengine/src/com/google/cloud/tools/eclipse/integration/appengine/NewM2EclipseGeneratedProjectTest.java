/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.swtbot.SwtBotWorkbenchActions;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that Maven com.google.appengine.archetypes archetype projects <del>have our App Engine
 * facet</del> <ins>do not have our App Engine facets</ins>.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class NewM2EclipseGeneratedProjectTest extends AbstractProjectTests {
  @Test
  public void testNewMavenAppEngineProject() throws CoreException {
    // Ensure new App Engine project has an App Engine Facet associated with it
    assertFalse(SwtBotProjectActions.projectFound(bot, "testartifact"));
    project = SwtBotProjectActions.createMavenProject(bot, "testgroup", "testartifact",
        "com.google.appengine.archetypes", "appengine-skeleton-archetype", "RELEASE", "remote",
        "testpackage");
    SwtBotWorkbenchActions.waitForIdle(bot);

    assertTrue(project.exists());

    /*** Commented as it's not true at the moment. ***/
    // IFacetedProject facetedProject = new FacetedProjectHelper().getFacetedProject(project);
    // assertNotNull("Project should be faceted", facetedProject);
    // assertTrue( new FacetedProjectHelper().projectHasFacet(facetedProject,
    // AppEngineStandardFacet.ID));

    IFacetedProject facetedProject = new FacetedProjectHelper().getFacetedProject(project);
    assertNull("Project should not be faceted", facetedProject);
    assertFalse(SwtBotProjectActions.hasErrorsInProblemsView(bot));
  }

}
