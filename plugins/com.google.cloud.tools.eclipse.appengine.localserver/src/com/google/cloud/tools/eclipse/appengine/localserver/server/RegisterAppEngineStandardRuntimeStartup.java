
package com.google.cloud.tools.eclipse.appengine.localserver.server;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.IStartup;

/**
 * Registers the default instance of our App Engine Standard runtime if not found. Uses the internal
 * WTP {@link IStartup} to minimize activation to use of WTP-related functionality.
 */
public class RegisterAppEngineStandardRuntimeStartup implements IStartup {
  private static final Logger logger =
      Logger.getLogger(RegisterAppEngineStandardRuntimeStartup.class.getName());

  /**
   * AppEngineStandardFacet#createAppEngineServerRuntime() creates the default runtime using a
   * {@code null} ID, which causes it to take the name of its runtime-type.
   */
  private static final String RUNTIME_ID = "App Engine Standard Runtime";

  @Override
  public void startup() {
    IRuntime runtime = ServerCore.findRuntime(RUNTIME_ID);
    if (runtime != null) {
      logger.info("Runtime found: " + RUNTIME_ID);
      return;
    }
    IRuntimeType runtimeType = ServerCore.findRuntimeType(AppToolsRuntime.RUNTIME_TYPE_ID);
    if (runtimeType == null) {
      logger.log(Level.SEVERE, "Cannot find server runtime type: ",
          AppToolsRuntime.RUNTIME_TYPE_ID);
      return;
    }
    try {
      IRuntimeWorkingCopy inprogress =
          runtimeType.createRuntime(RUNTIME_ID, new NullProgressMonitor());
      inprogress.save(true, new NullProgressMonitor());
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, "Unable to create default runtime instance", ex);
    }
  }
}
