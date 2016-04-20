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
package com.google.cloud.tools.eclipse.appengine.localserver.server;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.google.cloud.tools.eclipse.appengine.localserver.Activator;

/**
 * A {@link ServerBehaviourDelegate} for Google Cloud SDK.
 */
public class CloudSdkServerBehaviour extends ServerBehaviourDelegate {
  private CloudSdkServerStartupChecker cloudSdkServerStartupChecker;
  private IDebugEventSetListener processListener;

  @Override
  public IStatus canStart(String launchMode) {
    // Check that the port is not in use before start
    CloudSdkServer server = CloudSdkServer.getCloudSdkServer(getServer());
    int port = server.getApiPort();
    if (!isPortAvailable(port)) {
      String message = server.getServer().getName() + " has API port set to "
                       + port
                       + " which is already is in use.\nTo use port "
                       + port
                       + " stop the processes that are using it";
      Activator.logError(message);
      return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
    }
    return Status.OK_STATUS;
  }

  @Override
  public void stop(boolean force) {
    terminate();
  }

  /**
   * Notifies that the server started.
   */
  public void setServerStarted() {
    setServerState(IServer.STATE_STARTED);
  }

  /**
   * Finalizes preparations to launch server.
   *
   * @param launchMode the mode in which a server is running
   */
  protected void setupLaunch(String launchMode) {
    setServerRestartState(false);
    setServerState(IServer.STATE_STARTING);
    setMode(launchMode);
    String adminHost = CloudSdkServer.getCloudSdkServer(getServer()).getApiHost();
    int adminPort = CloudSdkServer.getCloudSdkServer(getServer()).getApiPort();

    // ping server to check for startup
    String url = "http://" + adminHost + ":" + adminPort;
    cloudSdkServerStartupChecker = new CloudSdkServerStartupChecker(getServer(), url, Integer.MAX_VALUE, this);
    cloudSdkServerStartupChecker.start();
  }

  /**
   * Terminates the Cloud SDK server instance, stops the ping thread and removes
   * debug listener.
   */
  protected void terminate() {
    int serverState = getServer().getServerState();
    if ((serverState == IServer.STATE_STOPPED) || (serverState == IServer.STATE_STOPPING)) {
      return;
    }

    try {
      setServerState(IServer.STATE_STOPPING);
      stopDevAppServer(); // sends a "quit" message to port
      ILaunch launch = getServer().getLaunch();
      if (launch != null) {
        launch.terminate();
      }

      if (cloudSdkServerStartupChecker != null) {
        cloudSdkServerStartupChecker.stop();
        cloudSdkServerStartupChecker = null;
      }

      if (processListener != null) {
        DebugPlugin.getDefault().removeDebugEventListener(processListener);
        processListener = null;
      }

      terminateRemoteDebugger();

      setServerState(IServer.STATE_STOPPED);
    } catch (DebugException e) {
      Activator.logError("Error terminating the Cloud SDK server", e);
    }
  }

  private void terminateRemoteDebugger() {
	try {
	  CloudSdkServer server = CloudSdkServer.getCloudSdkServer(getServer());
      ILaunchConfigurationWorkingCopy remoteDebugLaunchConfig = server.getRemoteDebugLaunchConfig();
      if (remoteDebugLaunchConfig != null) {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        for (ILaunch launch : manager.getLaunches()) {
          if (launch.getLaunchConfiguration().equals(remoteDebugLaunchConfig)) {
            launch.terminate();
            manager.removeLaunch(launch);
            return;
          }
        }
      }
	} catch (DebugException e) {
      Activator.logError(e);
    }
  }

  /**
   * Sets up process listener to be able to handle debug events for
   * {@code newProcess}.
   *
   * @param newProcess the process to be monitored
   */
  protected void addProcessListener(final IProcess newProcess) {
    if (processListener != null || newProcess == null) {
      return;
    }

    processListener = new IDebugEventSetListener() {
      @Override
      public void handleDebugEvents(DebugEvent[] events) {
        if (events != null) {
          for (int i = 0; i < events.length; i++) {
            if (newProcess.equals(events[i].getSource())
                && events[i].getKind() == DebugEvent.TERMINATE) {
              terminate();
            }
          }
        }
      }
    };
    DebugPlugin.getDefault().addDebugEventListener(processListener);
  }

  private boolean isPortAvailable(int port) {
    try (Socket socket = new Socket("localhost", port)) {
      // connection established to the port, so some process is using it
      return false;
    } catch (IOException e) {
      return true;
    }
  }

  private void stopDevAppServer() {
    CloudSdkServer server = CloudSdkServer.getCloudSdkServer(getServer());
    try {
      new URL("http", server.getHostName(), server.getAdminPort(), "/quit").getContent();
      try {
        // TODO: confirm appropriate delay time
        Thread.sleep(4000);
      } catch (InterruptedException e) {
        // ignore
      }
    } catch (IOException e) {
      Activator.logError("Error stopping the dev app server", e);
    }
  }
}
