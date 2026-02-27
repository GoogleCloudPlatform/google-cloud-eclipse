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

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

/**
 * A client that interacts directly with Google Cloud Storage to provide Dataflow-plugin specific
 * functionality.
 */
public class GcsDataflowProjectClient {
  private static final String GCS_PREFIX = "gs://";

  private final Storage gcsClient;

  public static GcsDataflowProjectClient create() {
    return new GcsDataflowProjectClient(GoogleApiFactory.INSTANCE.newStorageApi());
  }

  private GcsDataflowProjectClient(Storage gcsClient) {
    this.gcsClient = gcsClient;
  }

  /**
   * Gets a collection of potential Staging Locations.
   */
  public SortedSet<String> getPotentialStagingLocations(String projectId) throws IOException {
    SortedSet<String> result = new TreeSet<>();
    Buckets buckets = gcsClient.buckets().list(projectId).execute();
    List<Bucket> bucketList = buckets.getItems();
    if (bucketList != null) {
      for (Bucket bucket : bucketList) {
        result.add(GCS_PREFIX + bucket.getName());
      }
    }
    return result;
  }

  /**
   * Uses the provided specification for the staging location, creating it if it does not already
   * exist. This may be a long-running blocking operation.
   */
  public StagingLocationVerificationResult createStagingLocation(
      String projectId, String stagingLocation, IProgressMonitor progressMonitor) {
    SubMonitor monitor = SubMonitor.convert(progressMonitor, 2);
    String bucketName = toGcsBucketName(stagingLocation);
    if (locationIsAccessible(stagingLocation)) { // bucket already exists
      return new StagingLocationVerificationResult(
          String.format("Bucket %s exists", bucketName), true);
    }
    monitor.worked(1);

    // else create the bucket
    try {
      Bucket newBucket = new Bucket();
      newBucket.setName(bucketName);
      gcsClient.buckets().insert(projectId, newBucket).execute();
      return new StagingLocationVerificationResult(
          String.format("Bucket %s created", bucketName), true);
    } catch (GoogleJsonResponseException ex) {
      GoogleJsonError error = ex.getDetails();
      return new StagingLocationVerificationResult(error.getMessage(), false);
    } catch (IOException e) {
      return new StagingLocationVerificationResult(e.getMessage(), false);
    } finally {
      monitor.done();
    }
  }

  /**
   * Extracts a bucket name from a given GCS URL. For example, {@code "/bucket/object"} returns
   * {@code "bucket"}. The method assumes that the input is a valid GCS URL. (It just returns
   * the first segment split by {@code '/'}, ignoring leading {@code '/'}s and empty segments.)
   *
   * @param gcsUrl GCS URL, which may or may not start with case-insensitive {@link #GCS_PREFIX}
   * @return bucket name, which can be an empty string
   */
  public static String toGcsBucketName(String gcsUrl) {
    String noPrefixUrl;
    if (gcsUrl.toLowerCase(Locale.US).startsWith(GCS_PREFIX)) {
      noPrefixUrl = gcsUrl.substring(GCS_PREFIX.length());
    } else {
      noPrefixUrl = gcsUrl;
    }

    List<String> splitted = Splitter.on('/').omitEmptyStrings().splitToList(noPrefixUrl);
    return splitted.isEmpty() ? "" : splitted.get(0);
  }

  public static String toGcsLocationUri(String location) {
    if (Strings.isNullOrEmpty(location)) {
      return location;
    } else if (location.toLowerCase(Locale.US).startsWith(GCS_PREFIX)) {
      return GCS_PREFIX + location.substring(GCS_PREFIX.length());
    }
    return GCS_PREFIX + location;
  }

  /**
   * Gets whether the current staging location exists and is accessible. If this method returns
   * true, the provided staging location can be used.
   */
  boolean locationIsAccessible(String stagingLocation) {
    String bucketName = toGcsBucketName(stagingLocation);
    try {
      gcsClient.buckets().get(bucketName).execute();
      return true;
    } catch (IOException ex) {
      return false;
    }
  }

  /**
   * The result of creating or verifying a Staging Location.
   */
  public static class StagingLocationVerificationResult {
    private final String message;
    private final boolean successful;

    public StagingLocationVerificationResult(String message, boolean successful) {
      this.message = message;
      this.successful = successful;
    }

    /**
     * Gets the message associated with this attempt to create a staging location.
     */
    public String getMessage() {
      return message;
    }

    /**
     * Return whether this attempt to create a staging location was succesful.
     */
    public boolean isSuccessful() {
      return successful;
    }
  }
}

