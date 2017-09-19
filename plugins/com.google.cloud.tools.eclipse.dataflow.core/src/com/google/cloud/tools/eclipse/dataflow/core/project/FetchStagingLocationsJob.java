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

import com.google.cloud.tools.eclipse.util.jobs.FuturisticJob;
import java.util.SortedSet;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A job that retrieves a collection of potential Staging Locations from a {@link
 * GcsDataflowProjectClient}.
 */
public class FetchStagingLocationsJob extends FuturisticJob<SortedSet<String>> {
  private final GcsDataflowProjectClient gcsClient;

  private final String accountEmail;
  private final String cloudProjectId;

  public FetchStagingLocationsJob(GcsDataflowProjectClient gcsClient, String accountEmail,
      String cloudProjectId) {
    super("Update staging locations for project " + cloudProjectId);
    this.gcsClient = gcsClient;
    this.accountEmail = accountEmail;
    this.cloudProjectId = cloudProjectId;
  }

  public String getAccountEmail() {
    return accountEmail;
  }

  public String getProjectId() {
    return cloudProjectId;
  }

  @Override
  protected SortedSet<String> compute(IProgressMonitor monitor) throws Exception {
    return gcsClient.getPotentialStagingLocations(cloudProjectId);
  }
}
