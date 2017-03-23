/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.services.storage.Storage;

import java.io.IOException;
import java.util.Arrays;

/**
 * Helpers for interacting with Google Cloud Platform APIs.
 */
public class Transport {
  private static final String CLOUD_PLATFORM_SCOPE =
      "https://www.googleapis.com/auth/cloud-platform";

  private static final String GCP_APPLICATION_NAME = "google-cloud-dataflow-eclipse-plugin";

  /**
   * Returns a Cloud Storage client builder, or throws a {@link CouldNotCreateCredentialsException}
   * if the client could not be created due to an {@link IOException} while creating Application
   * Default Credentials.
   */
  public static Storage.Builder newStorageClient() throws CouldNotCreateCredentialsException {
    GoogleCredential applicationDefaultCredentials;

    try {
      applicationDefaultCredentials = GoogleCredential.getApplicationDefault();
    } catch (IOException e) {
      throw new CouldNotCreateCredentialsException(
          "Could not construct GCS client using default credentials."
          + "\nHave you authenticated with 'gcloud auth'?"
          + "\n\nSee https://cloud.google.com/sdk/gcloud/ for more",
          e);
    }
    Storage.Builder storageBuilder =
        new Storage
            .Builder(Utils.getDefaultTransport(), Utils.getDefaultJsonFactory(),
                applicationDefaultCredentials.createScoped(
                    Arrays.asList(CLOUD_PLATFORM_SCOPE)))
            .setApplicationName(GCP_APPLICATION_NAME);
    return storageBuilder;
  }
}

