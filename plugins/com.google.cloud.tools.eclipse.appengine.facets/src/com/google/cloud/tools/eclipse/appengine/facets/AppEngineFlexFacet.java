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

import org.eclipse.wst.common.project.facet.core.IFacetedProject;

import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;

public class AppEngineFlexFacet {
  public static final String ID = "com.google.cloud.tools.eclipse.appengine.facets.flex";

  /**
   * Returns true if project has the App Engine Flex facet and false otherwise.
   *
   * @param project should not be null
   * @return true if project has the App Engine Flex facet and false otherwise
   */
  public static boolean hasAppEngineFacet(IFacetedProject project) {
    FacetedProjectHelper facetedProjectHelper = new FacetedProjectHelper();
    return facetedProjectHelper.projectHasFacet(project, ID);
  }

}
