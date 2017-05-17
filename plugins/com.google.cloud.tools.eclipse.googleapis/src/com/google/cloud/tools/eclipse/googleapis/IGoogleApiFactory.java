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
import com.google.api.services.appengine.v1.Appengine.Apps;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.storage.Storage;

/**
 * Interface for factory classes to create clients for Google APIs.
 * <p>
 * <i>This interface exists because of the requirement on OSGi services that they need to implement
 * an interface</i>
 */
public interface IGoogleApiFactory {

  /**
   * @return a Google Cloud Storage API client
   */
  Storage newStorageApi(Credential credential) throws GoogleApiException;  
  
  /**
   * @return an Appengine Apps API client
   */
  Apps newAppsApi(Credential credential) throws GoogleApiException;

  /**
   * @return a CloudResourceManager/Projects API client
   */
  Projects newProjectsApi(Credential credential) throws GoogleApiException;

}
