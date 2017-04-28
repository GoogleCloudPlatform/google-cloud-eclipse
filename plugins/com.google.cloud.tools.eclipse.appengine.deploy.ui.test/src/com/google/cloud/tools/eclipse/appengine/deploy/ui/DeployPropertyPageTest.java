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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class DeployPropertyPageTest<P extends DeployPreferencesPanel> {

  @Rule
  public ShellTestResource shellTestResource = new ShellTestResource();
  @Mock
  private IGoogleLoginService loginService;
  @Mock
  private IGoogleApiFactory googleApiFactory;

  @Test
  public void testCorrectPanelIsShownForFacetedProject() {
    DeployPropertyPage page = new DeployPropertyPage(loginService, googleApiFactory);
    Shell parent = shellTestResource.getShell();
    page.setElement(getProject());
    page.createControl(parent);
    page.setVisible(true);
    Composite preferencePageComposite = (Composite) parent.getChildren()[0];
    for (Control control : preferencePageComposite.getChildren()) {
      if (control instanceof Composite) {
        Composite maybeDeployPageComposite = (Composite) control;
        Layout layout = maybeDeployPageComposite.getLayout();
        if (layout instanceof StackLayout) {
          StackLayout stackLayout = (StackLayout) layout;
          assertThat(stackLayout.topControl, instanceOf(getPanelClass()));
          return;
        }
      }
    }
    fail("Did not find the deploy preferences panel");
  }

  protected abstract IProject getProject();

  protected abstract Class<P> getPanelClass();

}