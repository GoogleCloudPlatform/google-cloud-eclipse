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

import com.google.cloud.tools.eclipse.dataflow.core.proxy.ListenableFutureProxy;
import com.google.common.util.concurrent.SettableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.IOException;

/**
 * A job that verifies a Staging Location.
 */
public class VerifyStagingLocationJob extends Job {
  private final GcsDataflowProjectClient client;
  private final String stagingLocation;
  private final SettableFuture<VerifyStagingLocationResult> future;

  public static VerifyStagingLocationJob create(
      GcsDataflowProjectClient client, String stagingLocation) {
    return new VerifyStagingLocationJob(client, stagingLocation);
  }

  private VerifyStagingLocationJob(GcsDataflowProjectClient client, String stagingLocation) {
    super("Verify Staging Location " + stagingLocation);
    this.client = client;
    this.stagingLocation = stagingLocation;
    this.future = SettableFuture.create();
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      VerifyStagingLocationResult result = new VerifyStagingLocationResult(
          stagingLocation, client.verifyLocationIsAccessible(stagingLocation));
      future.set(result);
    } catch (IOException e) {
      future.setException(e);
    }
    return Status.OK_STATUS;
  }

  public ListenableFutureProxy<VerifyStagingLocationResult> getVerifyResult() {
    return new ListenableFutureProxy<>(future);
  }

  /**
   * The result of verifying a staging location, containing both the staging location and the
   * verification result.
   */
  public static class VerifyStagingLocationResult {
    private final String stagingLocation;
    private final boolean accessible;

    public VerifyStagingLocationResult(String stagingLocation, boolean accessible) {
      this.stagingLocation = stagingLocation;
      this.accessible = accessible;
    }

    public String getStagingLocation() {
      return stagingLocation;
    }

    public boolean isAccessible() {
      return accessible;
    }
  }
}
