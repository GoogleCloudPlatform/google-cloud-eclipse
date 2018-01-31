package com.google.cloud.tools.eclipse.ui.status;

import com.google.cloud.tools.eclipse.util.jobs.Consumer;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

public class WorkbenchStartup implements IStartup {

  @Override
  public void earlyStartup() {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    GcpStatusService service = workbench.getService(GcpStatusService.class);
    if (service != null) {
      service.addStatusChangeListener(
          new Consumer<GcpStatusService>() {
            @Override
            public void accept(final GcpStatusService result) {
              ICommandService commandService = workbench.getService(ICommandService.class);
              if (commandService != null) {
                commandService.refreshElements(
                    "com.google.cloud.tools.eclipse.ui.status.showGcpStatus", null);
              }
            }
          });
    }
  }
}
