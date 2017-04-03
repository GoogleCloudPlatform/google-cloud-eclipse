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

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
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
  @Mock private org.eclipse.wst.server.core.IRuntime serverRuntime;
  @Mock private IRuntimeType runtimeType;

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

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
}
