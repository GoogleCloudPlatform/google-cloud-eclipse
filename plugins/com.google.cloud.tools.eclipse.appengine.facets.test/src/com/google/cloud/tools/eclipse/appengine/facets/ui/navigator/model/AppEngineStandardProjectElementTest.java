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

package com.google.cloud.tools.eclipse.appengine.facets.ui.navigator.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.Collections;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineStandardProjectElementTest {
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testGetAdapter() throws AppEngineException {
    projectCreator.withFacets(
        AppEngineStandardFacet.JRE7,
        WebFacetUtils.WEB_25,
        JavaFacet.VERSION_1_7);
    IProject project = projectCreator.getProject();

    AppEngineStandardProjectElement projectElement =
        AppEngineStandardProjectElement.create(project);
    assertNotNull(projectElement);
    assertNotNull(projectElement.getAdapter(IFile.class));
  }

  @Test
  public void testHasLayoutChanged_normalFile() {
    IFile file = mock(IFile.class);
    when(file.getProjectRelativePath()).thenReturn(new Path("foo"));
    assertFalse(AppEngineStandardProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }

  @Test
  public void testHasLayoutChanged_otherComponentsFile() {
    IFile file = mock(IFile.class);
    // not in .settings
    when(file.getProjectRelativePath()).thenReturn(new Path("org.eclipse.wst.common.component"));
    assertFalse(AppEngineStandardProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }

  @Test
  public void testHasLayoutChanged_settingsFile() {
    IFile file = mock(IFile.class);
    when(file.getProjectRelativePath()).thenReturn(new Path(".settings/foo"));
    assertFalse(AppEngineStandardProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }

  @Test
  public void testHasLayoutChanged_wtpComponentsFile() {
    IFile file = mock(IFile.class);
    when(file.getProjectRelativePath())
        .thenReturn(new Path(".settings/org.eclipse.wst.common.component"));
    assertTrue(AppEngineStandardProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }

  @Test
  public void testHasLayoutChanged_wtpFacetsFile() {
    IFile file = mock(IFile.class);
    when(file.getProjectRelativePath())
        .thenReturn(new Path(".settings/org.eclipse.wst.common.project.facet.core.xml"));
    assertTrue(AppEngineStandardProjectElement.hasLayoutChanged(Collections.singleton(file)));
  }
}
