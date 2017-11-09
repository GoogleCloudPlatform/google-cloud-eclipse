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

package com.google.cloud.tools.eclipse.dataflow.ui.page;

import org.eclipse.jface.dialogs.DialogPage;

/**
 * A {@link MessageTarget} that sets messages on a {@code DialogPage}.
 */
public class DialogPageMessageTarget implements MessageTarget {
  private final DialogPage target;

  public DialogPageMessageTarget(DialogPage target) {
    this.target = target;
  }

  @Override
  public void setInfo(String message) {
    target.setMessage(message, DialogPage.INFORMATION);
  }

  @Override
  public void setError(String message) {
    target.setMessage(message, DialogPage.ERROR);
  }

  @Override
  public void clear() {
    target.setMessage(null);
  }
}

