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
import com.google.api.services.iam.v1.Iam;
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
  APPENGINE_ADMIN_API(Appengine.DEFAULT_BASE_URL, "appengine.googleapis.com"),
  CLOUDRESOURCE_MANAGER_API(
      CloudResourceManager.DEFAULT_BASE_URL, "cloudresourcemanager.googleapis.com"),
  CLOUD_STORAGE_API(Storage.DEFAULT_BASE_URL, "storage-api.googleapis.com"),
  SERVICE_MANAGEMENT_API(ServiceManagement.DEFAULT_BASE_URL, "servicemanagement.googleapis.com"),
  IAM_API(Iam.DEFAULT_BASE_URL, "iam.googleapis.com");

  private final URI uri;
  private final String serviceId;

  private GoogleApi(String url, String serviceId) {
    try {
      uri = new URI(url);
    } catch (URISyntaxException ex) {
      throw new RuntimeException("Fix URL");
    }
    this.serviceId = serviceId;
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
