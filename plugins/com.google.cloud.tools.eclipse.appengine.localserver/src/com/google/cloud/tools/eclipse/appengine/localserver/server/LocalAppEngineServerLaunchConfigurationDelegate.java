/*
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
 */

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import com.google.cloud.tools.eclipse.appengine.localserver.Activator;
import com.google.cloud.tools.eclipse.appengine.localserver.PreferencesInitializer;
import com.google.cloud.tools.eclipse.appengine.localserver.ui.LocalAppEngineConsole;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.common.base.Preconditions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.ServerUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalAppEngineServerLaunchConfigurationDelegate
    extends AbstractJavaLaunchConfigurationDelegate {

  private final static Logger logger =
      Logger.getLogger(LocalAppEngineServerLaunchConfigurationDelegate.class.getName());

  private static final String DEBUGGER_HOST = "localhost";

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, final ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.APP_ENGINE_LOCAL_SERVER,
        AnalyticsEvents.APP_ENGINE_LOCAL_SERVER_MODE, mode);

    IServer server = ServerUtil.getServer(configuration);
    if (server == null) {
      String message = "There is no App Engine development server available";
      Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
      throw new CoreException(status);
    }
    IModule[] modules = server.getModules();
    if (modules == null || modules.length == 0) {
      return;
    }

    LocalAppEngineServerBehaviour serverBehaviour =
        (LocalAppEngineServerBehaviour) server.loadAdapter(LocalAppEngineServerBehaviour.class, null);

    List<File> runnables = new ArrayList<File>();
    for (IModule module : modules) {
      IPath deployPath = serverBehaviour.getModuleDeployDirectory(module);
      runnables.add(deployPath.toFile());
    }

    LocalAppEngineConsole console =
        MessageConsoleUtilities.findOrCreateConsole(configuration.getName(),
                                            new LocalAppEngineConsole.Factory(serverBehaviour));
    console.clearConsole();
    console.activate();

    new ServerLaunchMonitor(configuration, launch, server).engage();

    if (ILaunchManager.DEBUG_MODE.equals(mode)) {
      int debugPort = getDebugPort();
      setupDebugTarget(launch, configuration, debugPort, monitor);
      serverBehaviour.startDebugDevServer(
          runnables, console.newMessageStream(), server.getHost(), debugPort);
    } else {
      // A launch must have at least one debug target or process, or it otherwise becomes a zombie
      LocalAppEngineServerDebugTarget.addTarget(launch, serverBehaviour);
      serverBehaviour.startDevServer(runnables, console.newMessageStream(), server.getHost());
    }
  }

  /**
   * Listen for the server to enter STARTED and open a web browser on the server's main page
   */
  protected void openBrowserPage(final ILaunchConfiguration configuration, final IServer server) {
    if (!shouldOpenStartPage()) {
      return;
    }
    final String pageLocation = determinePageLocation(server, configuration);
    if (pageLocation == null) {
      return;
    }
    final IWorkbench workbench = PlatformUI.getWorkbench();

    Job openJob = new UIJob(workbench.getDisplay(), "Launching start page") {

      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if (server.getServerState() != IServer.STATE_STARTED) {
          return Status.CANCEL_STATUS;
        }
        try {
          URL url = new URL(pageLocation);
          IWorkbenchBrowserSupport browserSupport = workbench.getBrowserSupport();
          int style = IWorkbenchBrowserSupport.LOCATION_BAR
              | IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.STATUS;
          browserSupport.createBrowser(style, server.getId(), server.getName(), server.getName())
              .openURL(url);
        } catch (PartInitException ex) {
          // Unable to use the normal browser support, so punt to the OS
          logger.log(Level.WARNING, "Cannot launch a browser", ex);
          Program.launch(pageLocation);
        } catch (MalformedURLException ex) {
          logger.log(Level.SEVERE, "Invalid dev_appserver URL", ex);
        }
        return Status.OK_STATUS;
      }
    };
    openJob.schedule();
  }

  private void setupDebugTarget(ILaunch launch, ILaunchConfiguration configuration, int port,
      IProgressMonitor monitor) throws CoreException {
    // Attempt to retrieve our socketListenerMultipleConnector first: our fragment versions are
    // setup to ensure that it will not be installed on 4.7 (Oxygen) or beyond
    IVMConnector connector = JavaRuntime.getVMConnector(
        "com.google.cloud.tools.eclipse.jdt.launching.socketListenerMultipleConnector");
    if (connector == null) {
      // The 4.7 listen connector supports an acceptCount
      connector = JavaRuntime
          .getVMConnector(IJavaLaunchConfigurationConstants.ID_SOCKET_LISTEN_VM_CONNECTOR);
    }
    if (connector == null) {
      abort("Cannot find Socket Listening connector", null, 0);
      return; // keep JDT null analysis happy
    }

    // Set JVM debugger connection parameters
    @SuppressWarnings("deprecation")
    int timeout = JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT);
    Map<String, String> connectionParameters = new HashMap<>();
    connectionParameters.put("hostname", DEBUGGER_HOST);
    connectionParameters.put("port", Integer.toString(port));
    connectionParameters.put("timeout", Integer.toString(timeout));
    connectionParameters.put("acceptCount", "0");
    connector.connect(connectionParameters, monitor, launch);
  }

  private int getDebugPort() throws CoreException {
    int port = SocketUtil.findFreePort();
    if (port == -1) {
      abort("Cannot find free port for remote debugger", null, IStatus.ERROR);
    }
    return port;
  }

  /**
   * @return true if we should open a browser on the start page on successful launch
   */
  private boolean shouldOpenStartPage() {
    return Platform.getPreferencesService().getBoolean(Activator.PLUGIN_ID,
        PreferencesInitializer.LAUNCH_BROWSER, true, null);
  }

  private String determinePageLocation(IServer server, ILaunchConfiguration config) {
    // todo[issue #259]: pull this from the server or launch configuration
    return "http://" + DEBUGGER_HOST + ":8080";
  }


  /**
   * Monitors the server and launch state. Ensures listeners are properly removed on server stop and
   * launch termination. It is necessary in part as there may be several launches per
   * LaunchConfigurationDelegate.
   * <ul>
   * <li>On server start, open a browser page</li>
   * <li>On launch-termination, stop the server.</li>
   * <li>On server stop, terminate the launch</li>
   * </ul>
   */
  private class ServerLaunchMonitor implements ILaunchesListener2, IServerListener {
    private ILaunchConfiguration configuration;
    private ILaunch launch;
    private IServer server;

    /**
     * Setup the monitor.
     */
    ServerLaunchMonitor(ILaunchConfiguration configuration, ILaunch launch, IServer server) {
      this.configuration = configuration;
      this.launch = launch;
      this.server = server;
    }

    /** Add required listeners */
    private void engage() {
      getLaunchManager().addLaunchListener(this);
      server.addServerListener(this);
    }

    /** Remove any installed listeners */
    private void disengage() {
      getLaunchManager().removeLaunchListener(this);
      server.removeServerListener(this);
    }

    @Override
    public void serverChanged(ServerEvent event) {
      Preconditions.checkState(server == event.getServer());
      switch (event.getState()) {
        case IServer.STATE_STARTED:
          openBrowserPage(configuration, server);
          return;

        case IServer.STATE_STOPPED:
          disengage();
          try {
            logger.fine("Server stopped; terminating launch");//$NON-NLS-1$
            launch.terminate();
          } catch (DebugException ex) {
            logger.log(Level.WARNING, "Unable to terminate launch", ex);
          }
      }
    }

    @Override
    public void launchesTerminated(ILaunch[] launches) {
      for (ILaunch terminated : launches) {
        if (terminated == launch) {
          disengage();
          if (server.getServerState() == IServer.STATE_STARTED) {
            logger.fine("Launch terminated; stopping server");//$NON-NLS-1$
            server.stop(false);
          }
          return;
        }
      }
    }

    @Override
    public void launchesRemoved(ILaunch[] launches) {}

    @Override
    public void launchesChanged(ILaunch[] launches) {}

    @Override
    public void launchesAdded(ILaunch[] launches) {}
  }
}
