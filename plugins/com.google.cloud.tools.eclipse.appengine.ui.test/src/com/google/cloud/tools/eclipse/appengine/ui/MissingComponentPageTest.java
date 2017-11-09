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

package com.google.cloud.tools.eclipse.appengine.ui;

import static org.junit.Assert.assertFalse;

import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.jface.wizard.WizardPage;
import org.junit.Rule;
import org.junit.Test;

public class MissingComponentPageTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  @Test
  public void testPageComplete() {
    WizardPage page = new MissingComponentPage("pageName", "title", "errorMessage", "message");
    page.createControl(shellResource.getShell());
    // Should be false to disable "Finish", which will prevent sending Analytics pings.
    assertFalse(page.isPageComplete());
  }

}
