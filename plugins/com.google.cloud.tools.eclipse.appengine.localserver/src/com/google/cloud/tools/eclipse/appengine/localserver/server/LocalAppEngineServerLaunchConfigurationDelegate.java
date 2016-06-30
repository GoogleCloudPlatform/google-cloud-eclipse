package com.google.cloud.tools.eclipse.appengine.localserver.server;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import com.google.cloud.tools.eclipse.appengine.localserver.Activator;

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

    MessageConsole console = TargetPlatform.findConsole(configuration.getName());
    console.clearConsole();
    TargetPlatform.showConsole(console);

    LocalAppEngineServerDelegate serverDelegate = LocalAppEngineServerDelegate.getAppEngineServer(server);
    int debugPort = -1;
    if (ILaunchManager.DEBUG_MODE.equals(mode)) {
      debugPort = getDebugPort();

      try {
        runDebugTarget(serverDelegate, modules[0].getProject().getName(), debugPort);
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

  private void runDebugTarget(LocalAppEngineServerDelegate serverDelegate, String projectName, int port)
      throws CoreException {
    ILaunchConfigurationWorkingCopy remoteDebugLaunchConfig = createRemoteDebugLaunchConfiguration(serverDelegate,
        projectName,
        Integer.toString(port));
    remoteDebugLaunchConfig.launch(ILaunchManager.DEBUG_MODE, null);
  }

  private ILaunchConfigurationWorkingCopy createRemoteDebugLaunchConfiguration(LocalAppEngineServerDelegate serverDelegate,
      String projectName, String port) throws CoreException {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION);

    ILaunchConfigurationWorkingCopy remoteDebugConfig = type.newInstance(null,
        "Remote debugger for "
            + projectName
            + " ("
            + port
            + ")");

    // Set project
    remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
        projectName);

    // Set JVM debugger connection parameters
    Map<String, String> connectionParameters = new HashMap<>();
    connectionParameters.put("hostname", DEBUGGER_HOST);
    connectionParameters.put("port", port);
    remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP,
        connectionParameters);
    remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
        "org.eclipse.jdt.launching.socketListenConnector");

    return remoteDebugConfig;
  }

  private int getDebugPort() throws CoreException {
    int port = SocketUtil.findFreePort();
    if (port == -1) {
      abort("Cannot find free port for remote debugger", null, IStatus.ERROR);
    }
    return port;
  }
}
