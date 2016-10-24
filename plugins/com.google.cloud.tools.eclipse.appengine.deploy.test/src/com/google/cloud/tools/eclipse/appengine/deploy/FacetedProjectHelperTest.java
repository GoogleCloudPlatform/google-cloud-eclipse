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

package com.google.cloud.tools.eclipse.appengine.deploy;

import static org.mockito.Mockito.mock;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.junit.Test;

import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;

public class FacetedProjectHelperTest {

  @Test(expected = NullPointerException.class)
  public void testGetFacetedProject_nullArgument() throws CoreException {
    new FacetedProjectHelper().getFacetedProject(null);
  }

  @Test(expected = NullPointerException.class)
  public void testProjectHasFacet_projectNull() {
    new FacetedProjectHelper().projectHasFacet(null, null);
  }

  @Test(expected = NullPointerException.class)
  public void testProjectHasFacet_facetIdNull() {
    new FacetedProjectHelper().projectHasFacet(mock(IFacetedProject.class), null);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testProjectHasFacet_facetIdEmpty() {
    new FacetedProjectHelper().projectHasFacet(mock(IFacetedProject.class), "");
  }
}
