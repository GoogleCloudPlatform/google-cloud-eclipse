/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.localserver;

//import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
//import com.google.gdt.eclipse.core.OSUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.util.SocketUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;

/**
 * Utility class to run gcloud commands.
 */
public class GCloudCommandDelegate {
  public static final String APP_ENGINE_COMPONENT_NAME = "app-engine-java";
  public static final String GCLOUD_CMD = Platform.getOS().equals(Platform.OS_WIN32) ? "gcloud.cmd" : "gcloud";
  public static final String GCLOUD_DIR = File.separator + "bin" + File.separator + GCLOUD_CMD;
  public static final String GET_VERSION_CMD = GCLOUD_DIR + " version";
  public static final String GET_COMPONENTS_LIST_CMD = GCLOUD_DIR + " components list --format=value(id,state.name)";
  public static final String NO_USERS_MESSAGE = "No credentialed accounts.";
  public static final String AVAILABLE_USERS_LIST_PREFIX = "Credentialed accounts:";
  public static final String GET_AUTH_LIST_CMD = GCLOUD_DIR + " auth list";

  /**
   * Returns true if the Cloud SDK and the App Engine component have been installed.
   * Returns false otherwise.
   *
   * @param sdkLocation the location of the Cloud SDK
   * @return true if the Cloud SDK and App Engine component have been installed and false otherwise
   * @throws IOException
   * @throws InterruptedException
   */
  public static boolean areCloudSdkAndAppEngineInstalled(String sdkLocation)
      throws IOException, InterruptedException {
    if (!(new File(sdkLocation)).exists()) {
      throw new InvalidPathException(sdkLocation, "Path does not exist");
    }

    Process process = Runtime.getRuntime().exec(sdkLocation + GET_COMPONENTS_LIST_CMD);
    String output = getProcessOutput(process);

    // Check process output for Cloud SDK and App Engine status
    return isComponentInstalled(output, APP_ENGINE_COMPONENT_NAME);
  }

//  @VisibleForTesting
  public static boolean isComponentInstalled(String output, String componentName) {
	  //TODO: currently works, but needs more parsing: 
	  // gcloud components list --format="value(id,state.name)"
	  //TODO: soon to be implemented and easier parsing:
	  // gcloud components list --filter=id:app-engine-java --format='value(state.name)' 2>/dev/null
    // Sample output:
	// core	Update Available
	// gsutil	Update Available
	// app-engine-python	Update Available
	// gcd-emulator	Not Installed
	// pubsub-emulator	Not Installed
	// alpha	Not Installed
	// beta	Not Installed
	// kubectl	Not Installed
	// bq	Installed
	// gcloud	Installed
	// app-engine-java	Installed
    Pattern pattern =
        Pattern.compile(componentName + "\\s*(Update Available|Installed)\\s*");
    return pattern.matcher(output).find();
  }

  /**
   * Returns true if one or more accounts have been logged in to gcloud otherwise returns false.
   *
   * @param project an Eclipse project
   * @param serverRuntime a Cloud SDK runtime
   * @return
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the thread for the gcloud process is interrupted
   */
  public static boolean hasLoggedInUsers(IProject project,
      org.eclipse.wst.server.core.IRuntime serverRuntime) throws IOException, InterruptedException {
    if (project == null) {
      throw new NullPointerException("Select a valid project");
    }

    if (serverRuntime == null) {
      throw new NullPointerException("Select a valid runtime for project " + project.getName());
    }

    IPath sdkLocation = serverRuntime.getLocation();
    String command = sdkLocation + GET_AUTH_LIST_CMD;
    Process process = Runtime.getRuntime().exec(command);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ByteStreams.copy(process.getInputStream(), bos);
    process.waitFor();
    String output = new String(bos.toByteArray());

    // Get error output
    ByteArrayOutputStream bosError = new ByteArrayOutputStream();
    ByteStreams.copy(process.getErrorStream(), bosError);
    String error = new String(bosError.toByteArray());

    if (error.contains(NO_USERS_MESSAGE)) {
      return false;
    } else if (output.contains(AVAILABLE_USERS_LIST_PREFIX)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Creates a gcloud app run command. If {@code mode} is {@link ILaunchManager#DEBUG_MODE},
   * it configures the server to be run in debug mode using the "--jvm-flag" and also
   * configures a debugger to be attached to the Cloud SDK server through {@code port}.
   *
   * @param sdkLocation the location of the Cloud SDK
   * @param runnables the application directory of the module to be run on the server
   * @param mode the launch mode
   * @param apiHost The host and port on which to start the API server (in the format host:port)
   * @param debugPort the debug port
   * @return a gcloud app run command
   */
  // TODO: update the command to include the CloudSdkServer.SERVER_PROGRAM_FLAGS set by the user.
  public static String createAppRunCommand(String sdkLocation, String runnables, String mode,
      String apiHost, int apiPort, int debugPort) throws NullPointerException, InvalidPathException {

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
    	  throw new IllegalStateException("Debug port is set to " + debugPort + ", should be between 1-65535");
      }
      builder.append(" --jvm_flag=-Xdebug");
      builder.append(" --jvm_flag=-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=");
      builder.append(debugPort);
    }

    return builder.toString();
  }

  /**
   * Note: this method waits until the completion of the process.
   */
  private static String getProcessOutput(Process process) throws IOException, InterruptedException {
    InputStream diffStream = process.getInputStream();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ByteStreams.copy(diffStream, bos);
    process.waitFor();
    return new String(bos.toByteArray());
  }
}
