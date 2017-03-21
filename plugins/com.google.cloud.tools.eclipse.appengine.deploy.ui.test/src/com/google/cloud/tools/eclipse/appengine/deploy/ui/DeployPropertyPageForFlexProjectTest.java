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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.Arrays;
import org.eclipse.core.resources.IProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Ignore;
import org.junit.Rule;

// until Flex facet is enabled
@Ignore
public class DeployPropertyPageForFlexProjectTest
    extends AbstractDeployPropertyPageForProjectsTest<FlexDeployPreferencesPanel> {

  // commented out until Flex facet is enabled
//  @Rule
//  public TestProjectCreator flexProjectCreator =
//      new TestProjectCreator().withFacetVersions(Arrays.asList(
//          JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, APP_ENGINE_FLEX_FACET_1));

  @Override
  protected IProject getProject() {
    // commented out until Flex facet is enabled
//    return flexProjectCreator.getProject();
    return null;
  }

  @Override
  protected Class<FlexDeployPreferencesPanel> getPanelClass() {
    return FlexDeployPreferencesPanel.class;
  }
}
