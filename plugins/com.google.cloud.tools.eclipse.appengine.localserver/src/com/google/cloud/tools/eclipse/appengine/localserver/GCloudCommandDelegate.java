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
package com.google.cloud.tools.eclipse.appengine.localserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IRuntime;

import com.google.cloud.tools.eclipse.util.OSUtilities;
import com.google.cloud.tools.eclipse.util.ProcessUtilities;
import com.google.common.annotations.VisibleForTesting;

/**
 * Utility class to run gcloud commands.
 */
public class GCloudCommandDelegate {
  public static final String APP_ENGINE_COMPONENT_NAME = "app-engine-java";
  public static final String GCLOUD_CMD = OSUtilities.isWindows() ? "gcloud.cmd" : "gcloud";
  public static final String GCLOUD_DIR = File.separator + "bin" + File.separator + GCLOUD_CMD;
  public static final String GET_VERSION_CMD = GCLOUD_DIR + " version";
  public static final String GET_COMPONENTS_LIST_CMD = GCLOUD_DIR
                                                       + " components list --format=value(id,state.name)";
  public static final String NO_USERS_MESSAGE = "No credentialed accounts.";
  public static final String AVAILABLE_USERS_LIST_PREFIX = "Credentialed accounts:";
  public static final String GET_AUTH_LIST_CMD = GCLOUD_DIR + " auth list";

  /**
   * Returns true if the Cloud SDK and the App Engine component have been installed.
   * Returns false otherwise.
   *
   * @param sdkLocation the location of the Cloud SDK
   * @return true if the Cloud SDK and App Engine component have been installed
   *         and false otherwise
   */
  public static boolean areCloudSdkAndAppEngineInstalled(File sdkLocation) 
      throws IOException, InterruptedException {
    if (!sdkLocation.exists()) {
      throw new InvalidPathException(sdkLocation.getAbsolutePath(), "Path does not exist");
    }

    Process process = Runtime.getRuntime().exec(sdkLocation + GET_COMPONENTS_LIST_CMD);
    String output = ProcessUtilities.getProcessOutput(process);

    // Check process output for Cloud SDK and App Engine status
    return isComponentInstalled(output, APP_ENGINE_COMPONENT_NAME);
  }

  @VisibleForTesting
  public static boolean isComponentInstalled(String output, String componentName) {
    // Sample output:
    // core Update Available
    // gsutil Update Available
    // app-engine-python Update Available
    // gcd-emulator Not Installed
    // pubsub-emulator Not Installed
    // alpha Not Installed
    // beta Not Installed
    // kubectl Not Installed
    // bq Installed
    // gcloud Installed
    // app-engine-java Installed
    Pattern pattern = Pattern.compile(componentName + "\\s*(Update Available|Installed)\\s*");
    return pattern.matcher(output).find();
  }

  /**
   * Checks if there are any accounts logged in via gcloud.
   *
   * @param project an Eclipse project
   * @param serverRuntime a Cloud SDK runtime
   * @return true if one or more accounts have been logged in to gcloud
   * otherwise returns false
   * @throws IOException if an I/O error occurs while communicating with gcloud
   * @throws InterruptedException if the thread for the gcloud process is
   *           interrupted
   */
  public static boolean hasLoggedInUsers(IProject project, IRuntime serverRuntime) 
      throws IOException, InterruptedException {
    if (project == null) {
      throw new NullPointerException("Select a valid project");
    }

    if (serverRuntime == null) {
      throw new NullPointerException("Select a valid runtime for project " + project.getName());
    }

    IPath sdkLocation = serverRuntime.getLocation();
    String command = sdkLocation + GET_AUTH_LIST_CMD;
    Process process = Runtime.getRuntime().exec(command);
    String output = ProcessUtilities.getProcessOutput(process);
    String error = ProcessUtilities.getProcessErrorOutput(process);

    if (error.contains(NO_USERS_MESSAGE)) {
      return false;
    } else if (output.contains(AVAILABLE_USERS_LIST_PREFIX)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Creates a gcloud app run command. If {@code mode} is
   * {@link ILaunchManager#DEBUG_MODE}, it configures the server to be run in
   * debug mode using the "--jvm-flag" and also configures a debugger to be
   * attached to the Cloud SDK server through {@code debugPort}.
   *
   * @param sdkLocation the location of the Cloud SDK
   * @param runnables the application directory of the module to be run on the
   *          server
   * @param mode the launch mode
   * @param apiHost The host and port on which to start the API server (in the
   *          format host:port)
   * @param debugPort the debug port
   *
   * @return a gcloud app run command
   *
   * @throws IllegalStateException if {@code debugPort} is not between 1 and 65535
   * @throws InvalidPathException if either the {@code sdkLocation} or {@code runnables}
   *         denotes a path that does not exist
   * @throws NullPointerException if {@code apiHost} is null
   */
  public static String createAppRunCommand(String sdkLocation,
                                           String runnables,
                                           String mode,
                                           String apiHost,
                                           int apiPort,
                                           int debugPort) throws NullPointerException, InvalidPathException, IllegalStateException {

    if (!(new File(sdkLocation)).exists()) {
      throw new InvalidPathException(sdkLocation, "Path does not exist");
    }

    if (!(new File(runnables)).exists()) {
      throw new InvalidPathException(runnables, "Path does not exist");
    }

    if (apiHost == null) {
      throw new NullPointerException("API host cannot be null");
    }

    StringBuilder builder = new StringBuilder();
    builder.append(sdkLocation)
           .append("/bin/dev_appserver.py ")
           .append(runnables)
           .append(" --api_host ")
           .append(apiHost)
           .append(" --api_port ")
           .append(apiPort);

    if ((mode != null) && mode.equals(ILaunchManager.DEBUG_MODE)) {
      if (debugPort <= 0 || debugPort > 65535) {
        throw new IllegalStateException("Debug port is set to " + debugPort
                                        + ", should be between 1-65535");
      }
      builder.append(" --jvm_flag=-Xdebug");
      builder.append(" --jvm_flag=-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=");
      builder.append(debugPort);
    }

    return builder.toString();
  }

}
