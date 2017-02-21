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

package com.google.cloud.tools.eclipse.projectselector;

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
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;

/**
 * Class to obtain various Google cloud Platform related APIs.
 *
 * TODO move this class into a separate API bundle
 * https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1438
 */
public class GoogleApiFactory {

  private static final int DEFAULT_TIMEOUT_MS = 1000;
  private static final JsonFactory jsonFactory = new JacksonFactory();
  private static final ConnectionFactory connectionFactory =
      new TimeoutAwareConnectionFactory(DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS);
  private static final HttpTransport transport =
      new NetHttpTransport.Builder().setConnectionFactory(connectionFactory).build();

  /**
   * @return the CloudResourceManager/Projects API
   */
  public Projects newProjectsApi(Credential credential) {
    CloudResourceManager resourceManager =
        new CloudResourceManager.Builder(transport, jsonFactory, credential)
            .setApplicationName(CloudToolsInfo.USER_AGENT).build();
    Projects projects = resourceManager.projects();
    return projects;
  }

  /**
   * @return the Appengine/Apps API
   */
  public Apps newAppsApi(Credential credential) {
    Appengine appengine =
        new Appengine.Builder(transport, jsonFactory, credential)
            .setApplicationName(CloudToolsInfo.USER_AGENT).build();
    Apps apps = appengine.apps();
    return apps;
  }
}
