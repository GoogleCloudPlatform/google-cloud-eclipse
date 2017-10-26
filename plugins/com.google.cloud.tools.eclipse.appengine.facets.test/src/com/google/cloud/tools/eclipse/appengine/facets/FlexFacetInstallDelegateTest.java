/*
 * Copyright 2017 Google Inc.
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

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class FlexFacetInstallDelegateTest {

  @Rule public TestProjectCreator wtpProjectCreator = new TestProjectCreator().withFacetVersions(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);

  @Rule public TestProjectCreator javaProjectCreator = new TestProjectCreator().withFacetVersions(
      JavaFacet.VERSION_1_7);

  @Test
  public void testWarFacetInstall() throws CoreException {
    IProject project = wtpProjectCreator.getProject();

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    facetedProject.installProjectFacet(AppEngineFlexWarFacet.FACET_VERSION,
        null /* config */, new NullProgressMonitor());

    Assert.assertTrue(AppEngineFlexWarFacet.hasFacet(facetedProject));
    Assert.assertTrue(project.getFile("src/main/appengine/app.yaml").exists());
  }

  @Test
  public void testJarFacetInstall() throws CoreException {
    IProject project = javaProjectCreator.getProject();

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    facetedProject.installProjectFacet(AppEngineFlexJarFacet.FACET_VERSION,
        null /* config */, new NullProgressMonitor());

    Assert.assertTrue(AppEngineFlexJarFacet.hasFacet(facetedProject));
    Assert.assertTrue(project.getFile("src/main/appengine/app.yaml").exists());
  }
}
