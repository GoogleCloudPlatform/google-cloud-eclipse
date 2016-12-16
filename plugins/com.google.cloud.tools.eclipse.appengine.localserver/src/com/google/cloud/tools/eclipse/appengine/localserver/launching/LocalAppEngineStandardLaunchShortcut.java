
package com.google.cloud.tools.eclipse.appengine.localserver.launching;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Provides support for run/debug on App Engine. Some of these methods are called frequently, such
 * as to update tooltips.
 */
public class LocalAppEngineStandardLaunchShortcut implements ILaunchShortcut2 {

  private LaunchHelper launcher = new LaunchHelper();

  @Override
  public void launch(ISelection selection, String launchMode) {
    try {
      IModule[] modules = launcher.asModules(selection);
      launcher.launch(modules, launchMode);
    } catch (CoreException ex) {
      ErrorDialog.openError(null, "Unable to launch App Engine server", ex.getLocalizedMessage(),
          ex.getStatus());
    }
  }

  @Override
  public void launch(IEditorPart editor, String launchMode) {
    try {
      IModule[] modules = launcher.asModules(editor);
      launcher.launch(modules, launchMode);
    } catch (CoreException ex) {
      ErrorDialog.openError(null, "Unable to launch App Engine server", ex.getLocalizedMessage(),
          ex.getStatus());
    }
  }

  @Override
  public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
    IModule[] modules = null;
    try {
      modules = launcher.asModules(selection);
    } catch (CoreException ex) {
      // return null as we do not support these types of objects
      return null;
    }
    return getLaunchConfigurations(modules);
  }

  @Override
  public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editor) {
    IModule[] modules = null;
    try {
      modules = launcher.asModules(editor);
    } catch (CoreException ex) {
      // return null as we do not support this type of project container
      return null;
    }
    return getLaunchConfigurations(modules);
  }

  /**
   * Find any applicable launch configurations.
   */
  private ILaunchConfiguration[] getLaunchConfigurations(IModule[] modules) {
    // First check if there's a server with exactly these modules
    Collection<IServer> servers = launcher.findExistingServers(modules, /* exact */ true, null);
    if (servers.isEmpty()) {
      // otherwise check if there's a server with at least these modules
      servers = launcher.findExistingServers(modules, /* exact */ false, null);
    }
    Collection<ILaunchConfiguration> launchConfigs = new ArrayList<>();
    for (IServer server : servers) {
      // Could filter out running servers, but then more servers are created
      try {
        ILaunchConfiguration launchConfig = server.getLaunchConfiguration(false, null);
        if (launchConfig != null) {
          launchConfigs.add(launchConfig);
        }
      } catch (CoreException ex) {
        /* ignore */
      }
    }
    return launchConfigs.toArray(new ILaunchConfiguration[launchConfigs.size()]);
  }

  @Override
  public IResource getLaunchableResource(ISelection selection) {
    return null;
  }

  @Override
  public IResource getLaunchableResource(IEditorPart editor) {
    return null;
  }

}
