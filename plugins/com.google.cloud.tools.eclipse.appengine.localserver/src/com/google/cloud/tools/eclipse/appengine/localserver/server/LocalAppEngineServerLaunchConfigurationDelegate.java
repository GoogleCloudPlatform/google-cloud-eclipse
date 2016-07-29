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
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
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
    // as we cannot configure the IVMConnector to continue listening
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

    if (ILaunchManager.DEBUG_MODE.equals(mode)) {
      int debugPort = getDebugPort();
      setupDebugTarget(launch, configuration, debugPort, monitor);
      serverBehaviour.startDebugDevServer(runnables, console.newMessageStream(), debugPort);
    } else {
      serverBehaviour.startDevServer(runnables, console.newMessageStream());
    }
  }

  private void setupDebugTarget(ILaunch launch, ILaunchConfiguration configuration, int port,
      IProgressMonitor monitor) throws CoreException {
    IVMConnector connector =
        JavaRuntime.getVMConnector(IJavaLaunchConfigurationConstants.ID_SOCKET_LISTEN_VM_CONNECTOR);
    if (connector == null) {
      abort("Cannot find Socket Listening connector", null, 0);
      return; // keep JDT null analysis happy
    }

    // Set JVM debugger connection parameters
    int timeout = JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT);
    Map<String, String> connectionParameters = new HashMap<>();
    connectionParameters.put("hostname", DEBUGGER_HOST);
    connectionParameters.put("port", Integer.toString(port));
    connectionParameters.put("timeout", Integer.toString(timeout));
    connector.connect(connectionParameters, monitor, launch);
  }


  private int getDebugPort() throws CoreException {
    int port = SocketUtil.findFreePort();
    if (port == -1) {
      abort("Cannot find free port for remote debugger", null, IStatus.ERROR);
    }
    return port;
  }
}
