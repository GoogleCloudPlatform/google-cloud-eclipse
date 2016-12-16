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
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineDevServer;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.eclipse.appengine.localserver.Activator;
import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.sdk.ui.MessageConsoleWriterOutputLineListener;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * A {@link ServerBehaviourDelegate} for App Engine Server executed via the Java App Management
 * Client Library.
 */
public class LocalAppEngineServerBehaviour extends ServerBehaviourDelegate
    implements IModulePublishHelper {

  public static final String SERVER_PORT_ATTRIBUTE_NAME = "appEngineDevServerPort"; //$NON-NLS-1$
  public static final String ADMIN_PORT_ATTRIBUTE_NAME = "appEngineDevServerAdminPort"; //$NON-NLS-1$

  public static final int DEFAULT_SERVER_PORT = 8080;
  public static final int DEFAULT_ADMIN_PORT = 8000;

  private static final Logger logger =
      Logger.getLogger(LocalAppEngineServerBehaviour.class.getName());

  private LocalAppEngineStartListener localAppEngineStartListener;
  private LocalAppEngineExitListener localAppEngineExitListener;
  private AppEngineDevServer devServer;
  private Process devProcess;

  private int serverPort = -1;
  @VisibleForTesting int adminPort = -1;

  private DevAppServerOutputListener serverOutputListener;

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
      stopConfig.setAdminPort(adminPort);
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

  private void checkAndSetPorts() throws CoreException {
    checkAndSetPorts(getServer(), new PortProber() {
      @Override
      public boolean isPortInUse(int port) {
        return SocketUtil.isPortInUse(port);
      }
    });
  }

  @VisibleForTesting
  public interface PortProber {
    boolean isPortInUse(int port);
  }

  @VisibleForTesting
  void checkAndSetPorts(IServer server, PortProber portProber) throws CoreException {
    serverPort = checkPortAttribute(server, portProber,
        SERVER_PORT_ATTRIBUTE_NAME, DEFAULT_SERVER_PORT);
    adminPort = checkPortAttribute(server, portProber,
        ADMIN_PORT_ATTRIBUTE_NAME, DEFAULT_ADMIN_PORT);
  }

  private static int checkPortAttribute(IServer server, PortProber portProber,
      String attribute, int defaultPort) throws CoreException {
    int port = server.getAttribute(attribute, defaultPort);
    if (port < 0 || port > 65535) {
      throw new CoreException(newErrorStatus(Messages.PORT_OUT_OF_RANGE));
    }

    if (port != 0 && portProber.isPortInUse(port)) {
      boolean failover = !hasAttribute(server, attribute);
      if (failover) {
        logger.log(Level.INFO, attribute + ": port " + port + " in use. Picking an unused port.");
        port = 0;
      } else {
        throw new CoreException(newErrorStatus(
            MessageFormat.format(Messages.PORT_IN_USE, String.valueOf(port))));
      }
    }
    return port;
  }

  private static boolean hasAttribute(IServer server, String attribute) {
    return server.getAttribute(attribute, (String) null) != null;
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
   * Starts the development server.
   *
   * @param runnables the path to directories that contain configuration files like appengine-web.xml
   * @param console the stream (Eclipse console) to send development server process output to
   */
  void startDevServer(List<File> runnables, MessageConsoleStream console) throws CoreException {
    checkAndSetPorts();  // Must be called before setting the STARTING state.
    setServerState(IServer.STATE_STARTING);

    // Create dev app server instance
    initializeDevServer(console);

    // Create run configuration
    DefaultRunConfiguration devServerRunConfiguration = new DefaultRunConfiguration();
    devServerRunConfiguration.setAutomaticRestart(false);
    devServerRunConfiguration.setAppYamls(runnables);
    devServerRunConfiguration.setHost(getServer().getHost());
    devServerRunConfiguration.setPort(serverPort);
    devServerRunConfiguration.setAdminPort(adminPort);

    // Run server
    try {
      devServer.run(devServerRunConfiguration);
    } catch (AppEngineException ex) {
      Activator.logError("Error starting server: " + ex.getMessage()); //$NON-NLS-1$
      stop(true);
    }
  }

  /**
   * Starts the development server in debug mode.
   *
   * @param runnables the path to directories that contain configuration files like appengine-web.xml
   * @param console the stream (Eclipse console) to send development server process output to
   * @param debugPort the port to attach a debugger to if launch is in debug mode
   */
  void startDebugDevServer(List<File> runnables, MessageConsoleStream console, int debugPort)
      throws CoreException {
    checkAndSetPorts();  // Must be called before setting the STARTING state.
    setServerState(IServer.STATE_STARTING);

    // Create dev app server instance
    initializeDevServer(console);

    // Create run configuration
    DefaultRunConfiguration devServerRunConfiguration = new DefaultRunConfiguration();
    devServerRunConfiguration.setAutomaticRestart(false);
    devServerRunConfiguration.setAppYamls(runnables);
    devServerRunConfiguration.setHost(getServer().getHost());
    devServerRunConfiguration.setPort(serverPort);
    devServerRunConfiguration.setAdminPort(adminPort);

    // todo: make this a configurable option, but default to
    // 1 instance to simplify debugging
    devServerRunConfiguration.setMaxModuleInstances(1);

    List<String> jvmFlags = new ArrayList<String>();

    if (debugPort <= 0 || debugPort > 65535) {
      throw new IllegalArgumentException("Debug port is set to " + debugPort //$NON-NLS-1$
                                      + ", should be between 1-65535"); //$NON-NLS-1$
    }
    jvmFlags.add("-Xdebug"); //$NON-NLS-1$
    jvmFlags.add("-Xrunjdwp:transport=dt_socket,server=n,suspend=y,quiet=y,address=" + debugPort); //$NON-NLS-1$
    devServerRunConfiguration.setJvmFlags(jvmFlags);

    // Run server
    try {
      devServer.run(devServerRunConfiguration);
    } catch (AppEngineException ex) {
      Activator.logError("Error starting server: " + ex.getMessage()); //$NON-NLS-1$
      stop(true);
    }
  }

  private void initializeDevServer(MessageConsoleStream console) {
    MessageConsoleWriterOutputLineListener outputListener =
        new MessageConsoleWriterOutputLineListener(console);

    // dev_appserver output goes to stderr
    CloudSdk cloudSdk = new CloudSdk.Builder()
        .addStdOutLineListener(outputListener)
        .addStdErrLineListener(outputListener)
        .addStdErrLineListener(serverOutputListener)
        .startListener(localAppEngineStartListener)
        .exitListener(localAppEngineExitListener)
        .async(true)
        .build();

    devServer = new CloudSdkAppEngineDevServer(cloudSdk);
  }

  /**
   * A {@link ProcessExitListener} for the App Engine server.
   */
  public class LocalAppEngineExitListener implements ProcessExitListener {
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
  public class LocalAppEngineStartListener implements ProcessStartListener {
    @Override
    public void onStart(Process process) {
      logger.log(Level.FINE, "New Process: " + process); //$NON-NLS-1$
      devProcess = process;
    }
  }

  @VisibleForTesting
  static int extractPortFromServerUrlOutput(String line) {
    try {
      int urlBegin = line.lastIndexOf("http://"); //$NON-NLS-1$
      if (urlBegin != -1) {
        return new URI(line.substring(urlBegin)).getPort();
      }
    } catch (URISyntaxException ex) {}

    logger.log(Level.WARNING, "Cannot extract port from server output: " + line); //$NON-NLS-1$
    return -1;
  }

  /**
   * An output listener that monitors for well-known key dev_appserver output and affects server
   * state changes.
   */
  public class DevAppServerOutputListener implements ProcessOutputLineListener {

    private int serverPortCandidate = 0;

    @Override
    public void onOutputLine(String line) {
      if (line.endsWith("Dev App Server is now running")) { //$NON-NLS-1$
        // App Engine Standard (v1)
        setServerState(IServer.STATE_STARTED);
      } else if (line.endsWith(".Server:main: Started")) { //$NON-NLS-1$
        // App Engine Flexible (v2)
        setServerState(IServer.STATE_STARTED);
      } else if (line.equals("Traceback (most recent call last):")) { //$NON-NLS-1$
        // An error occurred
        setServerState(IServer.STATE_STOPPED);

      } else if (line.contains("Starting module") && line.contains("running at: http://")) { //$NON-NLS-1$ //$NON-NLS-2$
        if (serverPortCandidate == 0 || line.contains("Starting module \"default\"")) { //$NON-NLS-1$
          serverPortCandidate = extractPortFromServerUrlOutput(line);
        }

      } else if (line.contains("Starting admin server at: http://")) { //$NON-NLS-1$
        if (serverPort == 0) {  // We assume we will no longer see URLs for modules from now on.
          serverPort = serverPortCandidate;
        }
        if (adminPort == 0) {
          adminPort = extractPortFromServerUrlOutput(line);
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
}
