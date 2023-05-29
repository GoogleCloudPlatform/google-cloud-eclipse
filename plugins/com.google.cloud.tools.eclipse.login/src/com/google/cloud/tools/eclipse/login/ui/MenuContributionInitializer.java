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

package com.google.cloud.tools.eclipse.login.ui;

import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

public class MenuContributionInitializer implements IStartup {
  
  @Override
  public void earlyStartup() {
    updateLoginCommand();
    GoogleApiFactory.INSTANCE.addCredentialChangeListener(() -> updateLoginCommand());
  }

  static void updateLoginCommand() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    workbench.getDisplay().asyncExec(() ->
        workbench.getService(ICommandService.class).refreshElements(
            "com.google.cloud.tools.eclipse.login.commands.loginCommand", null)); //$NON-NLS-1$
  }
}
