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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FlexDeployPreferencesDialogTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  private FlexDeployPreferencesDialog dialog;

  @Before
  public void setUp() {
    IProject project = mock(IProject.class);
    when(project.getName()).thenReturn("");
    dialog = new FlexDeployPreferencesDialog(null, "title", project,
        mock(IGoogleLoginService.class), mock(IGoogleApiFactory.class));
  }

  @Test
  public void testFlexPricingLabel() {
    dialog.setBlockOnOpen(false);
    dialog.open();
    Composite dialogArea = (Composite) dialog.createDialogArea(shellResource.getShell());

    assertTrue(hasLinkWithText(dialogArea, "There is no free quota for the App Engine flexible "
        + "environment. Visit <a href=\"https://cloud.google.com/appengine/pricing\">GCP Pricing"
        + "</a> for pricing information."));
  }

  private static boolean hasLinkWithText(Composite composite, String text) {
    for (Control control : composite.getChildren()) {
      if (control instanceof Link) {
        if (((Link) control).getText().equals(text)) {
          return true;
        }
      } else if (control instanceof Composite) {
        if (hasLinkWithText((Composite) control, text)) {
          return true;
        }
      }
    }
    return false;
  }
}
