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
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.appengine.v1.Appengine.Apps;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.storage.Storage;
import com.google.cloud.tools.eclipse.googleapis.GoogleApiException;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Class to obtain various Google Cloud Platform related APIs.
 */
@Component
public class GoogleApiFactory implements IGoogleApiFactory {

  private IProxyService proxyService;

  private final JsonFactory jsonFactory = new JacksonFactory();
  private final ProxyFactory proxyFactory;
  private LoadingCache<GoogleApiUrl, HttpTransport> transportCache;

  private final IProxyChangeListener proxyChangeListener = new IProxyChangeListener() {
    @Override
    public void proxyInfoChanged(IProxyChangeEvent event) {
      if (transportCache != null) {
        transportCache.invalidateAll();
      }
    }
  };

  public GoogleApiFactory() {
    this(new ProxyFactory());
  }

  @VisibleForTesting
  public GoogleApiFactory(ProxyFactory proxyFactory) {
    Preconditions.checkNotNull(proxyFactory, "proxyFactory is null");
    this.proxyFactory = proxyFactory;
  }

  @Activate
  public void init() {
    transportCache =
        CacheBuilder.newBuilder().weakValues().build(new TransportCacheLoader(proxyFactory));
  }

  @Override
  public Projects newProjectsApi(Credential credential) throws GoogleApiException {
    try {
      Preconditions.checkNotNull(transportCache, "transportCache is null");
      HttpTransport transport = transportCache.get(GoogleApiUrl.CLOUDRESOURCE_MANAGER_API);
      Preconditions.checkNotNull(transport, "transport is null");
      Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");
      
      CloudResourceManager resourceManager =
          new CloudResourceManager.Builder(transport, jsonFactory, credential)
              .setApplicationName(CloudToolsInfo.USER_AGENT).build();
      return resourceManager.projects();
    } catch (ExecutionException ex) {
      throw new GoogleApiException(ex);
    }
  }
  
  @Override
  public Storage newStorageApi(Credential credential) throws GoogleApiException {
    try {
      Preconditions.checkNotNull(transportCache, "transportCache is null");
      HttpTransport transport = transportCache.get(GoogleApiUrl.CLOUD_STORAGE_API);
      Preconditions.checkNotNull(transport, "transport is null");
      Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");

      Storage.Builder builder = new Storage.Builder(transport, jsonFactory, credential);
      Storage storage = builder.build();
      return storage;
    } catch (ExecutionException ex) {
      throw new GoogleApiException(ex);
    }
  }

  @Override
  public Apps newAppsApi(Credential credential) throws GoogleApiException {
    try {
      Preconditions.checkNotNull(transportCache, "transportCache is null");
      HttpTransport transport = transportCache.get(GoogleApiUrl.APPENGINE_ADMIN_API);
      Preconditions.checkNotNull(transport, "transport is null");
      Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");

      Appengine appengine =
          new Appengine.Builder(transport, jsonFactory, credential)
              .setApplicationName(CloudToolsInfo.USER_AGENT).build();
      return appengine.apps();
    } catch (ExecutionException ex) {
      throw new GoogleApiException(ex);
    }
  }

  @Reference(policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.OPTIONAL)
  public void setProxyService(IProxyService proxyService) {
    this.proxyService = proxyService;
    this.proxyService.addProxyChangeListener(proxyChangeListener);
    proxyFactory.setProxyService(this.proxyService);
    if (transportCache != null) {
      transportCache.invalidateAll();
    }
  }

  public void unsetProxyService(IProxyService proxyService) {
    if (this.proxyService == proxyService) {
      proxyService.removeProxyChangeListener(proxyChangeListener);
      this.proxyService = null;
      proxyFactory.setProxyService(null);
      if (transportCache != null) {
        transportCache.invalidateAll();
      }
    }
  }

  @VisibleForTesting
  void setTransportCache(LoadingCache<GoogleApiUrl, HttpTransport> transportCache) {
    this.transportCache = transportCache;
  }
}
