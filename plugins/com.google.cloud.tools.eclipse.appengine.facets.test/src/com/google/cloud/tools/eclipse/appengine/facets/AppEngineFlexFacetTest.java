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

package com.google.cloud.tools.eclipse.appengine.facets;

import static org.mockito.Mockito.when;

import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineFlexFacetTest {
  @Mock private IFacetedProject facetedProject;

  @Test
  public void testFlexFacetExists() {
    Assert.assertTrue(
        ProjectFacetsManager.isProjectFacetDefined("com.google.cloud.tools.eclipse.appengine.facets.flex"));
  }

  @Test
  public void testHasAppEngineFacet_withFacet() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexFacet.ID);
    when(facetedProject.hasProjectFacet(projectFacet)).thenReturn(true);

    Assert.assertTrue(AppEngineFlexFacet.hasAppEngineFacet(facetedProject));
  }

  @Test
  public void testHasAppEngineFacet_withoutFacet() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexFacet.ID);
    when(facetedProject.hasProjectFacet(projectFacet)).thenReturn(false);

    Assert.assertFalse(AppEngineFlexFacet.hasAppEngineFacet(facetedProject));
  }
}
