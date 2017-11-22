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

package com.google.cloud.tools.eclipse.appengine.localserver;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.Base64;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.Keys;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.ServiceAccountKey;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class ServiceAccountUtil {

  /**
   * Creates and saves a service account key (JSON) for a service account.
   *
   * @param credential credential to use to create a service account key
   * @param projectId GCP project ID for {@code serviceAccountId} 
   * @param serviceAccountId the service account ID (for example, {@code
   *     project-id@appspot.gserviceaccount.com}
   * @param destination path of a key file to be saved
   * @throws FileAlreadyExistsException if {@code destination} already exists
   * @throws IOException if creation fails
   */
  public static void createServiceAccountKey(Credential credential, String projectId,
      String serviceAccountId, Path destination) throws FileAlreadyExistsException, IOException {
    createServiceAccountKey(credential, projectId, serviceAccountId, destination,
        getGoogleApiFactory());
  }

  @VisibleForTesting
  static void createServiceAccountKey(Credential credential, String projectId,
      String serviceAccountId, Path destination, IGoogleApiFactory apiFactory)
          throws FileAlreadyExistsException, IOException {
    Preconditions.checkNotNull(credential, "credential not given");
    Preconditions.checkState(!projectId.isEmpty(), "project ID empty");
    Preconditions.checkState(!serviceAccountId.isEmpty(), "service account empty");

    if (Files.exists(destination)) {
      throw new FileAlreadyExistsException(destination.toString());
    }

    Iam iam = apiFactory.newIamApi(credential);
    Keys keys = iam.projects().serviceAccounts().keys();

    String keyId = "projects/" + projectId + "/serviceAccounts/" + serviceAccountId;
    CreateServiceAccountKeyRequest createRequest = new CreateServiceAccountKeyRequest();
    ServiceAccountKey key = keys.create(keyId, createRequest).execute();

    byte[] jsonKey = Base64.decodeBase64(key.getPrivateKeyData());
    Files.write(destination, jsonKey);
  }

  @VisibleForTesting
  static IGoogleApiFactory getGoogleApiFactory() {
    BundleContext bundleContext = FrameworkUtil.getBundle(Activator.class).getBundleContext();
    return bundleContext.getService(bundleContext.getServiceReference(IGoogleApiFactory.class));
  }
}
