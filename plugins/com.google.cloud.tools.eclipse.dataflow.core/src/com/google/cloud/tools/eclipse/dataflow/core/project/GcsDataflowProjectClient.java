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

package com.google.cloud.tools.eclipse.dataflow.core.project;

import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;
import com.google.cloud.tools.eclipse.dataflow.core.util.CouldNotCreateCredentialsException;
import com.google.cloud.tools.eclipse.dataflow.core.util.Transport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A client that interacts directly with Google Cloud Storage to provide Dataflow-plugin specific
 * functionality.
 */
public class GcsDataflowProjectClient {
  private static final String GCS_PREFIX = "gs://";

  private final Storage gcsClient;

  public static GcsDataflowProjectClient createWithDefaultClient()
      throws CouldNotCreateCredentialsException {
    return new GcsDataflowProjectClient(Transport.newStorageClient().build());
  }

  @VisibleForTesting
  GcsDataflowProjectClient(Storage gcsClient) {
    this.gcsClient = gcsClient;
  }

  /**
   * Gets a collection of potential Staging Locations.
   */
  public SortedSet<String> getPotentialStagingLocations(String projectName) throws IOException {
    SortedSet<String> result = new TreeSet<>();
    Buckets buckets = gcsClient.buckets().list(projectName).execute();
    List<Bucket> bucketList = buckets.getItems();
    for (Bucket bucket : bucketList) {
      result.add(GCS_PREFIX + bucket.getName());
    }
    return result;
  }

  /**
   * Uses the provided specification for the staging location, creating it if it does not already
   * exist. This may be a long-running blocking operation.
   */
  public StagingLocationVerificationResult createStagingLocation(
      String projectName, String stagingLocation, IProgressMonitor progressMonitor) {
    SubMonitor monitor = SubMonitor.convert(progressMonitor, 2);
    monitor.newChild(1);
    String bucketName = toGcsBucketName(stagingLocation);
    try {
      if (verifyLocationIsAccessible(stagingLocation)) {
        return new StagingLocationVerificationResult(
            String.format("Bucket %s exists", bucketName), true);
      }
    } catch (IOException e) {
      DataflowCorePlugin.logInfo(
          "IOException when attempting to access Bucket %s", stagingLocation);
      // Continue.
    }
    try {
      monitor.newChild(1);
      Bucket newBucket = new Bucket();
      newBucket.setName(bucketName);
      gcsClient.buckets().insert(projectName, newBucket).execute();
    } catch (IOException e) {
      return new StagingLocationVerificationResult(e.getMessage(), false);
    } finally {
      monitor.done();
    }
    return new StagingLocationVerificationResult(
        String.format("Bucket %s created", bucketName), true);
  }

  private String toGcsBucketName(String stagingLocation) {
    final String gcsLocation;
    if (stagingLocation.startsWith(GCS_PREFIX)) {
      gcsLocation = stagingLocation.substring(GCS_PREFIX.length());
    } else {
      gcsLocation = stagingLocation;
    }
    final String bucketName;
    if (gcsLocation.indexOf('/') < 0) {
      bucketName = gcsLocation;
    } else {
      bucketName = gcsLocation.substring(0, gcsLocation.indexOf('/'));
    }
    return bucketName;
  }

  public String toGcsLocationUri(String location) {
    if (Strings.isNullOrEmpty(location) || location.startsWith(GCS_PREFIX)) {
      return location;
    }
    return GCS_PREFIX + location;
  }

  /**
   * Gets whether the current staging location exists and is accessible. If this method returns
   * true, the provided staging location can be used.
   * @throws IOException
   */
  public boolean verifyLocationIsAccessible(String stagingLocation) throws IOException {
    String bucketName = toGcsBucketName(stagingLocation);
    gcsClient.buckets().get(bucketName).execute();
    return true;
  }

  /**
   * The result of creating or verifying a Staging Location.
   */
  public class StagingLocationVerificationResult {
    private final String message;
    private final boolean successful;

    public StagingLocationVerificationResult(String message, boolean successful) {
      this.message = message;
      this.successful = successful;
    }

    /**
     * Gets the message associated with this attempt to create a staging location.
     */
    String getMessage() {
      return message;
    }

    /**
     * Return whether this attempt to create a staging location was succesful.
     */
    boolean isSuccessful() {
      return successful;
    }
  }
}

