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

import com.google.cloud.tools.eclipse.ui.status.Incident.Severity;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketException;
import java.net.URI;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
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
public class PollingStatusServiceImpl implements GcpStatusMonitoringService {
  /** Simple recurring job to refresh the status. */
  private class PollJob extends Job {
    public PollJob() {
      super(Messages.getString("poll.job.name")); //$NON-NLS-1$
      setSystem(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      refreshStatus();
      if (active) {
        // poll more frequently when having connection errors
        schedule(currentStatus.severity == Severity.ERROR ? errorPollTime : pollTime);
      }
      return Status.OK_STATUS;
    }
  };

  private static final Logger logger = Logger.getLogger(PollingStatusServiceImpl.class.getName());

  private static final URI STATUS_JSON_URI =
      URI.create("https://status.cloud.google.com/incidents.json"); //$NON-NLS-1$

  private Job pollingJob = new PollJob();


  /** Normally polls every 3 minutes. */
  private final long pollTime = TimeUnit.MINUTES.toMillis(3); // poll every 3 minutes

  /** Shorter poll when encountering connection errors. */
  private final long errorPollTime = TimeUnit.SECONDS.toMillis(30);

  private boolean active = false;
  private IProxyService proxyService;
  private ListenerList<Consumer<GcpStatusMonitoringService>> listeners = new ListenerList<>();
  private Gson gson = new Gson();

  private GcpStatus currentStatus = GcpStatus.OK_STATUS;

  @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
  public void setProxyService(IProxyService proxyService) {
    this.proxyService = proxyService;
  }

  public void unsetProxyService(IProxyService service) {
    if (proxyService == service) {
      proxyService = null;
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
  public GcpStatus getCurrentStatus() {
    return currentStatus;
  }

  void refreshStatus() {
    try {
      // As of 2018-01-30 the incidents log is 258k! But the incidents appear to be sorted from most
      // recent to the oldest.  Although fetching with gzip encoding reduces to 36k over the wire,
      // we can still do better by retrieving only the first 8k.
      URLConnection connection = STATUS_JSON_URI.toURL().openConnection(getProxy(STATUS_JSON_URI));
      connection.addRequestProperty("Range", "bytes=0-8192"); //$NON-NLS-1$ //$NON-NLS-2$
      try (InputStream input = connection.getInputStream()) {
        InputStreamReader streamReader = new InputStreamReader(input, StandardCharsets.UTF_8);
        Collection<Incident> activeIncidents = extractIncidentsInProgress(gson, streamReader);
        if (activeIncidents.isEmpty()) {
          currentStatus = GcpStatus.OK_STATUS;
        } else {
          Severity highestSeverity = Incident.getHighestSeverity(activeIncidents);
          Collection<String> affectedServices = Incident.getAffectedServiceNames(activeIncidents);
          currentStatus =
              new GcpStatus(highestSeverity, Joiner.on(", ").join(affectedServices), activeIncidents); //$NON-NLS-1$
        }
      }
    } catch (UnknownHostException | SocketException ex) {
      logger.log(Level.WARNING, "Cannot connect to GCP: " + ex); // $NON-NLS1$
      currentStatus =
          new GcpStatus(
              Severity.ERROR, Messages.getString("cannot.connect.to.gcp"), null); //$NON-NLS1$
    } catch (IOException ex) {
      // Could be a JSON error
      logger.log(Level.WARNING, "Failure retrieving GCP status", ex); // $NON-NLS1$
      currentStatus =
          new GcpStatus(
              Severity.ERROR, Messages.getString("failure.retrieving.status"), null); //$NON-NLS1$
    }
    for (Consumer<GcpStatusMonitoringService> listener : listeners) {
      listener.accept(this);
    }
  }

  /**
   * Process and accumulate the incidents from the input stream. As the input stream may be
   * incomplete (e.g., partial download), we ignore any JSON exceptions and {@link IOException}s
   * that may occur.
   */
  static Collection<Incident> extractIncidentsInProgress(Gson gson, Reader reader) {
    // Process the individual incident elements. An active incident has no {@code end} element.
    List<Incident> incidents = new LinkedList<>();
    try (JsonReader jsonReader = new JsonReader(reader)) {
      jsonReader.beginArray();
      while (jsonReader.hasNext()) {
        Incident incident = gson.fromJson(jsonReader, Incident.class);
        if (incident.end == null) {
          incidents.add(incident);
        }
      }
    } catch (JsonParseException | IOException ex) {
      // ignore this since we don't request all of the data
    }
    return incidents;
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
          logger.warning("Unknown proxy-data type: " + proxyData.getType()); //$NON-NLS-1$
          break;
      }
    }
    return Proxy.NO_PROXY;
  }

  @Override
  public void addStatusChangeListener(Consumer<GcpStatusMonitoringService> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeStatusChangeListener(Consumer<GcpStatusMonitoringService> listener) {
    listeners.remove(listener);
  }
}
