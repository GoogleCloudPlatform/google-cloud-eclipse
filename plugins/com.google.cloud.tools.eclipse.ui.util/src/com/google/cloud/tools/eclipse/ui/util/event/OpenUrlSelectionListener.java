package com.google.cloud.tools.eclipse.ui.util.event;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class OpenUrlSelectionListener implements SelectionListener {

  private ErrorHandler errorHandler;

  public OpenUrlSelectionListener(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  @Override
  public void widgetSelected(SelectionEvent event) {
    openAppEngineDashboard(event.text);
  }

  @Override
  public void widgetDefaultSelected(SelectionEvent event) {
    openAppEngineDashboard(event.text);
  }

  private void openAppEngineDashboard(String url) {
    try {
      IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
      browserSupport.getExternalBrowser().openURL(new URL(url));
    } catch (PartInitException | MalformedURLException ex) {
      errorHandler.handle(ex);
    }
  }

  public static interface ErrorHandler {
    void handle(Exception ex);
  }
}