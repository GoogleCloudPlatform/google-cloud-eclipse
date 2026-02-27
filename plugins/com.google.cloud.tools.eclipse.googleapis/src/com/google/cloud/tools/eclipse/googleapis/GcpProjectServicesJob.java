/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.googleapis;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.servicemanagement.ServiceManagement;
import com.google.api.services.servicemanagement.ServiceManagement.Services;
import com.google.api.services.servicemanagement.model.ListServicesResponse;
import com.google.api.services.servicemanagement.model.ManagedService;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.util.jobs.FuturisticJob;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Request the list of services enabled on a GCP project. On success, the {@link #getFuture() job's
 * future} will be set to the list of the <a
 * href="https://cloud.google.com/service-management/overview>GCP Service Management IDs</a> enabled
 * for the GCP Project.
 * <p>
 * On request error, the computation error will be an {@link HttpResponseException}, and most likely
 * a {@link GoogleJsonResponseException}.
 * <p>
 * Equivalent command-line:
 * 
 * <pre>
 * $ gcloud service-management list --log-http --project foo
 * </pre>
 */
public class GcpProjectServicesJob extends FuturisticJob<List<String>> {

  /** The GCP Project ID to check. */
  private final String projectId;

  public GcpProjectServicesJob(String projectId) {
    super("Checking GCP project configuration");
    this.projectId = projectId;
  }

  /** Return the GCP Project ID to be checked. */
  public String getProjectId() {
    return projectId;
  }

  @Override
  protected List<String> compute(IProgressMonitor monitor)
      throws GoogleJsonResponseException, IOException {
    String originalProjectId = this.projectId;
    ServiceManagement serviceManagement = GoogleApiFactory.INSTANCE.newServiceManagementApi();
    ListServicesResponse response = null;
    Collection<String> serviceIds = new ArrayList<>();
    do {
      checkCancelled(monitor);
      // We request only the serviceNames as the rest does not appear to be helpful.
      //@formatter:off
      Services.List request = serviceManagement.services().list()
          .setFields("services/serviceName")
          .setConsumerId("project:" + originalProjectId);
      //@formatter:on
      if (response != null && response.getNextPageToken() != null) {
        request.setPageToken(response.getNextPageToken());
      }
      response = request.execute();

      if (response.getServices() != null) {
        for (ManagedService service : response.getServices()) {
          serviceIds.add(service.getServiceName());
        }
      }
    } while (response.getNextPageToken() != null);

    return ImmutableList.copyOf(serviceIds);
  }

}
