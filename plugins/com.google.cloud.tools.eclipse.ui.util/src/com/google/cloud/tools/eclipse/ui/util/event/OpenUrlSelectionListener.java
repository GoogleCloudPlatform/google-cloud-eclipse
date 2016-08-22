package com.google.cloud.tools.eclipse.ui.util.event;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.google.common.annotations.VisibleForTesting;

public class OpenUrlSelectionListener implements SelectionListener {

  private ErrorHandler errorHandler;
  private IWorkbenchBrowserSupport browserSupport;

  public OpenUrlSelectionListener(ErrorHandler errorHandler) {
    this(errorHandler, PlatformUI.getWorkbench().getBrowserSupport());
  }

  @VisibleForTesting
  OpenUrlSelectionListener(ErrorHandler errorHandler, IWorkbenchBrowserSupport browserSupport) {
    this.errorHandler = errorHandler;
    this.browserSupport = browserSupport;
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
      browserSupport.getExternalBrowser().openURL(new URL(url));
    } catch (PartInitException | MalformedURLException ex) {
      errorHandler.handle(ex);
    }
  }

  public static interface ErrorHandler {
    void handle(Exception ex);
  }
}