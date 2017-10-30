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

package com.google.cloud.tools.eclipse.usagetracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.swt.widgets.Display;
import org.junit.Test;

public class OptInDialogTest {

  private final OptInDialog dialog = new OptInDialog(null);

  @Test
  public void testIsOptInYes_inUiThread() throws InterruptedException {
    assertTrue(Display.getCurrent() != null);
    try {
      dialog.isOptInYes();
      fail();
    } catch (IllegalStateException ex) {
      assertEquals("Cannot be called from the UI thread.", ex.getMessage());
    }
  }

  @Test
  public void testIsOptInYes_okPressed() throws InterruptedException {
    dialog.okPressed();
    assertTrue(getIsOptInYes());
  }

  @Test
  public void testIsOptInYes_cancelPressed() throws InterruptedException {
    dialog.cancelPressed();
    assertFalse(getIsOptInYes());
  }

  @Test
  public void testIsOptInYes_dialogDismissed() throws InterruptedException {
    dialog.cancelPressed();
    assertFalse(getIsOptInYes());
  }

  private boolean getIsOptInYes() throws InterruptedException {
    final Boolean[] answerHolder = new Boolean[1];
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          answerHolder[0] = dialog.isOptInYes();
        } catch (InterruptedException ex) {}
      }
    });
    thread.start();
    thread.join();
    assertNotNull(answerHolder[0]);
    return answerHolder[0];
  }
}
