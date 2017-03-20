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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import com.google.cloud.tools.appengine.api.devserver.DefaultRunConfiguration;
import com.google.cloud.tools.appengine.api.devserver.RunConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkOutOfDateException;
import com.google.cloud.tools.eclipse.appengine.localserver.Activator;
import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.appengine.localserver.PreferencesInitializer;
import com.google.cloud.tools.eclipse.appengine.localserver.ui.LocalAppEngineConsole;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.ServerUtil;

public class LocalAppEngineServerLaunchConfigurationDelegate
    extends AbstractJavaLaunchConfigurationDelegate {

  private final static Logger logger =
      Logger.getLogger(LocalAppEngineServerLaunchConfigurationDelegate.class.getName());

  public static final String[] SUPPORTED_LAUNCH_MODES =
      {ILaunchManager.RUN_MODE, ILaunchManager.DEBUG_MODE};

  private static final String DEBUGGER_HOST = "localhost"; //$NON-NLS-1$

  /**
   * Returns {@code value} unless it's null or empty, then returns {@code nullOrEmptyValue}.
   *
   * @see Strings#isNullOrEmpty(String)
   */
  private static String ifEmptyOrNull(String value, String nullOrEmptyValue) {
    return !Strings.isNullOrEmpty(value) ? value : nullOrEmptyValue;
  }

  private static int ifNull(Integer value, int nullValue) {
    return value != null ? value : nullValue;
  }

  private static void validateCloudSdk() throws CoreException  {
    try {
      CloudSdk cloudSdk = new CloudSdk.Builder().build();
      cloudSdk.validateCloudSdk();
    } catch (CloudSdkNotFoundException ex) {
      String detailMessage = Messages.getString("cloudsdk.not.configured"); //$NON-NLS-1$
      Status status = new Status(IStatus.ERROR,
          "com.google.cloud.tools.eclipse.appengine.localserver", detailMessage, ex); //$NON-NLS-1$
      throw new CoreException(status);
    } catch (CloudSdkOutOfDateException ex) {
      String detailMessage = Messages.getString("cloudsdk.out.of.date"); //$NON-NLS-1$
      Status status = new Status(IStatus.ERROR,
          "com.google.cloud.tools.eclipse.appengine.deploy.ui", detailMessage); //$NON-NLS-1$
      throw new CoreException(status);
    }
  }

  @Override
  public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    IServer thisServer = ServerUtil.getServer(configuration);
    DefaultRunConfiguration thisConfig = generateServerRunConfiguration(configuration, thisServer);

    for (ILaunch launch : getLaunchManager().getLaunches()) {
      if (launch.isTerminated()
          || launch.getLaunchConfiguration().getType() != configuration.getType()) {
        continue;
      }
      IServer otherServer = ServerUtil.getServer(launch.getLaunchConfiguration());
      DefaultRunConfiguration otherConfig =
          generateServerRunConfiguration(launch.getLaunchConfiguration(), otherServer);
      IStatus conflicts = checkConflicts(thisConfig, otherConfig,
          new MultiStatus(Activator.PLUGIN_ID, 0,
              MessageFormat.format("Conflicts with running server \"{0}\"", otherServer.getName()),
              null));
      if (!conflicts.isOK()) {
        throw new CoreException(StatusUtil.filter(conflicts));
      }
    }
    return super.getLaunch(configuration, mode);
  }

  /**
   * Create a CloudSdk RunConfiguration corresponding to the launch configuration and server
   * defaults. Details are pulled from {@link ILaunchConfiguration#getAttributes() launch
   * attributes} and {@link IServer server settings and attributes}.
   */
  @VisibleForTesting
  DefaultRunConfiguration generateServerRunConfiguration(ILaunchConfiguration configuration,
      IServer server) throws CoreException {

    DefaultRunConfiguration devServerRunConfiguration = new DefaultRunConfiguration();
    // Iterate through our different configurable parameters
    // TODO: storage-related paths, incl storage_path and the {blob,data,*search*,logs} paths

    // TODO: allow setting host from launch config
    if (server.getHost() != null) {
      devServerRunConfiguration.setHost(server.getHost());
    }

    // TODO: make this a configurable option in the launch config?
    // default to 1 instance to simplify debugging
    devServerRunConfiguration.setMaxModuleInstances(1);

    // don't restart server when on-disk changes detected
    devServerRunConfiguration.setAutomaticRestart(false);


    int serverPort = getPortAttribute(LocalAppEngineServerBehaviour.SERVER_PORT_ATTRIBUTE_NAME,
        LocalAppEngineServerBehaviour.DEFAULT_SERVER_PORT, configuration, server);
    if (serverPort >= 0) {
      devServerRunConfiguration.setPort(serverPort);
    }

    String adminHost = getAttribute(LocalAppEngineServerBehaviour.ADMIN_HOST_ATTRIBUTE_NAME,
        LocalAppEngineServerBehaviour.DEFAULT_ADMIN_HOST, configuration, server);
    if (!Strings.isNullOrEmpty(adminHost)) {
      devServerRunConfiguration.setAdminHost(adminHost);
    }

    int adminPort = getPortAttribute(LocalAppEngineServerBehaviour.ADMIN_PORT_ATTRIBUTE_NAME,
        -1, configuration, server);
    if (adminPort >= 0) {
      devServerRunConfiguration.setAdminPort(adminPort);
    } else {
      // adminPort = -1 perform failover if default port is busy
      devServerRunConfiguration.setAdminPort(LocalAppEngineServerBehaviour.DEFAULT_ADMIN_PORT);
      // adminHost == null is ok as that resolves to null == INADDR_ANY
      InetAddress addr = resolveAddress(devServerRunConfiguration.getAdminHost());
      if (org.eclipse.wst.server.core.util.SocketUtil.isPortInUse(addr,
          devServerRunConfiguration.getAdminPort())) {
        logger.log(Level.INFO, "default admin port " + devServerRunConfiguration.getAdminPort()
            + " in use. Picking an unused port.");
        devServerRunConfiguration.setAdminPort(0);
      }
    }

    // TODO: apiPort?
    // vmArguments is exactly as supplied by the user in the dialog box
    String vmArgumentString = getVMArguments(configuration);
    List<String> vmArguments = Arrays.asList(DebugPlugin.parseArguments(vmArgumentString));
    if (!vmArguments.isEmpty()) {
      devServerRunConfiguration.setJvmFlags(vmArguments);
    }

    return devServerRunConfiguration;
  }

  /**
   * Retrieve and resolve a string attribute. If not specified, returns {@code defaultValue}.
   */
  private static String getAttribute(String attributeName, String defaultValue,
      ILaunchConfiguration configuration, IServer server) {
    try {
      if (configuration.hasAttribute(attributeName)) {
        String result = configuration.getAttribute(attributeName, "");
        if (result != null) {
          return result;
        }
      }
    } catch (CoreException ex) {
      logger.log(Level.WARNING, "Unable to retrieve " + attributeName, ex);
    }
    return server.getAttribute(attributeName, defaultValue);
  }

  /**
   * Resolve a host or IP address to an IP address.
   *
   * @return an {@link InetAddress}, or {@code null} if unable to be resolved (equivalent to
   *         {@code INADDR_ANY})
   */
  static InetAddress resolveAddress(String ipOrHost) {
    if (!Strings.isNullOrEmpty(ipOrHost)) {
      if (InetAddresses.isInetAddress(ipOrHost)) {
        return InetAddresses.forString(ipOrHost);
      }
      try {
        InetAddress[] addresses = InetAddress.getAllByName(ipOrHost);
        return addresses[0];
      } catch (UnknownHostException ex) {
        logger.info("Unable to resolve '" + ipOrHost + "' to an address");
      }
    }
    return null;
  }

  /**
   * Pull out a port from the specified attribute on the given {@link ILaunchConfiguration} or
   * {@link IServer} instance.
   *
   * @param defaultPort the port if no port attributes are found
   * @return the port, or {@code defaultPort} if no port was found
   */
  @VisibleForTesting
  static int getPortAttribute(String attributeName, int defaultPort,
      ILaunchConfiguration configuration, IServer server) {
    int port = -1;
    try {
      port = configuration.getAttribute(attributeName, -1);
    } catch (CoreException ex) {
      logger.log(Level.WARNING, "Unable to retrieve " + attributeName, ex);
    }
    if (port < 0) {
      port = server.getAttribute(attributeName, defaultPort);
    }
    return port;
  }

  /**
   * Check for known conflicting settings.
   */
  @VisibleForTesting
  static IStatus checkConflicts(RunConfiguration ours, RunConfiguration theirs,
      MultiStatus status) {
    Class<?> clazz = LocalAppEngineServerLaunchConfigurationDelegate.class;
    // use {0,number,#} to avoid localized port numbers
    if (equalPorts(ours.getPort(), theirs.getPort(),
        LocalAppEngineServerBehaviour.DEFAULT_SERVER_PORT)) {
      status.add(StatusUtil.error(clazz,
          MessageFormat.format("server port: {0,number,#}",
              ifNull(ours.getPort(), LocalAppEngineServerBehaviour.DEFAULT_SERVER_PORT))));
    }
    if (equalPorts(ours.getAdminPort(), theirs.getAdminPort(),
        LocalAppEngineServerBehaviour.DEFAULT_ADMIN_PORT)) {
      status.add(StatusUtil.error(clazz,
          MessageFormat.format("admin port: {0,number,#}",
              ifNull(ours.getAdminPort(), LocalAppEngineServerBehaviour.DEFAULT_ADMIN_PORT))));
    }
    if (equalPorts(ours.getApiPort(), theirs.getApiPort(), 0)) {
      // ours.getAdminPort() will never be null with a 0 default
      Preconditions.checkNotNull(ours.getApiPort());
      status.add(StatusUtil.error(clazz,
          MessageFormat.format("API port: {0,number,#}", ours.getAdminPort())));
    }

    // Check the storage paths:
    // TODO: include the APP_ID as it is used to generate the default storage path
    // XXX: does it matter if storage_path is same if all other paths are explicitly specified
    // (e.g., the {blob,data,*search*,logs} paths)
    if (Objects.equals(ours.getStoragePath(), theirs.getStoragePath())) {
      status.add(StatusUtil.error(clazz, MessageFormat.format("storage path: {0}",
          ifEmptyOrNull(ours.getStoragePath(), "<default location>"))));
    }

    return status;
  }

  /** Compare whether two port specs map to the same port. */
  @VisibleForTesting
  static boolean equalPorts(Integer ours, Integer theirs, int defaultValue) {
    if (ours == null) {
      ours = defaultValue;
    }
    if (theirs == null) {
      theirs = defaultValue;
    }
    if (ours == 0 || theirs == 0) {
      return false;
    }
    return ours.equals(theirs);
  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, final ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.APP_ENGINE_LOCAL_SERVER,
        AnalyticsEvents.APP_ENGINE_LOCAL_SERVER_MODE, mode);

    validateCloudSdk();

    IServer server = ServerUtil.getServer(configuration);
    if (server == null) {
      String message = "There is no App Engine development server available";
      Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
      throw new CoreException(status);
    } else if (server.getServerState() != IServer.STATE_STOPPED) {
      String message = "Server is already in operation";
      Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
      throw new CoreException(status);
    }
    IModule[] modules = server.getModules();
    if (modules == null || modules.length == 0) {
      return;
    }


    LocalAppEngineServerBehaviour serverBehaviour = (LocalAppEngineServerBehaviour) server
        .loadAdapter(LocalAppEngineServerBehaviour.class, null);

    setDefaultSourceLocator(launch, configuration);

    List<File> runnables = new ArrayList<>();
    for (IModule module : modules) {
      IPath deployPath = serverBehaviour.getModuleDeployDirectory(module);
      runnables.add(deployPath.toFile());
    }

    LocalAppEngineConsole console =
        MessageConsoleUtilities.findOrCreateConsole(configuration.getName(),
            new LocalAppEngineConsole.Factory(serverBehaviour));
    console.clearConsole();
    console.activate();

    new ServerLaunchMonitor(launch, server).engage();

    // todo: programArguments is currently ignored
    if (!Strings.isNullOrEmpty(getProgramArguments(configuration))) {
      logger.warning("App Engine Local Server currently ignores program arguments");
    }
    DefaultRunConfiguration devServerRunConfiguration =
        generateServerRunConfiguration(configuration, server);
    devServerRunConfiguration.setAppYamls(runnables);

    if (ILaunchManager.DEBUG_MODE.equals(mode)) {
      int debugPort = getDebugPort();
      setupDebugTarget(devServerRunConfiguration, launch, debugPort, monitor);
    } else {
      // A launch must have at least one debug target or process, or it otherwise becomes a zombie
      LocalAppEngineServerDebugTarget.addTarget(launch, serverBehaviour);
    }
    serverBehaviour.startDevServer(devServerRunConfiguration, console.newMessageStream());
  }

  /**
   * Listen for the server to enter STARTED and open a web browser on the server's main page
   */
  protected void openBrowserPage(final IServer server) {
    if (!shouldOpenStartPage()) {
      return;
    }
    final String pageLocation = determinePageLocation(server);
    if (pageLocation == null) {
      return;
    }
    WorkbenchUtil.openInBrowserInUiThread(pageLocation, server.getId(), server.getName(), server.getName());
  }

  private void setupDebugTarget(DefaultRunConfiguration devServerRunConfiguration, ILaunch launch,
      int debugPort, IProgressMonitor monitor) throws CoreException {
    if (debugPort <= 0 || debugPort > 65535) {
      throw new IllegalArgumentException("Debug port is set to " + debugPort //$NON-NLS-1$
          + ", should be between 1-65535"); //$NON-NLS-1$
    }
    List<String> jvmFlags = new ArrayList<>();
    if (devServerRunConfiguration.getJvmFlags() != null) {
      jvmFlags.addAll(devServerRunConfiguration.getJvmFlags());
    }
    jvmFlags.add("-Xdebug"); //$NON-NLS-1$
    jvmFlags.add("-Xrunjdwp:transport=dt_socket,server=n,suspend=y,quiet=y,address=" + debugPort); //$NON-NLS-1$
    devServerRunConfiguration.setJvmFlags(jvmFlags);

    // The 4.7 listen connector supports a connectionLimit
    IVMConnector connector =
        JavaRuntime.getVMConnector(IJavaLaunchConfigurationConstants.ID_SOCKET_LISTEN_VM_CONNECTOR);
    if (connector == null || !connector.getArgumentOrder().contains("connectionLimit")) { //$NON-NLS-1$
      // Attempt to retrieve our socketListenerMultipleConnector
      connector = JavaRuntime.getVMConnector(
          "com.google.cloud.tools.eclipse.jdt.launching.socketListenerMultipleConnector"); //$NON-NLS-1$
    }
    if (connector == null) {
      abort("Cannot find Socket Listening connector", null, 0); //$NON-NLS-1$
      return; // keep JDT null analysis happy
    }

    // Set JVM debugger connection parameters
    @SuppressWarnings("deprecation")
    int timeout = JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT);
    Map<String, String> connectionParameters = new HashMap<>();
    connectionParameters.put("hostname", DEBUGGER_HOST); //$NON-NLS-1$
    connectionParameters.put("port", Integer.toString(debugPort)); //$NON-NLS-1$
    connectionParameters.put("timeout", Integer.toString(timeout)); //$NON-NLS-1$
    connectionParameters.put("connectionLimit", "0"); //$NON-NLS-1$ //$NON-NLS-2$
    connector.connect(connectionParameters, monitor, launch);
  }

  private int getDebugPort() throws CoreException {
    int port = SocketUtil.findFreePort();
    if (port == -1) {
      abort("Cannot find free port for remote debugger", null, IStatus.ERROR); //$NON-NLS-1$
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

  @VisibleForTesting
  static String determinePageLocation(IServer server) {
    LocalAppEngineServerBehaviour serverBehaviour = (LocalAppEngineServerBehaviour)
        server.loadAdapter(LocalAppEngineServerBehaviour.class, null /* monitor */);

    return "http://" + server.getHost() + ":" + serverBehaviour.getServerPort(); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode)
      throws CoreException {
    IProject[] projects = getReferencedProjects(configuration);
    return computeBuildOrder(projects);
  }

  @Override
  protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode)
      throws CoreException {
    IProject[] projects = getReferencedProjects(configuration);
    return computeReferencedBuildOrder(projects);
  }

  private static IProject[] getReferencedProjects(ILaunchConfiguration configuration)
      throws CoreException {
    IServer thisServer = ServerUtil.getServer(configuration);
    IModule[] modules = ModuleUtils.getAllModules(thisServer);
    Set<IProject> projects = new HashSet<>();
    for (IModule module : modules) {
      IProject project = module.getProject();
      if (project != null) {
        projects.add(project);
      }
    }
    return projects.toArray(new IProject[projects.size()]);
  }

  /**
   * Monitors the server and launch state. Ensures listeners are properly removed on server stop
   * and launch termination. It is necessary in part as there may be several launches per
   * LaunchConfigurationDelegate.
   * <ul>
   * <li>On server start, open a browser page</li>
   * <li>On launch-termination, stop the server.</li>
   * <li>On server stop, terminate the launch</li>
   * </ul>
   */
  private class ServerLaunchMonitor implements ILaunchesListener2, IServerListener {
    private ILaunch launch;
    private IServer server;

    /**
     * Setup the monitor.
     */
    ServerLaunchMonitor(ILaunch launch, IServer server) {
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
          openBrowserPage(server);
          return;

        case IServer.STATE_STOPPED:
          disengage();
          try {
            logger.fine("Server stopped; terminating launch"); //$NON-NLS-1$
            launch.terminate();
          } catch (DebugException ex) {
            logger.log(Level.WARNING, "Unable to terminate launch", ex); //$NON-NLS-1$
          }
      }
    }

    @Override
    public void launchesTerminated(ILaunch[] launches) {
      for (ILaunch terminated : launches) {
        if (terminated == launch) {
          disengage();
          if (server.getServerState() == IServer.STATE_STARTED) {
            logger.fine("Launch terminated; stopping server"); //$NON-NLS-1$
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
