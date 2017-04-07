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

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.api.devserver.AppEngineDevServer;
import com.google.cloud.tools.appengine.api.devserver.DefaultRunConfiguration;
import com.google.cloud.tools.appengine.api.devserver.DefaultStopConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineDevServer1;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import com.google.cloud.tools.eclipse.appengine.localserver.Activator;
import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.sdk.ui.MessageConsoleWriterOutputLineListener;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.IModulePublishHelper;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.SocketUtil;

/**
 * A {@link ServerBehaviourDelegate} for DevAppServer.
 * <p>
 * {@link IModulePublishHelper} is implemented for addons such as the GWT Plugin that wish to add
 * content during local deploys.
 */
public class LocalAppEngineServerBehaviour extends ServerBehaviourDelegate
    implements IModulePublishHelper {

  @VisibleForTesting // and should be replaced by Java8 BiFunction
  public interface PortChecker {
    boolean isInUse(InetAddress addr, int port);
  }

  /** Parse the numeric string. Return {@code defaultValue} if non-numeric. */
  private static int parseInt(String numeric, int defaultValue) {
    try {
      return Integer.parseInt(numeric);
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  public static final String SERVER_PORT_ATTRIBUTE_NAME = "appEngineDevServerPort"; //$NON-NLS-1$
  public static final String ADMIN_HOST_ATTRIBUTE_NAME = "appEngineDevServerAdminHost"; //$NON-NLS-1$
  public static final String ADMIN_PORT_ATTRIBUTE_NAME = "appEngineDevServerAdminPort"; //$NON-NLS-1$

  // These are the default values used by Cloud SDK's dev_appserver
  public static final int DEFAULT_SERVER_PORT = 8080;
  public static final String DEFAULT_ADMIN_HOST = "localhost"; //$NON-NLS-1$
  public static final int DEFAULT_ADMIN_PORT = 8000;
  public static final int DEFAULT_API_PORT = 0; // allocated at random

  private static final Logger logger =
      Logger.getLogger(LocalAppEngineServerBehaviour.class.getName());

  private LocalAppEngineStartListener localAppEngineStartListener;
  private LocalAppEngineExitListener localAppEngineExitListener;

  /** The {@link CloudSdk} instance currently in use; may be {@code null}. */
  private CloudSdk cloudSdk;
  private AppEngineDevServer devServer;
  private Process devProcess;

  @VisibleForTesting
  int serverPort = -1;
  @VisibleForTesting
  int adminPort = -1;

  private DevAppServerOutputListener serverOutputListener;
  
  @VisibleForTesting
  Map<String, String> moduleToUrlMap = new LinkedHashMap<>();

  public LocalAppEngineServerBehaviour () {
    localAppEngineStartListener = new LocalAppEngineStartListener();
    localAppEngineExitListener = new LocalAppEngineExitListener();
    serverOutputListener = new DevAppServerOutputListener();
  }

  @Override
  public void stop(boolean force) {
    int serverState = getServer().getServerState();
    if (serverState == IServer.STATE_STOPPED) {
      return;
    }
    // If the server seems to be running, and we haven't already tried to stop it,
    // then try to shut it down nicely
    if (devServer != null && (!force || serverState != IServer.STATE_STOPPING)) {
      setServerState(IServer.STATE_STOPPING);
      DefaultStopConfiguration stopConfig = new DefaultStopConfiguration();
      if (isDevAppServer1()) {
        stopConfig.setAdminPort(serverPort);        
      } else {
        stopConfig.setAdminPort(adminPort);
      }
      try {
        devServer.stop(stopConfig);
      } catch (AppEngineException ex) {
        logger.log(Level.WARNING, "Error terminating server: " + ex.getMessage(), ex); //$NON-NLS-1$
      }
    } else {
      // we've already given it a chance
      logger.info("forced stop: destroying associated processes"); //$NON-NLS-1$
      if (devProcess != null) {
        devProcess.destroy();
        devProcess = null;
      }
      devServer = null;
      setServerState(IServer.STATE_STOPPED);
    }
  }


  @Override
  public IStatus canStop() {
    int serverState = getServer().getServerState();
    if (serverState == IServer.STATE_STARTED) {
      return Status.OK_STATUS;
    }
    return StatusUtil.error(this, "Not started");
  }

  /**
   * Convenience method allowing access to protected method in superclass.
   */
  @Override
  protected IModuleResourceDelta[] getPublishedResourceDelta(IModule[] module) {
    return super.getPublishedResourceDelta(module);
  }

  /**
   * Convenience method allowing access to protected method in superclass.
   */
  @Override
  protected IModuleResource[] getResources(IModule[] module) {
    return super.getResources(module);
  }

  /**
   * Returns runtime base directory. Uses temp directory.
   */
  public IPath getRuntimeBaseDirectory() {
    return getTempDirectory(false);
  }

  /**
   * @return the directory at which module will be published.
   */
  public IPath getModuleDeployDirectory(IModule module) {
    return getRuntimeBaseDirectory().append(module.getName());
  }

  /**
   * Convenience accessor to protected member in superclass.
   */
  public final void setModulePublishState2(IModule[] module, int state) {
    setModulePublishState(module, state);
  }

  private static IStatus newErrorStatus(String message) {
    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
  }


  @Override
  public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy,
      IProgressMonitor monitor) throws CoreException {
    super.setupLaunchConfiguration(workingCopy, monitor);

    // it seems surprising that the Server class doesn't already do this
    Collection<IProject> projects = new ArrayList<>();
    for (IModule module : getServer().getModules()) {
      IProject project = module.getProject();
      if (project != null) {
        projects.add(project);
      }
    }
    workingCopy.setMappedResources(projects.toArray(new IResource[projects.size()]));
  }

  /**
   * Check whether the provided port is in use. Returns the port if not, or throws an exception if
   * the port is in use.
   * 
   * @param addr a machine address or {@code null} for all addresses
   * @param portInUse returns true if the (host,port) is in use
   * @return the port value ⊂ [0, 65535]
   * @throws CoreException if the port is in use
   */
  @VisibleForTesting
  static int checkPort(InetAddress addr, int port, PortChecker portInUse)
      throws CoreException {
    Preconditions.checkNotNull(portInUse);
    if (port < 0 || port > 65535) {
      throw new CoreException(newErrorStatus(Messages.getString("PORT_OUT_OF_RANGE")));
    }

    if (port != 0 && portInUse.isInUse(addr, port)) {
      throw new CoreException(
          newErrorStatus(Messages.getString("PORT_IN_USE", String.valueOf(port))));
    }
    return port;
  }

  /**
   * Returns service port of this server. Note that this method returns -1 if the user has never
   * attempted to launch the server.
   *
   * @return user-specified port, unless the user let the server choose it (by setting the port
   *     to 0 or an empty string in the UI initially). If the user let the server choose it,
   *     returns the port of the module with the name "default", if the module exists; otherwise,
   *     returns the port of an arbitrary module.
   */
  public int getServerPort() {
    return serverPort;
  }

  /**
   * Returns the admin port of this server. Note that this method returns -1 if the user has never
   * attempted to launch the server.
   */
  public int getAdminPort() {
    return adminPort;
  }

  /**
   * @return a short pithy description of this server suitable for use in UI elements
   */
  public String getDescription() {
    if (cloudSdk != null) {
      try {
        CloudSdkVersion version = cloudSdk.getVersion();
        return Messages.getString("cloudsdk.server.description.version", version); //$NON-NLS-1$
      } catch (AppEngineException ex) {
        logger.log(Level.WARNING, "Unable to obtain CloudSdk version", ex); //$NON-NLS-1$
      }
    }
    return Messages.getString("cloudsdk.server.description"); //$NON-NLS-1$
  }

  /**
   * Starts the development server.
   *
   * @param console the stream (Eclipse console) to send development server process output to
   * @param arguments JVM arguments to pass to the dev server
   */
  void startDevServer(DefaultRunConfiguration devServerRunConfiguration,
      MessageConsoleStream console)
      throws CoreException {
    
    PortChecker portInUse = new PortChecker() {
      @Override
      public boolean isInUse(InetAddress addr, int port) {
        Preconditions.checkNotNull(port);
        return SocketUtil.isPortInUse(addr, port);
      }
    };

    InetAddress serverHost = InetAddress.getLoopbackAddress();
    if (devServerRunConfiguration.getHost() != null) {
      serverHost = LocalAppEngineServerLaunchConfigurationDelegate
          .resolveAddress(devServerRunConfiguration.getHost());
    }
    serverPort = checkPort(serverHost,
        ifNull(devServerRunConfiguration.getPort(), DEFAULT_SERVER_PORT), portInUse);

    InetAddress adminHost = InetAddress.getLoopbackAddress();
    if (devServerRunConfiguration.getAdminHost() != null) {
      adminHost = LocalAppEngineServerLaunchConfigurationDelegate
          .resolveAddress(devServerRunConfiguration.getAdminHost());
    }
    adminPort = checkPort(adminHost,
        ifNull(devServerRunConfiguration.getAdminPort(), DEFAULT_ADMIN_PORT), portInUse);

    // API port seems on localhost in practice
    checkPort(InetAddress.getLoopbackAddress(),
        ifNull(devServerRunConfiguration.getApiPort(), DEFAULT_API_PORT), portInUse);

    setServerState(IServer.STATE_STARTING);

    // Create dev app server instance
    initializeDevServer(console);

    // Run server
    try {
      devServer.run(devServerRunConfiguration);
    } catch (AppEngineException ex) {
      Activator.logError("Error starting server: " + ex.getMessage()); //$NON-NLS-1$
      stop(true);
    }
  }

  private static int ifNull(Integer value, int defaultValue) {
    return value != null ? value : defaultValue;
  }

  private void initializeDevServer(MessageConsoleStream console) {
    MessageConsoleWriterOutputLineListener outputListener =
        new MessageConsoleWriterOutputLineListener(console);

    // dev_appserver output goes to stderr
    cloudSdk = new CloudSdk.Builder()
        .addStdOutLineListener(outputListener)
        .addStdErrLineListener(outputListener)
        .addStdErrLineListener(serverOutputListener)
        .startListener(localAppEngineStartListener)
        .exitListener(localAppEngineExitListener)
        .async(true)
        .build();

    devServer = new CloudSdkAppEngineDevServer1(cloudSdk);
    moduleToUrlMap.clear();
  }
  
  /**
   * @return true if and only if we're using devappserver1
   */
  private boolean isDevAppServer1() {
    return devServer instanceof CloudSdkAppEngineDevServer1;
  }

  /**
   * A {@link ProcessExitListener} for the App Engine server.
   */
  private class LocalAppEngineExitListener implements ProcessExitListener {
    @Override
    public void onExit(int exitCode) {
      logger.log(Level.FINE, "Process exit: code=" + exitCode); //$NON-NLS-1$
      devServer = null;
      devProcess = null;
      setServerState(IServer.STATE_STOPPED);
    }
  }

  /**
   * A {@link ProcessStartListener} for the App Engine server.
   */
  private class LocalAppEngineStartListener implements ProcessStartListener {
    @Override
    public void onStart(Process process) {
      logger.log(Level.FINE, "New Process: " + process); //$NON-NLS-1$
      devProcess = process;
    }
  }

  /**
   * An output listener that monitors for well-known key dev_appserver output and effects server
   * state changes.
   */
  public class DevAppServerOutputListener implements ProcessOutputLineListener {
    // DevAppServer2 outputs the following for module-started and admin line (on one line):
    // <<HEADER>> Starting module "default" running at: http://localhost:8080
    // <<HEADER>> Starting admin server at: http://localhost:8000
    // where <<HEADER>> = INFO 2017-01-31 21:00:40,700 dispatcher.py:197]
    
    // devappserver2 patterns
    private final Pattern moduleStartedPattern = Pattern.compile(
        "INFO .*Starting module \"(?<service>[^\"]+)\" running at: (?<url>http://.+:(?<port>[0-9]+))$");
    private final Pattern adminStartedPattern =
        Pattern.compile("INFO .*Starting admin server at: (?<url>http://.+:(?<port>[0-9]+))$");

    // devappserver1 patterns
    private final Pattern moduleRunningPattern = Pattern.compile(
        "INFO: Module instance (?<service>[\\w\\d\\-]+) is running at (?<url>http://.+:(?<port>[0-9]+)/)$");
    private final Pattern adminRunningPattern =
        Pattern.compile("INFO: The admin console is running at (?<url>http://.+:(?<port>[0-9]+))/_ah/admin$");
      
    private int serverPortCandidate = 0;

    @Override
    public void onOutputLine(String line) {
      Matcher matcher;
      if (line.endsWith("Dev App Server is now running")) { //$NON-NLS-1$
        // App Engine Standard (v1)
        setServerState(IServer.STATE_STARTED);
      } else if (line.endsWith(".Server:main: Started")) { //$NON-NLS-1$
        // App Engine Flexible (v2)
        setServerState(IServer.STATE_STARTED);
      } else if (line.equals("Traceback (most recent call last):")) { //$NON-NLS-1$
        // An error occurred
        setServerState(IServer.STATE_STOPPED);
      } else if (line.contains("Error: A fatal exception has occurred. Program will exit")) { //$NON-NLS-1$
        // terminate the Python process
        stop(false);
      } else if ((matcher = moduleStartedPattern.matcher(line)).matches()
          || (matcher = moduleRunningPattern.matcher(line)).matches()) {
        String serviceId = matcher.group("service");
        String url = matcher.group("url");
        moduleToUrlMap.put(serviceId, url);
        String portString = matcher.group("port");
        int port = parseInt(portString, 0);
        if (port > 0 && (serverPortCandidate == 0 || "default".equals(serviceId))) { // $NON-NLS-1$
          serverPortCandidate = port;
        }
      } else if ((matcher = adminStartedPattern.matcher(line)).matches()
          || (matcher = adminRunningPattern.matcher(line)).matches()) {
        int port = parseInt(matcher.group("port"), 0);
        if (port > 0 && adminPort <= 0) {
          adminPort = port;
        }
        // Admin comes after other modules, so no more module URLs
        if (serverPort <= 0) {
          serverPort = serverPortCandidate;
        }
      }
    }
  }

  @Override
  public IPath getPublishDirectory(IModule[] module) {
    if (module == null || module.length == 0) {
      return null;
    }
    return getModuleDeployDirectory(module[0]);
  }

  /** Return the URL for the given service, or {@code null} if unknown. */
  public String getServiceUrl(String serviceId) {
    Preconditions.checkNotNull(serviceId);
    return moduleToUrlMap.get(serviceId);
  }
}
