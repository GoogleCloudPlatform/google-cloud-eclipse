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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider.State;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.ui.progress.UIJob;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OptInDialogTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  private Shell shell;
  private OptInDialog dialog;
  private Job dialogCloser;
  
  @Before
  public void setUp() {
    TestAccountProvider.setAsDefaultProvider(State.LOGGED_IN);
    shell = shellResource.getShell();
    dialog = new OptInDialog(shell);
  }

  @After
  public void tearDown() throws InterruptedException {
    if (dialogCloser != null) {
      dialogCloser.join();  // to exit cleanly without any potential job-running warning
    }
  }

  @Test
  public void testPrivacyPolicyLink() {
    dialog.createDialogArea(shell);
    Link link = CompositeUtil.findControl(shell, Link.class);
    assertEquals("Sharing usage statistics is subject to the"
        + " <a href=\"http://www.google.com/policies/privacy/\">Google Privacy Policy</a>.",
        link.getText());
  }

  @Test
  public void testInitialReturnCode() {
    assertEquals(Window.CANCEL, dialog.getReturnCode());
  }

  @Test
  public void testReturnCode_okPressed() {
    scheduleClosingDialogAfterOpen(CloseAction.PRESS_OK);
    assertEquals(Window.OK, dialog.open());
  }

  @Test
  public void testReturnCode_cancelPressed() {
    scheduleClosingDialogAfterOpen(CloseAction.PRESS_CANCEL);
    assertNotEquals(Window.OK, dialog.open());
  }

  @Test
  public void testReturnCode_dialogClosed() {
    scheduleClosingDialogAfterOpen(CloseAction.CLOSE_SHELL);
    assertNotEquals(Window.OK, dialog.open());
  }

  @Test
  public void testReturnCode_dialogDisposed() {
    scheduleClosingDialogAfterOpen(CloseAction.DISPOSE_SHELL);
    assertNotEquals(Window.OK, dialog.open());
  }

  private void scheduleClosingDialogAfterOpen(final CloseAction closeAction) {
    dialogCloser = new UIJob("dialog closer") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if (dialog.getShell() != null && dialog.getShell().isVisible()) {
          closeDialog(closeAction);
        } else {
          schedule(100);
        }
        return Status.OK_STATUS;
      }
    };
    dialogCloser.schedule();
  }

  private enum CloseAction {
    PRESS_OK, PRESS_CANCEL, CLOSE_SHELL, DISPOSE_SHELL
  }

  private void closeDialog(CloseAction closeAction) {
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

      case DISPOSE_SHELL:
        dialog.getShell().dispose();
        break;

      default:
        throw new RuntimeException("bug");
    }
  }
}
