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

package com.google.cloud.tools.eclipse.appengine.localserver;

import com.google.api.client.util.Base64;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.Keys;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.ServiceAccountKey;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServiceAccountUtil {

  /**
   * Creates and saves a service account key the App Engine default service account.
   *
   * @param credential credential to use to create a service account key
   * @param projectId GCP project ID for {@code serviceAccountId} 
   * @param destination path of a key file to be saved
   */
  public static void createAppEngineDefaultServiceAccountKey(String projectId, Path destination)
          throws FileAlreadyExistsException, IOException {
    Preconditions.checkArgument(GoogleApiFactory.INSTANCE.getCredential().isPresent(), "credentials not set");
    Preconditions.checkArgument(!projectId.isEmpty(), "project ID empty");
    Preconditions.checkArgument(destination.isAbsolute(), "destination not absolute");

    if (!Files.exists(destination.getParent())) {
      Files.createDirectories(destination.getParent());
    }

    Iam iam = GoogleApiFactory.INSTANCE.newIamApi();
    Keys keys = iam.projects().serviceAccounts().keys();
    
    String projectEmail = projectId;
    // The appengine service account for google.com:gcloud-for-eclipse-testing 
    // would be gcloud-for-eclipse-testing.google.com@appspot.gserviceaccount.com.
    if (projectId.contains(":")) {
      String[] parts = projectId.split(":");
      projectEmail = parts[1] + "." + parts[0];
    }
    String serviceAccountId = projectEmail + "@appspot.gserviceaccount.com";

    String keyId = "projects/" + projectId + "/serviceAccounts/" + serviceAccountId;
    CreateServiceAccountKeyRequest createRequest = new CreateServiceAccountKeyRequest();
    ServiceAccountKey key = keys.create(keyId, createRequest).execute();

    byte[] jsonKey = Base64.decodeBase64(key.getPrivateKeyData());
    Files.write(destination, jsonKey);
  }
}
