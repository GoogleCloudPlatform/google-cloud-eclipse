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

import com.google.api.services.appengine.v1.Appengine;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.dataflow.Dataflow;
import com.google.api.services.servicemanagement.ServiceManagement;
import com.google.api.services.storage.Storage;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Captures important API enablement and usage information for various Google APIs:
 * <ol>
 * <li>The API name, for use in UI.</li>
 * <li>The base endpoint used for access, for determining proxying requirements.</li>
 * <li>The service-management ID to check for project API enablement.</li>
 * </ol>
 */
public enum GoogleApi {
  APPENGINE_ADMIN_API(Messages.getString("api.appengine"), Appengine.DEFAULT_BASE_URL, //$NON-NLS-1$
      "appengine.googleapis.com"), //$NON-NLS-1$
  DATAFLOW_API(Messages.getString("api.dataflow"), Dataflow.DEFAULT_BASE_URL, //$NON-NLS-1$
      "dataflow.googleapis.com"), //$NON-NLS-1$
  CLOUDRESOURCE_MANAGER_API(Messages.getString("api.cloudresourcemanager"), //$NON-NLS-1$
      CloudResourceManager.DEFAULT_BASE_URL, "cloudresourcemanager.googleapis.com"), //$NON-NLS-1$
  CLOUD_STORAGE_API(Messages.getString("api.storage-json"), Storage.DEFAULT_BASE_URL, //$NON-NLS-1$
      "storage-api.googleapis.com"), //$NON-NLS-1$
  SERVICE_MANAGEMENT_API(Messages.getString("api.servicemanagement"), //$NON-NLS-1$
      ServiceManagement.DEFAULT_BASE_URL, "servicemanagement.googleapis.com"); //$NON-NLS-1$

  private final String name;
  private final URI uri;
  private final String serviceId;

  /**
   * Return the corresponding API with the specified service management ID.
   * 
   * @return the corresponding instance or {@code null} if not found
   */
  public static GoogleApi forServiceID(String serviceId) {
    for (GoogleApi api : values()) {
      if (serviceId.equals(api.serviceId)) {
        return api;
      }
    }
    return null;
  }

  private GoogleApi(String name, String url, String serviceId) {
    try {
      uri = new URI(url);
    } catch (URISyntaxException ex) {
      throw new RuntimeException("Fix URL"); //$NON-NLS-1$
    }
    this.name = name;
    this.serviceId = serviceId;
  }

  /**
   * The API name.
   */
  public String getName() {
    return name;
  }

  /**
   * Return the service management ID used to describe this API.
   */
  public String getServiceId() {
    return serviceId;
  }


  public URI toUri() {
    return uri;
  }
}
