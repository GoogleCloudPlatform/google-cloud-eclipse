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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.servicemanagement.ServiceManagement;
import com.google.api.services.servicemanagement.ServiceManagement.Services.List;
import com.google.api.services.servicemanagement.model.ListServicesResponse;
import com.google.api.services.servicemanagement.model.ManagedService;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiUrl;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Request the list of services enabled on a GCP project. On success, the job will result in a list
 * of the <a href="https://cloud.google.com/service-management/overview>GCP Service Management
 * IDs</a> enabled for the GCP Project.
 * <p>
 * On error, the job will result in an error {@link IStatus} object with a meaningful exception:
 * <ul>
 * <li>HttpResponseException: on any non-2xx result</li>
 * <li>GoogleJsonResponseException: if the JSON is invalid (unexpected)</li>
 * </ul>
 * The {@link #getProjectServiceIDs()} future will also have its exception set.
 * <p>
 * This job is configured as a system job so that it can return a real {@link IStatus} object to
 * indicate success/failure without the error being reported to the user.
 * 
 * <pre>
 * $ gcloud service-management list --log-http --project foo
 * </pre>
 * 
 */
public class GcpProjectServicesJob extends Job {
  final private IGoogleApiFactory apiFactory;

  /** The GCP Project ID. */
  final private String projectId;

  /** The user credential for checks. */
  private Credential credential;

  private SettableFuture<Collection<String>> servicesFound;

  public GcpProjectServicesJob(IGoogleApiFactory apiFactory, Credential credential,
      String projectId) {
    super("Checking GCP project configuration");
    setSystem(true);
    this.apiFactory = apiFactory;
    this.credential = credential;
    this.projectId = projectId;

    this.servicesFound = SettableFuture.create();
  }

  /** Return the GCP Project ID to be checked. */
  public String getProjectId() {
    return projectId;
  }

  /** Get the user credential for check. */
  public Credential getCredential() {
    return credential;
  }

  /**
   * Return the list of enabled service-management API IDs on the project.
   * 
   * @see GoogleApiUrl
   */
  public Future<Collection<String>> getProjectServiceIDs() {
    return servicesFound;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    String projectId = this.projectId;
    ServiceManagement serviceManagement = apiFactory.newServiceManagementApi(credential);
    try {
      ListServicesResponse response = null;
      Collection<String> serviceIds = new ArrayList<>();
      do {
        if (monitor.isCanceled() || servicesFound.isCancelled()) {
          return Status.CANCEL_STATUS;
        }

        // We request only the serviceNames as the rest does not appear to be helpful.
        //@formatter:off
          List request = serviceManagement.services().list()
              .setFields("services/serviceName")
              .setConsumerId("project:" + projectId);
          //@formatter:on
        if (response != null && response.getNextPageToken() != null) {
          request.setPageToken(response.getNextPageToken());
        }
        response = request.execute();
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }

        for (ManagedService service : response.getServices()) {
          serviceIds.add(service.getServiceName());
        }
      } while (response.getNextPageToken() != null);

      servicesFound.set(ImmutableList.copyOf(serviceIds));
      return Status.OK_STATUS;

    } catch (GoogleJsonResponseException ex) {
      GoogleJsonError details = ex.getDetails();
      servicesFound.setException(ex);
      return StatusUtil.error(this, details.getMessage(), ex);
    } catch (IOException ex) {
      servicesFound.setException(ex);
      return StatusUtil.error(this, "Unable to request enabled services", ex);
    }
  }

}
