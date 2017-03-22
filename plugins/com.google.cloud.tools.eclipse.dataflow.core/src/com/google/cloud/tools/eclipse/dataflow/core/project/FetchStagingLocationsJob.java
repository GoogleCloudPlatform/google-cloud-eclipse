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
import java.util.SortedSet;

/**
 * A job that retrieves a collection of potential Staging Locations from a {@link
 * GcsDataflowProjectClient}.
 */
public class FetchStagingLocationsJob extends Job {
  private final GcsDataflowProjectClient gcsClient;

  private final String cloudProject;
  private final SettableFuture<SortedSet<String>> stagingLocations;

  private FetchStagingLocationsJob(GcsDataflowProjectClient gcsClient, String cloudProject) {
    super("Update Status Locations for project " + cloudProject);
    this.gcsClient = gcsClient;
    this.cloudProject = cloudProject;
    this.stagingLocations = SettableFuture.create();
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      stagingLocations.set(gcsClient.getPotentialStagingLocations(cloudProject));
    } catch (IOException e) {
      stagingLocations.setException(e);
    }
    return Status.OK_STATUS;
  }

  /**
   * Creates a new {@link FetchStagingLocationsJob} for the specified project using the specified
   * client, schedules the job, and returns a future containing the results of the job.
   */
  public static ListenableFutureProxy<SortedSet<String>> schedule(
      GcsDataflowProjectClient client, String project) {
    FetchStagingLocationsJob job = new FetchStagingLocationsJob(client, project);
    job.schedule();
    return new ListenableFutureProxy<>(job.stagingLocations);
  }
}
