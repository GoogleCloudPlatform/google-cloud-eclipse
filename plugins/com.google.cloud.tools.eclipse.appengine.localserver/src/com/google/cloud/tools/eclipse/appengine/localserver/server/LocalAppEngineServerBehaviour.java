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
import com.google.cloud.tools.eclipse.sdk.ui.MessageConsoleWriterOutputLineListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

/**
 * A {@link ServerBehaviourDelegate} for App Engine Server executed via the Java App Management
 * Client Library.
 */
public class LocalAppEngineServerBehaviour extends ServerBehaviourDelegate {

  public static final String SERVER_PORT_ATTRIBUTE_NAME = "appEngineDevServerPort";
  public static final int DEFAULT_SERVER_PORT = 8080;

  private static final Logger logger =
      Logger.getLogger(LocalAppEngineServerBehaviour.class.getName());

  private LocalAppEngineStartListener localAppEngineStartListener;
  private LocalAppEngineExitListener localAppEngineExitListener;
  private AppEngineDevServer devServer;
  private Process devProcess;
  private int port = -1;

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
      // TODO: when available configure the host and port specified in the server
      DefaultStopConfiguration stopConfig = new DefaultStopConfiguration();
      try {
        devServer.stop(stopConfig);
      } catch (AppEngineException ex) {
        logger.log(Level.WARNING, "Error terminating server: " + ex.getMessage(), ex);
      }
    } else {
      // we've already given it a chance
      logger.info("forced stop: destroying associated processes");
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

  @Override
  public IStatus canStop() {
    int serverState = getServer().getServerState();
    if ((serverState != IServer.STATE_STOPPING) && (serverState != IServer.STATE_STOPPED)) {
      return Status.OK_STATUS;
    } else {
      return newErrorStatus("Stop in progress");
    }
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

  private IStatus newErrorStatus(String message) {
    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
  }

  private int checkAndSetPort() throws CoreException {
    port = getServer().getAttribute(SERVER_PORT_ATTRIBUTE_NAME, DEFAULT_SERVER_PORT);
    if (port < 0 || port > 65535) {
      throw new CoreException(newErrorStatus("Port must be between 0 and 65535."));
    }

    if (port == 0) {
      port = SocketUtil.findFreePort();
      if (port == -1) {
        throw new CoreException(newErrorStatus("Failed to find a free port."));
      }
    }
    return port;
  }

  public int getPort() {
    return port;
  }

  /**
   * Starts the development server.
   *
   * @param runnables the path to directories that contain configuration files like appengine-web.xml
   * @param console the stream (Eclipse console) to send development server process output to
   */
  void startDevServer(List<File> runnables, MessageConsoleStream console) throws CoreException {
    checkAndSetPort();  // Must be called before setting the STARTING state.
    setServerState(IServer.STATE_STARTING);

    // Create dev app server instance
    initializeDevServer(console);

    // Create run configuration
    DefaultRunConfiguration devServerRunConfiguration = new DefaultRunConfiguration();
    devServerRunConfiguration.setAutomaticRestart(false);
    devServerRunConfiguration.setAppYamls(runnables);
    devServerRunConfiguration.setHost(getServer().getHost());
    devServerRunConfiguration.setPort(port);

    // FIXME: workaround bug when running on a Java8 JVM
    // https://github.com/GoogleCloudPlatform/gcloud-eclipse-tools/issues/181
    devServerRunConfiguration.setJvmFlags(Arrays.asList("-Dappengine.user.timezone=UTC"));

    // Run server
    try {
      devServer.run(devServerRunConfiguration);
    } catch (AppEngineException ex) {
      Activator.logError("Error starting server: " + ex.getMessage());
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
    checkAndSetPort();  // Must be called before setting the STARTING state.
    setServerState(IServer.STATE_STARTING);

    // Create dev app server instance
    initializeDevServer(console);

    // Create run configuration
    DefaultRunConfiguration devServerRunConfiguration = new DefaultRunConfiguration();
    devServerRunConfiguration.setAutomaticRestart(false);
    devServerRunConfiguration.setAppYamls(runnables);
    devServerRunConfiguration.setHost(getServer().getHost());
    devServerRunConfiguration.setPort(port);

    // todo: make this a configurable option, but default to
    // 1 instance to simplify debugging
    devServerRunConfiguration.setMaxModuleInstances(1);

    List<String> jvmFlags = new ArrayList<String>();
    // FIXME: workaround bug when running on a Java8 JVM
    // https://github.com/GoogleCloudPlatform/gcloud-eclipse-tools/issues/181
    jvmFlags.add("-Dappengine.user.timezone=UTC");

    if (debugPort <= 0 || debugPort > 65535) {
      throw new IllegalArgumentException("Debug port is set to " + debugPort
                                      + ", should be between 1-65535");
    }
    jvmFlags.add("-Xdebug");
    jvmFlags.add("-Xrunjdwp:transport=dt_socket,server=n,suspend=y,quiet=y,address=" + debugPort);
    devServerRunConfiguration.setJvmFlags(jvmFlags);

    // Run server
    try {
      devServer.run(devServerRunConfiguration);
    } catch (AppEngineException ex) {
      Activator.logError("Error starting server: " + ex.getMessage());
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
      logger.log(Level.FINE, "Process exit: code=" + exitCode);
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
      logger.log(Level.FINE, "New Process: " + process);
      devProcess = process;
    }
  }

  /**
   * An output listener that monitors for well-known key dev_appserver output and affects server
   * state changes.
   */
  public class DevAppServerOutputListener implements ProcessOutputLineListener {

    @Override
    public void onOutputLine(String line) {
      if (line.endsWith("Dev App Server is now running")) {
        // App Engine Standard (v1)
        setServerState(IServer.STATE_STARTED);
      } else if (line.endsWith(".Server:main: Started")) {
        // App Engine Flexible (v2)
        setServerState(IServer.STATE_STARTED);
      } else if (line.equals("Traceback (most recent call last):")) {
        // An error occurred
        setServerState(IServer.STATE_STOPPED);
      }
    }
  }
}
