package com.google.cloud.tools.eclipse.ui.status;

import com.google.cloud.tools.eclipse.ui.util.ServiceUtils;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.common.base.Joiner;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

public class ShowGcpStatusHandler extends AbstractHandler implements IElementUpdater {
  private static final Logger logger = Logger.getLogger(ShowGcpStatusHandler.class.getName());

  private static final String STATUS_URL = "https://status.cloud.google.com";

  private static final String BUNDLE_ID = "com.google.cloud.tools.eclipse.ui.status";

  private static ImageDescriptor IMG_OK;
  private static ImageDescriptor IMG_LOW;
  private static ImageDescriptor IMG_MEDIUM;
  private static ImageDescriptor IMG_HIGH;
  private static ImageDescriptor IMG_ERROR;

  static {
    try {
      String prefix = "platform:/plugin/" + BUNDLE_ID + "/icons/";
      IMG_OK = ImageDescriptor.createFromURL(new URL(prefix + "gcp-ok.png"));
      IMG_LOW = ImageDescriptor.createFromURL(new URL(prefix + "gcp-low.png"));
      IMG_MEDIUM = ImageDescriptor.createFromURL(new URL(prefix + "gcp-medium.png"));
      IMG_HIGH = ImageDescriptor.createFromURL(new URL(prefix + "gcp-high.png"));
      IMG_ERROR =
          ImageDescriptor.createFromURL(
              new URL("platform:/plugin/org.eclipse.jface/icons/full/message_error.png"));
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
    GcpStatus status = service.getCurrentStatus();
    element.setText("Status: " + status.summary);
    switch (status.severity) {
      case OK:
        element.setIcon(IMG_OK);
        element.setTooltip(status.summary);
        break;
      case LOW:
        element.setIcon(IMG_LOW);
        element.setTooltip(summarizeIncidents(status.active));
        break;
      case MEDIUM:
        element.setIcon(IMG_MEDIUM);
        element.setTooltip(summarizeIncidents(status.active));
        break;
      case HIGH:
        element.setIcon(IMG_HIGH);
        element.setTooltip(summarizeIncidents(status.active));
        break;
      case ERROR:
      default:
        element.setIcon(IMG_ERROR);
        element.setTooltip(status.summary); // show error text
        break;
    }
  }

  private String summarizeIncidents(Collection<Incident> incidents) {
    List<String> messages = new ArrayList<>();
    for (Incident incident : incidents) {
      messages.add(incident.toString());
    }
    return Joiner.on("\n").join(messages);
  }
}
