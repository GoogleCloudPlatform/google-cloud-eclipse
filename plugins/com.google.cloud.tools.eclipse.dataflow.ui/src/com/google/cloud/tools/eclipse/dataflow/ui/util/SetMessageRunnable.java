/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.util;

import org.eclipse.jface.dialogs.DialogPage;

/**
 * A runnable that sets a message on a dialog page.
 */
public class SetMessageRunnable implements Runnable {
  private final DialogPage page;
  private final String message;
  private final int level;

  public static SetMessageRunnable create(DialogPage page, String message, int level) {
    return new SetMessageRunnable(page, message, level);
  }

  private SetMessageRunnable(DialogPage page, String message, int level) {
    this.page = page;
    this.message = message;
    this.level = level;
  }

  @Override
  public void run() {
    page.setMessage(message, level);
  }
}

