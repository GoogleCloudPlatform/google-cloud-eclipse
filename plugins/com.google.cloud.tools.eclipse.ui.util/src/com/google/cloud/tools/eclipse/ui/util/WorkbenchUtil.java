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

package com.google.cloud.tools.eclipse.ui.util;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.UIJob;

public class WorkbenchUtil {
  private static final Logger logger = Logger.getLogger(WorkbenchUtil.class.getName());

  /**
   * Open the specified file in the editor.
   *
   * @param workbench the active workbench
   * @param file the file to open
   */
  public static IEditorPart openInEditor(IWorkbench workbench, IFile file) {
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window != null && file != null) {
      IWorkbenchPage page = window.getActivePage();
      try {
        return IDE.openEditor(page, file, true);
      } catch (PartInitException ex) {
        // ignore; we don't have to open the file
      }
    }
    return null;
  }

  /**
   * Opens the specified url in a Web browser instance in a UI thread.
   *
   * @param urlPath the URL to display
   * @param browserId if an instance of a browser with the same id is already opened, it will be
   *   returned instead of creating a new one. Passing null will create a new instance with a
   *   generated id.
   * @param name a name displayed on the tab of the internal browser
   * @param tooltip the text for a tooltip on the <code>name</code> of the internal browser
   */
  public static void openInBrowserInUiThread(final String urlPath, final String browserId,
      final String name, final String tooltip) {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    Job launchBrowserJob = new UIJob(workbench.getDisplay(), name) {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        return openInBrowser(workbench, urlPath, browserId, name, tooltip);
      }

    };
    launchBrowserJob.schedule();
  }

  /**
   * Opens the specified url in a Web browser instance.
   *
   * @param workbench the current workbench
   * @param urlPath the URL to display
   * @param browserId if an instance of a browser with the same id is already opened, it will be
   *   returned instead of creating a new one. Passing null will create a new instance with a
   *   generated id.
   * @param name a name displayed on the tab of the internal browser
   * @param tooltip the text for a tooltip on the <code>name</code> of the internal browser
   * @return resulting status of the operation
   */
  public static IStatus openInBrowser(IWorkbench workbench, String urlPath, String browserId,
      String name, String tooltip) {
    try {
      URL url = new URL(urlPath);
      IWorkbenchBrowserSupport browserSupport = workbench.getBrowserSupport();
      int style = IWorkbenchBrowserSupport.LOCATION_BAR
          | IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.STATUS;
      browserSupport.createBrowser(style, browserId, name, tooltip).openURL(url);
    } catch (PartInitException ex) {
      // Unable to use the normal browser support, so push to the OS
      logger.log(Level.WARNING, "Cannot launch a browser", ex);
      Program.launch(urlPath);
    } catch (MalformedURLException ex) {
      return StatusUtil.error(WorkbenchUtil.class, Messages.getString("invalid.url", urlPath), ex);
    }
    return Status.OK_STATUS;
  }

  /**
   * Opens the specified url in a Web browser instance.
   *
   * @param workbench the current workbench
   * @param urlPath the URL to display
   * @return resulting status of the operation
   */
  public static IStatus openInBrowser(IWorkbench workbench, String urlPath) {
    return openInBrowser(workbench, urlPath, null, null, null);
  }
}
