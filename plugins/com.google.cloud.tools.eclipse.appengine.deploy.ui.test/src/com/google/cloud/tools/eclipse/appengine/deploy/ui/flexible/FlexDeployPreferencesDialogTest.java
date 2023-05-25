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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FlexDeployPreferencesDialogTest {

  private static final String GCP_PRICING_MESSAGE = "There is no free quota for the App Engine "
      + "flexible environment. Visit <a href=\"https://cloud.google.com/appengine/pricing\">"
      + "GCP Pricing</a> for pricing information.";

  @Rule
  public ShellTestResource shellResource = new ShellTestResource();

  private FlexDeployPreferencesDialog dialog;

  @Before
  public void setUp() {
    TestAccountProvider.setAsDefaultProvider();
    IProject project = mock(IProject.class);
    when(project.getName()).thenReturn("");
    when(project.getLocation()).thenReturn(new Path("/"));
    dialog = new FlexDeployPreferencesDialog(null, "title", project);
  }

  @Test
  public void testFlexPricingLabel() {
    dialog.setBlockOnOpen(false);
    dialog.open();
    Composite dialogArea = (Composite) dialog.createDialogArea(shellResource.getShell());

    assertNotNull(findGcpPricingLink(dialogArea));
  }

  private static Control findGcpPricingLink(Composite dialogArea) {
    return CompositeUtil.findControl(dialogArea, control -> control instanceof Link
        && GCP_PRICING_MESSAGE.equals(((Link) control).getText()));
  }
}
