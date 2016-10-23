/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.ui.util.event;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.google.common.annotations.VisibleForTesting;

public class OpenUriSelectionListener implements SelectionListener {

  private ErrorHandler errorHandler;
  private IWorkbenchBrowserSupport browserSupport;
  private QueryParameterProvider queryParameterProvider;

  public OpenUriSelectionListener(QueryParameterProvider queryParameterProvider, ErrorHandler errorHandler) {
    this(queryParameterProvider, errorHandler, PlatformUI.getWorkbench().getBrowserSupport());
  }

  @VisibleForTesting
  OpenUriSelectionListener(QueryParameterProvider queryParameterProvider,
                           ErrorHandler errorHandler,
                           IWorkbenchBrowserSupport browserSupport) {
    this.queryParameterProvider = queryParameterProvider;
    this.errorHandler = errorHandler;
    this.browserSupport = browserSupport;
  }

  @Override
  public void widgetSelected(SelectionEvent event) {
    openUri(event.text);
  }

  @Override
  public void widgetDefaultSelected(SelectionEvent event) {
    openUri(event.text);
  }

  private void openUri(String uriString) {
    try {
      URI uri = appendQueryParameters(new URI(uriString));
      browserSupport.getExternalBrowser().openURL(uri.toURL());
    } catch (PartInitException | MalformedURLException | URISyntaxException ex) {
      errorHandler.handle(ex);
    }
  }

  private URI appendQueryParameters(URI uri) throws URISyntaxException {
    String queryString = uri.getQuery();
    if (queryString == null) {
      queryString = "";
    }
    StringBuilder query = new StringBuilder(queryString);
    for (Entry<String, String> parameter : queryParameterProvider.getParameters().entrySet()) {
      if (query.length() > 0) {
        query.append('&');
      }
      query.append(parameter.getKey())
           .append('=')
           .append(parameter.getValue());
    }

    return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(),
                   uri.getPort(), uri.getPath(), query.toString(), uri.getFragment());
  }

  public static interface ErrorHandler {
    void handle(Exception ex);
  }

  public static interface QueryParameterProvider {
    Map<String, String> getParameters();
  }
}
