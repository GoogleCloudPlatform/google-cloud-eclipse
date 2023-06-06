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
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.appengine.v1.Appengine.Apps;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.servicemanagement.ServiceManagement;
import com.google.api.services.storage.Storage;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.util.Optional;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Class to obtain various Google Cloud Platform related APIs.
 */
public class GoogleApiFactory implements IGoogleApiFactory {

  private static AccountProvider accountProvider = DefaultAccountProvider.INSTANCE;
  
  private final JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
  private final ProxyFactory proxyFactory;
  
  private LoadingCache<GoogleApi, HttpTransport> transportCache;
  private IProxyService proxyService;

  public static GoogleApiFactory INSTANCE = new GoogleApiFactory();
  
  private final IProxyChangeListener proxyChangeListener = new IProxyChangeListener() {
    @Override
    public void proxyInfoChanged(IProxyChangeEvent event) {
      if (transportCache != null) {
        transportCache.invalidateAll();
      }
    }
  };

  @VisibleForTesting 
  GoogleApiFactory() {
    this(new ProxyFactory());
  }

  @VisibleForTesting
  GoogleApiFactory(ProxyFactory proxyFactory) {
    Preconditions.checkNotNull(proxyFactory, "proxyFactory is null");
    this.proxyFactory = proxyFactory;
    // NetHttpTransport advises: "For maximum efficiency, applications should use a single
    // globally-shared instance of the HTTP transport." But as we need a separate proxy per URL,
    // we cannot reuse the same httptransport.
    transportCache =
        CacheBuilder.newBuilder().weakValues().build(new TransportCacheLoader(proxyFactory));
  }
  
  @Override
  public void addCredentialChangeListener(Runnable listener) {
    accountProvider.addCredentialChangeListener(listener);
  }

  @Override
  public void removeCredentialChangeListener(Runnable listener) {
    accountProvider.removeCredentialChangeListener(listener);
  }
  
  @Override
  public Optional<Account> getAccount() {
    return accountProvider.getAccount();
  }
  
  @Override
  public Optional<Credential> getCredential() {
    return accountProvider.getCredential();
  }
  
  private Credential getCredentialOrFail() {
    Optional<Credential> credential = getCredential();
    Preconditions.checkState(credential.isPresent(), "credential is not present");
    return credential.get();
  }
 
  @Override
  public Projects newProjectsApi() {
    Preconditions.checkNotNull(transportCache, "transportCache is null");
    HttpTransport transport = transportCache.getUnchecked(GoogleApi.CLOUDRESOURCE_MANAGER_API);
    Preconditions.checkNotNull(transport, "transport is null");
    Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");
    Credential credential = getCredentialOrFail();
    CloudResourceManager resourceManager =
        new CloudResourceManager.Builder(transport, jsonFactory, credential)
            .setApplicationName(CloudToolsInfo.USER_AGENT).build();
    
    return resourceManager.projects();
  }

  @Override
  public Storage newStorageApi() {
    Preconditions.checkNotNull(transportCache, "transportCache is null");
    HttpTransport transport = transportCache.getUnchecked(GoogleApi.CLOUD_STORAGE_API);
    Preconditions.checkNotNull(transport, "transport is null");
    Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");
    Credential credential = getCredentialOrFail();

    Storage.Builder builder = new Storage.Builder(transport, jsonFactory, credential)
        .setApplicationName(CloudToolsInfo.USER_AGENT);
    Storage storage = builder.build();
    return storage;
  }

  @Override
  public Apps newAppsApi() {
    Preconditions.checkNotNull(transportCache, "transportCache is null");
    HttpTransport transport = transportCache.getUnchecked(GoogleApi.APPENGINE_ADMIN_API);
    Preconditions.checkNotNull(transport, "transport is null");
    Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");
    Credential credential = getCredentialOrFail();

    Appengine appengine =
        new Appengine.Builder(transport, jsonFactory, credential)
            .setApplicationName(CloudToolsInfo.USER_AGENT).build();
    return appengine.apps();
  }

  @Override
  public ServiceManagement newServiceManagementApi() {
    Preconditions.checkNotNull(transportCache, "transportCache is null");
    HttpTransport transport = transportCache.getUnchecked(GoogleApi.SERVICE_MANAGEMENT_API);
    Preconditions.checkNotNull(transport, "transport is null");
    Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");
    Credential credential = getCredentialOrFail();

    ServiceManagement serviceManagement =
        new ServiceManagement.Builder(transport, jsonFactory, credential)
            .setApplicationName(CloudToolsInfo.USER_AGENT).build();
    return serviceManagement;
  }

  @Override
  public Iam newIamApi() {
    Preconditions.checkNotNull(transportCache, "transportCache is null");
    HttpTransport transport = transportCache.getUnchecked(GoogleApi.IAM_API);
    Preconditions.checkNotNull(transport, "transport is null");
    Preconditions.checkNotNull(jsonFactory, "jsonFactory is null");
    Credential credential = getCredentialOrFail();

    Iam iam = new Iam.Builder(transport, jsonFactory, credential)
        .setApplicationName(CloudToolsInfo.USER_AGENT).build();
    return iam;
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
  void setTransportCache(LoadingCache<GoogleApi, HttpTransport> transportCache) {
    this.transportCache = transportCache;
  }
  
  /**
   * Use case: set a test provider.
   * TestAccountProvider is defined in com.google.(...).test.util, which is a non-test package
   * @param provider the new account provider to be used by this class
   */
  public static void setAccountProvider(AccountProvider provider) {
    accountProvider = provider;
  }
  
  @VisibleForTesting
  public static void setInstance(GoogleApiFactory instance) {
    INSTANCE = instance;
  }
  
  public static void resetInstance() {
    INSTANCE = new GoogleApiFactory();
  }
}
