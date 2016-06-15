package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

public class LocalAppEngineServerLaunchConfigurationDelegate
    extends AbstractJavaLaunchConfigurationDelegate {

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    
    final IServer server = ServerUtil.getServer(configuration);
    if (server == null) {
      return;
    }

    LocalAppEngineServerBehaviour serverBehaviour =
        (LocalAppEngineServerBehaviour) server.loadAdapter(LocalAppEngineServerBehaviour.class, null);

    serverBehaviour.setStarting();
    
    // TODO actually launch the server
    
    serverBehaviour.setStarted();
    
    MessageConsole console = TargetPlatform.findConsole("debugging");
    TargetPlatform.showConsole(console);
    MessageConsoleStream out = console.newMessageStream();
    out.println("Server started (not really)");
  }

}
