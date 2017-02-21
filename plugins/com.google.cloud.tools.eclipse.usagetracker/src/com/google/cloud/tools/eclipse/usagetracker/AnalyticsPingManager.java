/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.usagetracker;

import com.google.cloud.tools.eclipse.preferences.Activator;
import com.google.cloud.tools.eclipse.preferences.AnalyticsPreferences;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.io.HttpUtil;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Provides methods that report plugin-specific events to Analytics.
 */
public class AnalyticsPingManager {

  private static final Logger logger = Logger.getLogger(AnalyticsPingManager.class.getName());

  private static final String PREFERENCES_PLUGIN_ID = Activator.PLUGIN_ID;

  private static final String ANALYTICS_COLLECTION_URL = "https://ssl.google-analytics.com/collect";

  // Fixed-value query parameters present in every ping, and their fixed values:
  //
  // For the semantics of each parameter, consult the following:
  //
  // https://github.com/google/cloud-reporting/blob/master/src/main/java/com/google/cloud/metrics/MetricsUtils.java#L183
  // https://developers.google.com/analytics/devguides/collection/protocol/v1/reference
  @SuppressWarnings("serial")
  private static final Map<String, String> STANDARD_PARAMETERS = Collections.unmodifiableMap(
      new HashMap<String, String>() {
        {
          put("v", "1");  // Google Analytics Measurement Protocol version
          put("tid", Constants.ANALYTICS_TRACKING_ID);  // tracking ID
          put("ni", "0");  // Non-interactive? Report as interactive.
          put("t", "pageview");  // Hit type
          // "cd??" are for Custom Dimensions in GA.
          put("cd21", "1");  // Yes, this ping is a virtual "page".
          put("cd16", "0");  // Internal user? No.
          put("cd17", "0");  // User signed in? We will ignore this.
        }
      });

  private static AnalyticsPingManager instance;

  // Preference store (should be configuration scoped) from which we get UUID, opt-in status, etc.
  private IEclipsePreferences preferences;
  private Display display;
  private boolean analyticsEnabled;

  private ConcurrentLinkedQueue<PingEvent> pingEventQueue;
  private Job eventFlushJob = new Job("Analytics Event Submission") {
    @Override
    protected IStatus run(IProgressMonitor monitor) {
      while (!pingEventQueue.isEmpty()) {
        PingEvent event = pingEventQueue.poll();
        showOptInDialogIfNeeded(event.shell);
        sendPingHelper(event);
      }
      return Status.OK_STATUS;
    }
  };

  @VisibleForTesting
  AnalyticsPingManager(IEclipsePreferences preferences, Display display,
      ConcurrentLinkedQueue<PingEvent> concurrentLinkedQueue, boolean analyticsEnabled) {
    this.preferences = preferences;
    this.display = display;
    this.pingEventQueue = concurrentLinkedQueue;
    this.analyticsEnabled = analyticsEnabled;
  }

  public static synchronized AnalyticsPingManager getInstance() {
    if (instance == null) {
      IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(PREFERENCES_PLUGIN_ID);
      if (preferences == null) {
        throw new NullPointerException("Preference store cannot be null.");
      }

      boolean analyticsEnabled = !Platform.inDevelopmentMode() && isTrackingIdDefined();
      Display display = !analyticsEnabled ? null : PlatformUI.getWorkbench().getDisplay();

      instance = new AnalyticsPingManager(preferences, display,
          new ConcurrentLinkedQueue<PingEvent>(), analyticsEnabled);
    }
    return instance;
  }

  private static boolean isTrackingIdDefined() {
    return Constants.ANALYTICS_TRACKING_ID != null
        && Constants.ANALYTICS_TRACKING_ID.startsWith("UA-");
  }

  private String getAnonymizedClientId() {
    String clientId = preferences.get(AnalyticsPreferences.ANALYTICS_CLIENT_ID, null);
    if (clientId == null) {
      clientId = UUID.randomUUID().toString();
      preferences.put(AnalyticsPreferences.ANALYTICS_CLIENT_ID, clientId);
      flushPreferences();
    }
    return clientId;
  }

  private boolean userHasOptedIn() {
    return preferences.getBoolean(AnalyticsPreferences.ANALYTICS_OPT_IN, false);
  }

  /**
   * Returns true if a user has made a decision on the opt-in status via the opt-in dialog.
   * Currently, dismissal of the dialog without explicit opt-in/opt-out decision counts as
   * opting out. (Therefore, this method essentially returns true if the opt-in dialog was ever
   * presented before.)
   */
  private boolean userHasRegisteredOptInStatus() {
    return preferences.getBoolean(AnalyticsPreferences.ANALYTICS_OPT_IN_REGISTERED, false);
  }

