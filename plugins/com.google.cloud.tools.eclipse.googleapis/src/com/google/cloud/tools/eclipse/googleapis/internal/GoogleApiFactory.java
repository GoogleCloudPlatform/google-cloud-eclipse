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

package com.google.cloud.tools.eclipse.googleapis.internal;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.ConnectionFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.appengine.v1.Appengine.Apps;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Class to obtain various Google cloud Platform related APIs.
 */
@Component
public class GoogleApiFactory implements IGoogleApiFactory {

  private static final int DEFAULT_TIMEOUT_MS = 1000;

  private static final Logger logger = Logger.getLogger(GoogleApiFactory.class.getName());

  private JsonFactory jsonFactory;
  private Map<String, HttpTransport> transports = new HashMap<>();

  private IProxyService proxyService;
  private GoogleApiUrls googleApiUrls;

  private final IProxyChangeListener proxyChangeListener = new IProxyChangeListener() {
    @Override
    public void proxyInfoChanged(IProxyChangeEvent event) {
      buildTransports();
    }
  };

  public GoogleApiFactory() {
    this(new GoogleApiUrls());
  }

  @VisibleForTesting
  GoogleApiFactory(GoogleApiUrls googleApiUrls) {
    this.googleApiUrls = googleApiUrls;
  }

  @Activate
  public void init() {
    jsonFactory = new JacksonFactory();
    buildTransports();
  }

  /**
   * @return the CloudResourceManager/Projects API
   */
  @Override
  public Projects newProjectsApi(Credential credential) {
    synchronized (transports) {
      HttpTransport transport = transports.get(googleApiUrls.cloudResourceManagerUrl());
      Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");
      Preconditions.checkNotNull(transport, "transport is null");

      CloudResourceManager resourceManager =
          new CloudResourceManager.Builder(transports.get(googleApiUrls.cloudResourceManagerUrl()),
                                           jsonFactory, credential)
              .setApplicationName(CloudToolsInfo.USER_AGENT).build();
      Projects projects = resourceManager.projects();
      return projects;
    }
  }

  /**
   * @return the Appengine/Apps API
   */
  @Override
  public Apps newAppsApi(Credential credential) {
    synchronized (transports) {
      HttpTransport transport = transports.get(googleApiUrls.appEngineAdminUrl());
      Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");
      Preconditions.checkNotNull(transport, "transport is null");

      Appengine appengine =
          new Appengine.Builder(transport, jsonFactory,
                                credential)
              .setApplicationName(CloudToolsInfo.USER_AGENT).build();
      Apps apps = appengine.apps();
      return apps;
    }
  }

  private void buildTransports() {
    synchronized (transports) {
      try {
        buildAppEngineAdminTransport();
        buildCloudResourceManagerTransport();
      } catch (URISyntaxException ex) {
        logger.log(Level.SEVERE, "Could not create transport using the proxy settings", ex);
      }
    }
  }

  private void buildAppEngineAdminTransport() throws URISyntaxException {
    buildTransportForUrl(googleApiUrls.appEngineAdminUrl());
  }

  private void buildCloudResourceManagerTransport() throws URISyntaxException {
    buildTransportForUrl(googleApiUrls.cloudResourceManagerUrl());
  }

  private void buildTransportForUrl(String url) throws URISyntaxException {
    ConnectionFactory connectionFactory =
        new TimeoutAwareConnectionFactory(createProxy(url), DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS);
    transports.put(url,
                   new NetHttpTransport.Builder().setConnectionFactory(connectionFactory).build());
  }

  private Proxy createProxy(String url) throws URISyntaxException {
    Preconditions.checkNotNull(googleApiUrls, "googleApiUrl is null");
    Preconditions.checkArgument(!url.startsWith("http://"), "http is not supported schema");

    if (proxyService == null) {
      return Proxy.NO_PROXY;
    }

    IProxyData[] proxyDataForUri = proxyService.select(new URI(url));
    for (final IProxyData iProxyData : proxyDataForUri) {
      switch (iProxyData.getType()) {
        case IProxyData.HTTPS_PROXY_TYPE:
          return new Proxy(Type.HTTP, new InetSocketAddress(iProxyData.getHost(),
                                                            iProxyData.getPort()));
        case IProxyData.SOCKS_PROXY_TYPE:
          return new Proxy(Type.SOCKS, new InetSocketAddress(iProxyData.getHost(),
                                                             iProxyData.getPort()));
        default:
          logger.warning("Unsupported proxy type: " + iProxyData.getType());
          break;
      }
    }
    return Proxy.NO_PROXY;
  }

  @Reference(policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.OPTIONAL)
  public void setProxyService(IProxyService proxyService) {
    this.proxyService = proxyService;
    this.proxyService.addProxyChangeListener(proxyChangeListener);
    buildTransports();
  }

  public void unsetProxyService(IProxyService proxyService) {
    if (this.proxyService == proxyService) {
      proxyService.removeProxyChangeListener(proxyChangeListener);
      this.proxyService = null;
      buildTransports();
    }
  }

  @VisibleForTesting
  void setTransport(Map<String, HttpTransport> transport) {
    this.transports = transport;
  }

  @VisibleForTesting
  Map<String, HttpTransport> getTransports() {
    return transports;
  }

  @VisibleForTesting
  void setJsonFactory(JsonFactory jsonFactory) {
    this.jsonFactory = jsonFactory;
  }
}
