package com.google.cloud.tools.eclipse.usagetracker;

import com.google.cloud.tools.eclipse.preferences.Activator;
import com.google.cloud.tools.eclipse.preferences.CloudToolsPreferencePage;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides methods that report plugin-specific events to Analytics.
 */
public class AnalyticsPingManager {

  private static final Logger logger = Logger.getLogger(AnalyticsPingManager.class.getName());

  private static final String PREFERENCES_PLUGIN_ID = Activator.PLUGIN_ID;

  private static final String ANALYTICS_COLLECTION_URL = "https://ssl.google-analytics.com/collect";

  // This name will be recorded as an originating app on Google Analytics.
  private static final String APPLICATION_NAME = "gcloud-eclipse-tools";

  // Fixed-value query parameters present in every ping, and their fixed values:
  //
  // For the semantics of each parameter, consult the following:
  //
  // https://github.com/google/cloud-reporting/blob/master/src/main/java/com/google/cloud/metrics/MetricsUtils.java#L183
  // https://developers.google.com/analytics/devguides/collection/protocol/v1/reference
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

  private static boolean isTrackingIdDefined() {
    return Constants.ANALYTICS_TRACKING_ID != null
        && Constants.ANALYTICS_TRACKING_ID.startsWith("UA-");
  }

  private static String getAnonymizedClientId() {
    String clientId = "0";  // For the extremely unlikely event of getNode() failure.

    IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(PREFERENCES_PLUGIN_ID);
    if (prefs != null) {
      clientId = prefs.get(CloudToolsPreferencePage.ANALYTICS_CLIENT_ID, null);
      if (clientId == null) {
        clientId = UUID.randomUUID().toString();
        prefs.put(CloudToolsPreferencePage.ANALYTICS_CLIENT_ID, clientId);
        flushPreferences(prefs);
      }
    }
    return clientId;
  }

  private static boolean hasUserOptedIn() {
    return getBooleanPreference(CloudToolsPreferencePage.ANALYTICS_OPT_IN, false);
  }

  /**
   * Returns true if a user has made a decision on the opt-in status via the opt-in dialog.
   * Currently, dismissal of the dialog without explicit opt-in/opt-out decision counts as
   * opting out. (Therefore, this method essentially returns true if the opt-in dialog was ever
   * presented before.)
   */
  private static boolean hasUserRegisteredOptInStatus() {
    return getBooleanPreference(CloudToolsPreferencePage.ANALYTICS_OPT_IN_REGISTERED, false);
  }

  static void registerOptInStatus(boolean optedIn) {
    IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(PREFERENCES_PLUGIN_ID);
    if (prefs != null) {
      prefs.putBoolean(CloudToolsPreferencePage.ANALYTICS_OPT_IN, optedIn);
      prefs.putBoolean(CloudToolsPreferencePage.ANALYTICS_OPT_IN_REGISTERED, true);
      flushPreferences(prefs);
    }
  }

  public static void sendPing(String eventName, String metadataKey, String metadataValue) {
    if (Platform.inDevelopmentMode() || !isTrackingIdDefined() || !hasUserOptedIn()) {
      return;
    }

    Map<String, String> parametersMap = buildParametersMap(eventName, metadataKey, metadataValue);
    sendPostRequest(getParametersString(parametersMap));
  }

  private static void sendPostRequest(String parametersString) {
    HttpURLConnection connection = null;

    try {
      URL url = new URL(ANALYTICS_COLLECTION_URL);
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Length", Integer.toString(parametersString.length()));

      try (Writer writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8")) {
        writer.write(parametersString);
        writer.flush();
      }
    } catch (IOException ex) {
      // Don't try to recover or retry.
      logger.log(Level.WARNING, "Failed to send a POST request", ex);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  @VisibleForTesting
  static Map<String, String> buildParametersMap(
      String eventName, String metadataKey, String metadataValue) {
    Map<String, String> parametersMap = new HashMap<>(STANDARD_PARAMETERS);
    parametersMap.put("cid", getAnonymizedClientId());
    parametersMap.put("cd19", APPLICATION_NAME);  // cd19: "event type"
    parametersMap.put("cd20", eventName);

    String virtualPageUrl = "/virtual/" + APPLICATION_NAME + "/" + eventName;
    parametersMap.put("dp", virtualPageUrl);
    parametersMap.put("dh", "virtual.eclipse");

    if (metadataKey != null) {
      // Event metadata are passed as a (virtual) page title.
      String virtualPageTitle = metadataKey + "=";
      if (metadataValue != null) {
        virtualPageTitle += metadataValue;
      } else {
        virtualPageTitle += "null";
      }

      parametersMap.put("dt", virtualPageTitle);
    }

    return parametersMap;
  }

  @VisibleForTesting
  static String getParametersString(Map<String, String> parametersMap) {
    StringBuilder resultBuilder = new StringBuilder();
    boolean ampersandNeeded = false;
    for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
      if (ampersandNeeded) {
        resultBuilder.append('&');
      } else {
        ampersandNeeded = true;
      }
      resultBuilder.append(entry.getKey());
      resultBuilder.append('=');
      try {
        resultBuilder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      } catch (UnsupportedEncodingException uee) {
        throw new RuntimeException("UTF-8 not supported");
      }
    }
    return resultBuilder.toString();
  }

  public static void showOptInDialog() {
    if (!hasUserOptedIn() && !hasUserRegisteredOptInStatus()) {
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window != null) {
        new OptInDialog(window.getShell()).open();
      } else {
        logger.log(Level.WARNING, "No active workbench window found.");
      }
    }
  }

  private static void flushPreferences(IEclipsePreferences prefs) {
    try {
      prefs.flush();
    } catch (BackingStoreException bse) {
      logger.log(Level.WARNING, bse.getMessage(), bse);
    }
  }

  private static boolean getBooleanPreference(String key, boolean defaultValue) {
    IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(PREFERENCES_PLUGIN_ID);
    if (prefs != null) {
      return prefs.getBoolean(key, defaultValue);
    }
    return defaultValue;
  }
}
