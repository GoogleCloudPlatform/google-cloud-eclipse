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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A job that verifies a Staging Location.
 */
public class VerifyStagingLocationJob extends Job {
  private final GcsDataflowProjectClient client;
  private final String email;
  private final String stagingLocation;
  private final SettableFuture<VerifyStagingLocationResult> future;

  public VerifyStagingLocationJob(GcsDataflowProjectClient client,
      String email, String stagingLocation) {
    super("Verify Staging Location " + stagingLocation);
    this.client = client;
    this.email = email;
    this.stagingLocation = stagingLocation;
    this.future = SettableFuture.create();
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    VerifyStagingLocationResult result = new VerifyStagingLocationResult(
        email, stagingLocation, client.locationIsAccessible(stagingLocation));
    if (monitor.isCanceled()) {
      future.cancel(false);
      return Status.CANCEL_STATUS;
    }
    future.set(result);
    return Status.OK_STATUS;
  }

  public ListenableFuture<VerifyStagingLocationResult> getVerifyResult() {
    return future;
  }

  public String getEmail() {
    return email;
  }

  public String getStagingLocation() {
    return stagingLocation;
  }

  /**
   * The result of verifying a staging location: the staging location, the account email used
   * to access the location, and the verification result.
   */
  public static class VerifyStagingLocationResult {
    public final String email;
    public final String stagingLocation;
    public final boolean accessible;

    public VerifyStagingLocationResult(String email, String stagingLocation, boolean accessible) {
      this.email = email;
      this.stagingLocation = stagingLocation;
      this.accessible = accessible;
    }
  }
}
