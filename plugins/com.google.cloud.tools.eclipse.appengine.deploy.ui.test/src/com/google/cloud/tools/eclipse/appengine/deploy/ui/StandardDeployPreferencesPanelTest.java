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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.appengine.login.ui.AccountSelectorObservableValue;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StandardDeployPreferencesPanelTest {

  private Composite parent;
  private Shell shell;
  @Mock
  private IProject project;
  @Mock
  private IGoogleLoginService loginService;
  @Mock
  private Runnable layoutChangedHandler;

  @Before
  public void setUp() throws Exception {
    shell = new Shell(Display.getDefault());
    parent = new Composite(shell, SWT.NONE);
    when(project.getName()).thenReturn("testProject");
  }

  @After
  public void tearDown() {
    if (parent != null) {
      parent.dispose();
    }
    if (shell != null) {
      shell.dispose();
    }
  }

  @Test
  public void testValidationMessageWhenNotSignedIn() {
    StandardDeployPreferencesPanel deployPanel = new StandardDeployPreferencesPanel(parent, project, loginService, layoutChangedHandler, true);
    assertThat(getAccountSelectorValidationStatus(deployPanel), is("Sign in to Google."));
  }

  @Test
  public void testValidationMessageWhenSignedIn() {
    when(loginService.hasAccounts()).thenReturn(true);
    StandardDeployPreferencesPanel deployPanel = new StandardDeployPreferencesPanel(parent, project, loginService, layoutChangedHandler, true);
    assertThat(getAccountSelectorValidationStatus(deployPanel), is("Select an account."));
  }

  private String getAccountSelectorValidationStatus(StandardDeployPreferencesPanel deployPanel) {
    for (Object object : deployPanel.getDataBindingContext().getValidationStatusProviders()) {
      ValidationStatusProvider statusProvider = (ValidationStatusProvider) object;
      if (!statusProvider.getTargets().isEmpty()) {
        if (statusProvider.getTargets().get(0) instanceof AccountSelectorObservableValue) {
          IStatus status = (IStatus) statusProvider.getValidationStatus().getValue();
          return status.getMessage();
        }
      }
    }
    fail("Could not find AccountSelector databinding to verify validation");
    return null;
  }

}
