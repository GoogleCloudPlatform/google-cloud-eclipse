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

package com.google.cloud.tools.eclipse.util;

import com.google.common.base.Preconditions;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class FacetedProjectHelper {

  public static boolean projectHasFacet(IFacetedProject facetedProject, String facetId) {
    Preconditions.checkNotNull(facetedProject, "facetedProject is null");
    Preconditions.checkNotNull(facetId, "facetId is null");
    Preconditions.checkArgument(!facetId.isEmpty(), "facetId is empty string");

    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(facetId);
    return facetedProject.hasProjectFacet(projectFacet);
  }

}
