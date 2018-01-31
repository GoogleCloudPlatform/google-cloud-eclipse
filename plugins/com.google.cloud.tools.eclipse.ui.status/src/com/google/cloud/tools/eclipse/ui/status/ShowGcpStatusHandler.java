package com.google.cloud.tools.eclipse.ui.status;

import com.google.cloud.tools.eclipse.ui.util.ServiceUtils;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.common.base.Joiner;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

public class ShowGcpStatusHandler extends AbstractHandler implements IElementUpdater {
  private static final Logger logger = Logger.getLogger(ShowGcpStatusHandler.class.getName());

  private static final String STATUS_URL = "https://status.cloud.google.com";

  private static final String BUNDLE_ID = "com.google.cloud.tools.eclipse.ui.status";

  private static ImageDescriptor IMG_OK;
  private static ImageDescriptor IMG_INFO;
  private static ImageDescriptor IMG_WARNING;
  private static ImageDescriptor IMG_ERROR;

  static {
    try {
      String prefix = "platform:/plugin/" + BUNDLE_ID + "/icons/";
      IMG_OK = ImageDescriptor.createFromURL(new URL(prefix + "gcp-ok.png"));
      IMG_INFO = ImageDescriptor.createFromURL(new URL(prefix + "gcp-info.png"));
      IMG_WARNING = ImageDescriptor.createFromURL(new URL(prefix + "gcp-warning.png"));
      IMG_ERROR = ImageDescriptor.createFromURL(new URL(prefix + "gcp-error.png"));
    } catch (MalformedURLException ex) {
      logger.log(Level.SEVERE, "Invalid image locations", ex);
    }
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    GcpStatusService service = ServiceUtils.getService(event, GcpStatusService.class);
    ((PollingStatusServiceImpl) service).refreshStatus();
    WorkbenchUtil.openInBrowser(PlatformUI.getWorkbench(), STATUS_URL);
    return null;
  }

  @Override
  public void updateElement(UIElement element, Map parameters) {
    GcpStatusService service = element.getServiceLocator().getService(GcpStatusService.class);
    IStatus status = service.getCurrentStatus();
    element.setText("Status: " + summarizeServices(status));
    element.setTooltip(summarizeIncidents(status));
    switch (status.getSeverity()) {
      case IStatus.OK:
        element.setIcon(IMG_OK);
        break;
      case IStatus.INFO:
        element.setIcon(IMG_INFO);
        break;
      case IStatus.WARNING:
        element.setIcon(IMG_WARNING);
        break;
      case IStatus.ERROR:
        element.setIcon(IMG_ERROR);
        break;
    }
  }

  private static String summarizeServices(IStatus status) {
    if (status.isOK()) {
      return "All services available";
    }
    if (!status.isMultiStatus()) {
      return status.getPlugin();
    }
    Set<String> services = new HashSet<>();
    for (IStatus child : status.getChildren()) {
      services.add(child.getPlugin());
    }
    return Joiner.on(",").join(services);
  }

  private String summarizeIncidents(IStatus status) {
    if (status.isOK()) {
      return "All services available";
    }
    if (!status.isMultiStatus()) {
      return status.getMessage();
    }
    List<String> messages = new ArrayList<>();
    for (IStatus child : status.getChildren()) {
      messages.add(child.getMessage());
    }
    return Joiner.on("\n").join(messages);
  }
}
