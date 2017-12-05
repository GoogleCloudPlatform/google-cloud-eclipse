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

import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OptInDialogTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  private Shell shell;
  private OptInDialog dialog;
  private Boolean answer;

  @Before
  public void setUp() {
    shell = shellResource.getShell();
    dialog = new OptInDialog(shell);
  }

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
  public void testIsOptInYes_okPressed() {
    scheduleCallingIsOptInYes();
    scheduleClosingDialogAfterOpen(CloseAction.PRESS_OK);
    dialog.open();
    assertTrue(answer);
  }

  @Test
  public void testIsOptInYes_cancelPressed() {
    scheduleCallingIsOptInYes();
    scheduleClosingDialogAfterOpen(CloseAction.PRESS_CANCEL);
    dialog.open();
    assertFalse(answer);
  }

  @Test
  public void testIsOptInYes_dialogClosed() {
    scheduleCallingIsOptInYes();
    scheduleClosingDialogAfterOpen(CloseAction.CLOSE_SHELL);
    dialog.open();
    assertFalse(answer);
  }

  private void scheduleCallingIsOptInYes() {
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          answer = dialog.isOptInYes();
        } catch (InterruptedException ex) {}
      }
    });
    thread.start();
  }

  private enum CloseAction { PRESS_OK, PRESS_CANCEL, CLOSE_SHELL };

  private void scheduleClosingDialogAfterOpen(final CloseAction closeAction) {
    shell.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        switch (closeAction) {
          case PRESS_OK:
            Button okButton = CompositeUtil.findButton(dialog.getShell(), "Share");
            assertNotNull(okButton);
            new SWTBotButton(okButton).click();
            break;

          case PRESS_CANCEL:
            Button cancelButton = CompositeUtil.findButton(dialog.getShell(), "Do Not Share");
            assertNotNull(cancelButton);
            new SWTBotButton(cancelButton).click();
            break;

          case CLOSE_SHELL:
            dialog.getShell().close();
            break;

          default:
            throw new RuntimeException("bug");
        };
      }
    });
  }
}
