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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntimeType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineStandardFacetTest {
  private static final IProjectFacetVersion APPENGINE_STANDARD_FACET = ProjectFacetsManager
      .getProjectFacet(AppEngineStandardFacet.ID).getVersion(AppEngineStandardFacet.VERSION);

  @Mock private org.eclipse.wst.server.core.IRuntime serverRuntime;
  @Mock private IRuntimeType runtimeType;

  @Rule
  public TestProjectCreator baseProjectCreator = new TestProjectCreator();

  @Rule
  public TestProjectCreator appEngineProjectCreator =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
          APPENGINE_STANDARD_FACET);

  @Test
  public void testStandardFacetExists() {
    Assert.assertTrue(ProjectFacetsManager
        .isProjectFacetDefined("com.google.cloud.tools.eclipse.appengine.facets.standard"));
  }

  @Test
  public void testIsAppEngineStandardRuntime_appEngineRuntime() {
    when(runtimeType.getId()).thenReturn(AppEngineStandardFacet.DEFAULT_RUNTIME_ID);
    when(serverRuntime.getRuntimeType()).thenReturn(runtimeType);

    Assert.assertTrue(AppEngineStandardFacet.isAppEngineStandardRuntime(serverRuntime));
  }

  @Test
  public void testIsAppEngineStandardRuntime_nonAppEngineRuntime() {
    when(runtimeType.getId()).thenReturn("some id");
    when(serverRuntime.getRuntimeType()).thenReturn(runtimeType);

    Assert.assertFalse(AppEngineStandardFacet.isAppEngineStandardRuntime(serverRuntime));
  }

  @Test
  public void testFacetLabel() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID);

    Assert.assertEquals("App Engine Java Standard Environment", projectFacet.getLabel());
  }

  @Test
  public void testGetProjectFacetVersion_noFacet() {
    IProjectFacetVersion facetVersion =
        AppEngineStandardFacet.getProjectFacetVersion(baseProjectCreator.getProject());
    assertNull(facetVersion);
  }

  @Test
  public void testGetProjectFacetVersion_withFacet() {
    IProjectFacetVersion facetVersion =
        AppEngineStandardFacet.getProjectFacetVersion(appEngineProjectCreator.getProject());
    assertEquals(APPENGINE_STANDARD_FACET, facetVersion);
  }

  @Test(expected = NullPointerException.class)
  public void testCheckServletApiSupport_noFacet() {
    AppEngineStandardFacet.checkServletApiSupport(baseProjectCreator.getProject(), "2.5");
  }

  @Test
  public void testCheckServletApiSupport_withFacet() {
    assertTrue(
        AppEngineStandardFacet.checkServletApiSupport(appEngineProjectCreator.getProject(), "2.5"));
  }

  @Test
  public void testCheckServletApiSupport_nullVersion() {
    assertFalse(AppEngineStandardFacet.checkServletApiSupport(APPENGINE_STANDARD_FACET, null));
  }

  @Test
  public void testCheckServletApiSupport_invalidVersion() {
    assertFalse(AppEngineStandardFacet.checkServletApiSupport(APPENGINE_STANDARD_FACET, "2.6"));
  }

  @Test
  public void testCheckServletApiSupport_sameVersion() {
    assertTrue(AppEngineStandardFacet.checkServletApiSupport(APPENGINE_STANDARD_FACET, "2.5"));
  }

  @Test
  public void testCheckServletApiSupport_earlierVersion() {
    assertFalse(AppEngineStandardFacet.checkServletApiSupport(APPENGINE_STANDARD_FACET, "2.4"));
  }
}
