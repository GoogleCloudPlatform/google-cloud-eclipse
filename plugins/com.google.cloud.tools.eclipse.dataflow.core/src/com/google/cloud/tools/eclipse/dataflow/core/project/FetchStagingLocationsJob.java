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
import java.io.IOException;
import java.util.SortedSet;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A job that retrieves a collection of potential Staging Locations from a {@link
 * GcsDataflowProjectClient}.
 */
public class FetchStagingLocationsJob extends Job {
  private final GcsDataflowProjectClient gcsClient;

  private final String accountEmail;
  private final String cloudProjectId;
  private final SettableFuture<SortedSet<String>> stagingLocations;

  public FetchStagingLocationsJob(GcsDataflowProjectClient gcsClient, String accountEmail,
      String cloudProjectId) {
    super("Update Status Locations for project " + cloudProjectId);
    this.gcsClient = gcsClient;
    this.accountEmail = accountEmail;
    this.cloudProjectId = cloudProjectId;
    this.stagingLocations = SettableFuture.create();
  }

  public String getAccountEmail() {
    return accountEmail;
  }

  public String getProject() {
    return cloudProjectId;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      SortedSet<String> locations = gcsClient.getPotentialStagingLocations(cloudProjectId);
      stagingLocations.set(locations);
    } catch (IOException ex) {
      stagingLocations.setException(ex);
    }
    return Status.OK_STATUS;
  }

  public ListenableFuture<SortedSet<String>> getStagingLocations() {
    return stagingLocations;
  }
}
