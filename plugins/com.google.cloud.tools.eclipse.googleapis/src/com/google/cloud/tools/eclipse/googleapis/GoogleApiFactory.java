/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.googleapis;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.ConnectionFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.appengine.v1.Appengine.Apps;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.annotations.VisibleForTesting;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Class to obtain various Google cloud Platform related APIs.
 */
@Component
public class GoogleApiFactory implements IGoogleApiFactory {

  private static final String GOOGLEAPIS_URL = "https://appengine.googleapis.com";
  private static final int DEFAULT_TIMEOUT_MS = 1000;

  private static final Logger logger = Logger.getLogger(GoogleApiFactory.class.getName());

  private JsonFactory jsonFactory;
  private HttpTransport transport;

  private IProxyService proxyService;
  private String googleApiUrl;

  public GoogleApiFactory() {
    this(GOOGLEAPIS_URL);
  }

  @VisibleForTesting
  GoogleApiFactory(String googleApiUrl) {
    this.googleApiUrl = googleApiUrl;
  }

  @Activate
  public void init() {
    Preconditions.checkNotNull(proxyService, "proxyService is null");
    jsonFactory = new JacksonFactory();
    buildTransport();
    proxyService.addProxyChangeListener(new IProxyChangeListener() {
      @Override
      public void proxyInfoChanged(IProxyChangeEvent event) {
        buildTransport();
      }
    });
  }

  private void buildTransport() {
    try {
    ConnectionFactory connectionFactory =
        new TimeoutAwareConnectionFactory(createProxy(), DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS);
    transport =
        new NetHttpTransport.Builder().setConnectionFactory(connectionFactory).build();
    } catch (URISyntaxException ex) {
      logger.log(Level.SEVERE, "Could not create transport using the proxy settings", ex);
      transport = null;
    }
  }

  /**
   * @return the CloudResourceManager/Projects API
   */
  @Override
  public Projects newProjectsApi(Credential credential) {
    checkFields();
    CloudResourceManager resourceManager =
        new CloudResourceManager.Builder(transport, jsonFactory, credential)
            .setApplicationName(CloudToolsInfo.USER_AGENT).build();
    Projects projects = resourceManager.projects();
    return projects;
  }

  /**
   * @return the Appengine/Apps API
   */
  @Override
  public Apps newAppsApi(Credential credential) {
    checkFields();
    Appengine appengine =
        new Appengine.Builder(transport, jsonFactory, credential)
            .setApplicationName(CloudToolsInfo.USER_AGENT).build();
    Apps apps = appengine.apps();
    return apps;
  }

  private void checkFields() {
    Preconditions.checkNotNull(transport, "transport is null");
    Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");
  }

  private Proxy createProxy() throws URISyntaxException {
    Preconditions.checkNotNull(proxyService, "proxyService is null");
    Preconditions.checkNotNull(googleApiUrl, "googleApiUrl is null");

    IProxyData[] proxyData = proxyService.select(new URI(googleApiUrl));
    for (final IProxyData iProxyData : proxyData) {
      if (IProxyData.HTTPS_PROXY_TYPE.equals(iProxyData.getType())) {
        return new Proxy(Type.HTTP, new InetSocketAddress(iProxyData.getHost(), iProxyData.getPort()));
      }
    }
    return Proxy.NO_PROXY;
  }

  @Reference(unbind="-")
  public void setProxyService(IProxyService proxyService) {
    this.proxyService = proxyService;
  }

  @VisibleForTesting
  void setTransport(HttpTransport transport) {
    this.transport = transport;
  }

  @VisibleForTesting
  HttpTransport getTransport() {
    return transport;
  }

  @VisibleForTesting
  void setJsonFactory(JsonFactory jsonFactory) {
    this.jsonFactory = jsonFactory;
  }
}
