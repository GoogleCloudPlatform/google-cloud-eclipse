package com.google.cloud.tools.eclipse.appengine.localserver.server;

import com.google.cloud.tools.eclipse.appengine.localserver.Activator;
import com.google.cloud.tools.eclipse.appengine.localserver.ui.LocalAppEngineConsole;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalAppEngineServerLaunchConfigurationDelegate
    extends AbstractJavaLaunchConfigurationDelegate {
  private static final String DEBUGGER_HOST = "localhost";

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    IServer server = ServerUtil.getServer(configuration);
    if (server == null) {
      return;
    }
    IModule[] modules = server.getModules();
    if (modules == null || modules.length == 0) {
      return;
    }

    // App Engine dev server can only debug one module at a time
    if (mode.equals(ILaunchManager.DEBUG_MODE) && modules.length > 1) {
      String message = "The App Engine development server supports only 1 module when running in debug mode";
      Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
      throw new CoreException(status);
    }

    LocalAppEngineServerBehaviour serverBehaviour =
        (LocalAppEngineServerBehaviour) server.loadAdapter(LocalAppEngineServerBehaviour.class, null);

    List<File> runnables = new ArrayList<File>();
    for (IModule module : modules) {
      IPath deployPath = serverBehaviour.getModuleDeployDirectory(module);
      runnables.add(deployPath.toFile());
    }

    LocalAppEngineConsole console = ConsoleUtilities.findConsole(configuration.getName(), serverBehaviour);
    console.clearConsole();
    console.activate();

    setDefaultSourceLocator(launch, configuration);

    LocalAppEngineServerDelegate serverDelegate = LocalAppEngineServerDelegate.getAppEngineServer(server);
    int debugPort = -1;
    if (ILaunchManager.DEBUG_MODE.equals(mode)) {
      debugPort = getDebugPort();

      try {
        setupDebugTarget(launch, configuration, serverDelegate, modules[0].getProject().getName(),
            debugPort, monitor);
      } catch (CoreException e) {
        Activator.logError(e);
      }
    }

    // Start server
    if (mode.equals(ILaunchManager.DEBUG_MODE)) {
      serverBehaviour.startDebugDevServer(runnables, console.newMessageStream(), debugPort);
    } else {
      serverBehaviour.startDevServer(runnables, console.newMessageStream());
    }
  }

  private void setupDebugTarget(ILaunch launch, ILaunchConfiguration configuration, LocalAppEngineServerDelegate serverDelegate, String projectName, int port, IProgressMonitor monitor)
      throws CoreException {

    // Set JVM debugger connection parameters
    int timeout = JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT);
    Map<String, String> connectionParameters = new HashMap<>();
    connectionParameters.put("hostname", DEBUGGER_HOST);
    connectionParameters.put("port", Integer.toString(port));
    connectionParameters.put("timeout", Integer.toString(timeout));
    
    IVMConnector connector = JavaRuntime.getVMConnector("org.eclipse.jdt.launching.socketListenConnector");
    if(connector == null) {
      abort("Cannot find Socket Listening connector", null, 0);
    } else {
      connector.connect(connectionParameters, monitor, launch);
    }
  }


  private int getDebugPort() throws CoreException {
    int port = SocketUtil.findFreePort();
    if (port == -1) {
      abort("Cannot find free port for remote debugger", null, IStatus.ERROR);
    }
    return port;
  }
}
