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

package com.google.cloud.tools.eclipse.ui.util.console;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.program.Program;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.IHyperlink;

/**
 * {@link IHyperlink} implementation that uses {@link IWorkbenchBrowserSupport} to open the URL.
 */
public class BrowserSupportBasedHyperlink implements IHyperlink {

  static final Logger logger = Logger.getLogger(BrowserSupportBasedHyperlink.class.toString());
  private String url;


  BrowserSupportBasedHyperlink(String url) {
    this.url = url;
  }

  @Override
  public void linkExited() {
  }

  @Override
  public void linkEntered() {
  }

  @Override
  public void linkActivated() {
    try {
      IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
      browserSupport.createBrowser(null).openURL(new URL(url));
    } catch (PartInitException partInitException) {
      logger.log(Level.SEVERE, "Cannot open hyperlink using browser support, will try SWT's Program.launch(String)",
                 partInitException);
      if (!Program.launch(url)) {
        logger.log(Level.SEVERE, "Cannot open hyperlink using SWT's Program.launch(String)");
      }
    } catch (MalformedURLException malformedURLException) {
      logger.log(Level.SEVERE, "Cannot open hyperlink", malformedURLException);
    }
  }
}
