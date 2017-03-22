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

package com.google.cloud.tools.eclipse.bugreport.ui;

import com.google.common.annotations.VisibleForTesting;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class BugReportCommandHandler extends AbstractHandler {

  @VisibleForTesting
  static final String BUG_REPORT_URL =
      "https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
      browserSupport.getExternalBrowser().openURL(new URL(BUG_REPORT_URL));
    } catch (MalformedURLException | PartInitException ex) {
      throw new ExecutionException("Failed to open external browser", ex);
    }
    return null;
  }

}