  /**
   * "Registered" here means a user made a decision through the opt-in dialog. (That is, it
   * essentially means if the opt-in dialog was ever presented in our current setting.) Note
   * that it is possible that a user may have already opted in/out explicitly through the
   * preference page without ever seeing the opt-in dialog. It would have been ideal if we
   * defined a three-valued settings (e.g., OPTED_IN | OPTED_OUT | UNDEF). However, this would
   * require implementing a custom UI editor field class for the preference page, so here we
   * take a simple approach to use two Boolean settings.
   */
  void registerOptInStatus(boolean optedIn) {
    preferences.putBoolean(AnalyticsPreferences.ANALYTICS_OPT_IN, optedIn);
    preferences.putBoolean(AnalyticsPreferences.ANALYTICS_OPT_IN_REGISTERED, true);
    flushPreferences();
  }

  /**
   * Sends a usage metric to Google Analytics.
   *
   * If the user has never seen the opt-in dialog or set the opt-in preference beforehand,
   * this method can potentially present an opt-in dialog at the top workbench level. If you
   * are calling this method inside another modal dialog, consider using {@link #sendPing(
   * String, String, String, Shell)} and pass the {@Shell} of the currently open modal dialog.
   * (Otherwise, the opt-in dialog won't be able to get input until the workbench can get input.)
   *
   * Safe to call from non-UI contexts.
   */
  public void sendPing(String eventName, String metadataKey, String metadataValue) {
    sendPing(eventName, metadataKey, metadataValue, null);
  }

  @VisibleForTesting
  boolean unitTestMode;
  public void sendPing(String eventName,
      String metadataKey, String metadataValue, Shell parentShell) {
    if (analyticsEnabled || unitTestMode) {
      // Note: always enqueue if a user has not seen the opt-in dialog yet; enqueuing itself
      // doesn't mean that the event ping will be posted.
      if (userHasOptedIn() || !userHasRegisteredOptInStatus()) {
        pingEventQueue.add(new PingEvent(eventName, metadataKey, metadataValue, parentShell));
        eventFlushJob.schedule();
      }
    }
  }

  private void sendPingHelper(PingEvent pingEvent) {
    if (analyticsEnabled && userHasOptedIn()) {
      try {
        Map<String, String> parametersMap = buildParametersMap(getAnonymizedClientId(), pingEvent);
        HttpUtil.sendPost(ANALYTICS_COLLECTION_URL, parametersMap);
      } catch (IOException ex) {
        // Don't try to recover or retry.
        logger.log(Level.WARNING, "Failed to send a POST request", ex);
      }
    }
  }

  @VisibleForTesting
  static Map<String, String> buildParametersMap(String clientId, PingEvent pingEvent) {
    Map<String, String> parametersMap = new HashMap<>(STANDARD_PARAMETERS);
    parametersMap.put("cid", clientId);
    parametersMap.put("cd19", CloudToolsInfo.METRICS_NAME);  // cd19: "event type"
    parametersMap.put("cd20", pingEvent.eventName);

    String virtualPageUrl = "/virtual/" + CloudToolsInfo.METRICS_NAME + "/" + pingEvent.eventName;
    parametersMap.put("dp", virtualPageUrl);
    parametersMap.put("dh", "virtual.eclipse");

    if (pingEvent.metadataKey != null) {
      // Event metadata are passed as a (virtual) page title.
      String virtualPageTitle = pingEvent.metadataKey + "=";
      if (pingEvent.metadataValue != null) {
        virtualPageTitle += pingEvent.metadataValue;
      } else {
        virtualPageTitle += "null";
      }

      parametersMap.put("dt", virtualPageTitle);
    }

    return parametersMap;
  }

  // To prevent showing multiple opt-in dialogs. Assumes that once a dialog is opened,
  // implicit closure is considered opting out.
  private AtomicBoolean optInDialogOpened = new AtomicBoolean(false);

  /**
   * @param parentShell if null, tries to show the dialog at the workbench level.
   */
  public void showOptInDialogIfNeeded(final Shell parentShell) {
    if (!userHasOptedIn() && !userHasRegisteredOptInStatus()) {
      if (optInDialogOpened.compareAndSet(false, true)) {
        display.syncExec(new Runnable() {
          @Override
          public void run() {
            new OptInDialog(findShell(parentShell)).open();
          }
        });
      }
    }
  }

  /**
   * May return null. (However, dialogs can have null as a parent shell.)
   */
  private Shell findShell(Shell parentShell) {
    if (parentShell != null && !parentShell.isDisposed()) {
      return parentShell;
    }

    try {
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window != null) {
        return window.getShell();
      }
    } catch (IllegalStateException ise) {  // getWorkbench() might throw this.
      // Fall through.
    }

    Display display = Display.getCurrent();
    return display != null ? display.getActiveShell() : null;
  }

  private void flushPreferences() {
    try {
      preferences.flush();
    } catch (BackingStoreException bse) {
      logger.log(Level.WARNING, bse.getMessage(), bse);
    }
  }

  @VisibleForTesting
  static class PingEvent {
    private String eventName;
    private String metadataKey;
    private String metadataValue;
    private Shell shell;

    public PingEvent(String eventName, String metadataKey, String metadataValue, Shell shell) {
      this.eventName = eventName;
      this.metadataKey = metadataKey;
      this.metadataValue = metadataValue;
      this.shell = shell;
    }
  }
}
