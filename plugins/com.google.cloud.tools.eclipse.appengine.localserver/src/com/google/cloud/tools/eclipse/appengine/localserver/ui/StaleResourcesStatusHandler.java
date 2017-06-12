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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * A simple prompter for the Platform/Debug framework to prompt the user to confirm whether the
 * launch should continue when possibly stale resources have been found.
 */
public class StaleResourcesStatusHandler implements IStatusHandler {
  /**
   * The error code indicating that there may be stale resources. Used with the plugin ID to
   * uniquely identify this prompter.
   */
  static final int CONFIRM_LAUNCH_CODE = 255;

  /**
   * A specially crafted status message that is pass into the Debug Prompter class to obtain our
   * confirmation prompter.
   */
  public static final IStatus CONTINUE_LAUNCH_REQUEST = new Status(IStatus.INFO,
      "com.google.cloud.tools.eclipse.appengine.localserver", CONFIRM_LAUNCH_CODE, "", null);

  @Override
  public Object handleStatus(IStatus status, Object source) throws CoreException {
    if (source instanceof ILaunchConfiguration) {
      ILaunchConfiguration config = (ILaunchConfiguration) source;
      if (DebugUITools.isPrivate(config)) {
        return Boolean.FALSE;
      }
    }

    Shell activeShell = DebugUIPlugin.getShell();
    // should consider using MessageDialogWithToggle?
    return MessageDialog.openQuestion(activeShell, Messages.getString("STALE_RESOURCES_DETECTED"),
        Messages.getString("STALE_RESOURCES_LAUNCH_CONFIRMATION"));
  }
}
