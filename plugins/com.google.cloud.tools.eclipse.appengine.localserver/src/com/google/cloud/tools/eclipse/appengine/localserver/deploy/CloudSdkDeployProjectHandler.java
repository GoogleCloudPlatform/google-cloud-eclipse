/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.localserver.deploy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IRuntime;

import com.google.cloud.tools.eclipse.appengine.localserver.Activator;
import com.google.cloud.tools.eclipse.appengine.localserver.CloudSdkUtils;
import com.google.cloud.tools.eclipse.appengine.localserver.GCloudCommandDelegate;
import com.google.cloud.tools.eclipse.util.ActiveProjectFinder;
import com.google.cloud.tools.eclipse.util.MessageConsoleUtilities;
import com.google.cloud.tools.eclipse.util.ProcessUtilities;

/**
 * Handler for the Cloud SDK deploy action.
 */
public class CloudSdkDeployProjectHandler extends AbstractHandler {
  public static final String TITLE = "Cloud SDK Deploy";
  private static final String WAR_SRC_DIR_DEFAULT = "src/main/webapp";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
	Shell activeShell = HandlerUtil.getActiveShell(event);

	// Get initial project selection
    final IProject project = ActiveProjectFinder.getSelectedProject(event);
    if (project == null) {
      Activator.logAndDisplayError(activeShell, TITLE, "Cannot find selected project");
      return null;
    }

    try {
      final IRuntime runtime = CloudSdkUtils.getPrimaryRuntime(project);
      if (runtime == null) {
        Activator.logAndDisplayError(activeShell,
                                     TITLE,
                                     "Must select a primary runtime for " + project.getName());
        return null;
      }
      
      boolean hasLoggedInUsers = GCloudCommandDelegate.hasLoggedInUsers(project, runtime);
      if (!hasLoggedInUsers) {
        Activator.logAndDisplayError(activeShell,
                                     TITLE,
                                     "Please sign in to gcloud before deploying project "
                                            + project.getName());
        return null;
      }

      final IPath warLocation = getWarLocationOrPrompt(project, activeShell);
      if (warLocation == null) {
        Activator.logAndDisplayError(null,
                                     TITLE,
                                     "Must select the WAR directory to deploy " + project.getName());
        return null;
      }

      final IPath sdkLocation = runtime.getLocation();
      if (sdkLocation == null) {
        Activator.logAndDisplayError(null, TITLE, "Set the location of " + runtime.getId());
        return null;
      }

      Job job = new Job("Running App Engine Deploy Action") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            List<String> commands = new ArrayList<>();
            commands.add(sdkLocation + GCloudCommandDelegate.GCLOUD_DIR);

            commands.add("preview");
            commands.add("app");
            commands.add("deploy");
            commands.add(warLocation.toString());

            MessageConsole messageConsole = MessageConsoleUtilities.getMessageConsole(project.getName()
                                                                                      + " - "
                                                                                      + TITLE,
                                                                                      null);
            messageConsole.activate();

            IPath projectLocation = project.getLocation();

            int exitCode = ProcessUtilities.launchProcessAndWaitFor(commands,
                                                                    projectLocation.toFile(),
                                                                    messageConsole.newMessageStream());

            if (exitCode != 0) {
              Activator.logAndDisplayError(null,
                                           TITLE,
                                           "Cloud SDK deploy action terminated with exit code "
                                              + exitCode);
            }
          } catch (IOException | InterruptedException e) {
            Activator.logError(e);
          }
          return Status.OK_STATUS;
        }
      };
      job.schedule();
      return null;
    } catch (CoreException | IOException | InterruptedException e) {
      Activator.logAndDisplayError(null, TITLE, e.getMessage());
      return null;
    }
  }

  private IPath getWarLocationOrPrompt(final IProject project, final Shell shell) {
    IFolder warDir = project.getFolder(WAR_SRC_DIR_DEFAULT);
    if (warDir.exists()) {
      return warDir.getLocation();
    }

    final IPath[] fileSystemPath = new IPath[1];
    shell.getDisplay().syncExec(new Runnable() {
      @Override
      public void run() {
        DirectoryDialog dialog = new DirectoryDialog(shell);
        dialog.setText("WAR Directory Selection");
        dialog.setMessage("Select the WAR directory");
        dialog.setFilterPath(project.getLocation().toOSString());
        String pathString = dialog.open();
        if (pathString != null) {
          fileSystemPath[0] = new Path(pathString);
        }
      }
    });
    return fileSystemPath[0];
  }
}
