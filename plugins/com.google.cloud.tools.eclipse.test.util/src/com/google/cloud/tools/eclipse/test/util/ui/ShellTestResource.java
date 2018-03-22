/*
 * Copyright 2016 Google LLC. All Rights Reserved.
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

package com.google.cloud.tools.eclipse.test.util.ui;

import static org.junit.Assert.assertNotNull;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.rules.ExternalResource;

public class ShellTestResource extends ExternalResource {

  private Display display;
  private Shell shell;

  public Display getDisplay() {
    return display;
  }

  public Shell getShell() {
    return shell;
  }

  @Override
  protected void before() {
    display = Display.getDefault();
    assertNotNull(display);
    shell = new Shell(display);
  }

  @Override
  protected void after() {
    shell.dispose();
  }
}
