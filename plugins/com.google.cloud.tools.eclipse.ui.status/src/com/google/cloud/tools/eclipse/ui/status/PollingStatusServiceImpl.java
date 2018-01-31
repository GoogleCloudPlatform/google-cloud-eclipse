/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.ui.status;

import com.google.cloud.tools.eclipse.util.jobs.Consumer;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * An implementation that polls the Google Cloud Platform's status page. The Google Cloud Platform
 * status page provides a incident log in JSON, which appears to be ordered from most recent to
 * oldest. We fetch the first N bytes and process the incidents listed. Incidents that are still
 * on-going do not have an "end".
 */
@Component(name = "polling")
public class PollingStatusServiceImpl implements GcpStatusService {
  private static final Logger logger = Logger.getLogger(PollingStatusServiceImpl.class.getName());

  private static final URI STATUS_JSON_URI =
      URI.create("https://status.cloud.google.com/incidents.json");

  private Job pollingJob =
      new Job("Retrieving Google Cloud Platform status") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          logger.info("Starting GCP status refresh");
          refreshStatus();
          if (active) {
            logger.info("Still active: rescheduling");
            schedule(pollTime);
          }
          return Status.OK_STATUS;
        }
      };

  private boolean active = false;
  private long pollTime = 3 * 60 * 1000; // poll every 3 minutes
  private IProxyService proxyService;
  private ListenerList /*<Consumer<GcpStatusService>>*/ listeners = new ListenerList /*<>*/();
  private Gson gson = new Gson();

  private IStatus currentStatus = Status.OK_STATUS;

  @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
  public void setProxyService(IProxyService proxyService) {
    this.proxyService = proxyService;
  }

  public void unsetProxyService(IProxyService proxyService) {
    if (this.proxyService == proxyService) {
      this.proxyService = null;
    }
  }

  @Activate
  public void start() {
    active = true;
    pollingJob.schedule();
  }

  @Deactivate
  public void stop() {
    active = false;
    pollingJob.cancel();
  }

  @Override
  public IStatus getCurrentStatus() {
    return currentStatus;
  }

  void refreshStatus() {
    try {
      URLConnection connection = STATUS_JSON_URI.toURL().openConnection(getProxy(STATUS_JSON_URI));
      // the incidents log is 270k as of 2018-01-30!
      connection.addRequestProperty("Range", "bytes=0-8192");
      try (InputStream input = connection.getInputStream()) {
        InputStreamReader streamReader = new InputStreamReader(input, StandardCharsets.UTF_8);
        currentStatus = processIncidents(gson, streamReader);
      }
    } catch (IOException ex) {
      currentStatus = StatusUtil.error(this, ex.toString(), ex);
    }
    logger.info("current GCP status = " + currentStatus);
    for (Object listener : listeners.getListeners()) {
      ((Consumer<GcpStatusService>) listener).accept(this);
    }
  }

  /**
   * Process and accumulate the incidents from the input stream. As the the input stream may be
   * incomplete (e.g., partial download), we ignore {@link IOException}s that may occur.
   */
  static IStatus processIncidents(Gson gson, Reader reader) {
    // Process the individual incident elements. These are sorted from most recent to the
    // earliest. Active incidents no {@code end} element.
    MultiStatus status =
        StatusUtil.multi(PollingStatusServiceImpl.class, "Google Cloud Platform status");
    try {
      JsonReader jsonReader = new JsonReader(reader);
      jsonReader.beginArray();
      while (jsonReader.hasNext()) {
        JsonObject incident = gson.fromJson(jsonReader, JsonObject.class);
        if (!incident.has("end")) {
          status.merge(toStatus(incident));
        }
      }
    } catch (JsonParseException | IOException ex) {
      // ignore this since we don't request all of the data
    }
    return StatusUtil.filter(status);
  }

  /** Encode a Google Cloud Status incident JSON object into a status object. */
  @VisibleForTesting
  static IStatus toStatus(JsonObject incident) {
    int id = incident.get("number").getAsInt();
    String serviceKey = incident.get("service_key").getAsString();
    String serviceName = incident.get("service_name").getAsString();
    String externalDescription = incident.get("external_desc").getAsString();
    String message = String.format("Incident %d [%s]: %s", id, serviceName, externalDescription);

    String severity = incident.get("severity").getAsString();
    switch (severity) {
      case "low":
        return new Status(IStatus.INFO, serviceKey, id, message, null);
      case "medium":
        return new Status(IStatus.WARNING, serviceKey, id, message, null);
      case "high":
      default:
        return new Status(IStatus.ERROR, serviceKey, id, message, null);
    }
  }

  private Proxy getProxy(URI uri) {
    if (proxyService == null) {
      return Proxy.NO_PROXY;
    }
    IProxyData[] proxies = proxyService.select(uri);
    for (IProxyData proxyData : proxies) {
      switch (proxyData.getType()) {
        case IProxyData.HTTPS_PROXY_TYPE:
        case IProxyData.HTTP_PROXY_TYPE:
          return new Proxy(
              Type.HTTP, new InetSocketAddress(proxyData.getHost(), proxyData.getPort()));
        case IProxyData.SOCKS_PROXY_TYPE:
          return new Proxy(
              Type.SOCKS, new InetSocketAddress(proxyData.getHost(), proxyData.getPort()));
        default:
          logger.warning("Unknown proxy-data type: " + proxyData.getType());
          break;
      }
    }
    return Proxy.NO_PROXY;
  }

  @Override
  public void addStatusChangeListener(Consumer<GcpStatusService> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeStatusChangeListener(Consumer<GcpStatusService> listener) {
    listeners.remove(listener);
  }
}
