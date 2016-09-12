package com.google.cloud.tools.eclipse.ui.util.event;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.google.common.annotations.VisibleForTesting;

public class OpenUriSelectionListener implements SelectionListener {

  private static final String URI_PARAM_PROJECT = "project";
  private ErrorHandler errorHandler;
  private IWorkbenchBrowserSupport browserSupport;
  private ProjectIdProvider projectIdProvider;

  public OpenUriSelectionListener(ProjectIdProvider projectIdProvider, ErrorHandler errorHandler) {
    this(projectIdProvider, errorHandler, PlatformUI.getWorkbench().getBrowserSupport());
  }

  @VisibleForTesting
  OpenUriSelectionListener(ProjectIdProvider projectIdProvider, ErrorHandler errorHandler, IWorkbenchBrowserSupport browserSupport) {
    this.projectIdProvider = projectIdProvider;
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

  private void openAppEngineDashboard(String uriString) {
    try {
      String projectId = projectIdProvider.getProjectId();
      URI uri = appendProjectId(new URI(uriString), projectId);
      browserSupport.getExternalBrowser().openURL(uri.toURL());
    } catch (PartInitException | MalformedURLException | URISyntaxException ex) {
      errorHandler.handle(ex);
    }
  }

  private URI appendProjectId(URI uri, String projectId) throws URISyntaxException {
    if (projectId != null && !projectId.isEmpty() && !projectId.trim().isEmpty()) {
      projectId = projectId.trim();
      String query = uri.getQuery();
      if (query == null) {
        query = URI_PARAM_PROJECT + "=" + projectId;
      } else {
        query += "&" + URI_PARAM_PROJECT + "=" + projectId;
      }
      return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(),
                     uri.getPort(), uri.getPath(), query, uri.getFragment());
    } else {
      return uri;
    }
  }

  public static interface ErrorHandler {
    void handle(Exception ex);
  }

  public static interface ProjectIdProvider {
    String getProjectId();
  }
}
