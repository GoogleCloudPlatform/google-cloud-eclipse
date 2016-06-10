package com.google.cloud.tools.eclipse.usagetracker;

import com.google.common.annotations.VisibleForTesting;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

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

/**
 * Provides methods that report plugin-specific events to Analytics.
 */
public class AnalyticsPingManager {

  public static final String ANALYTICS_PREFERENCE_QUALIFIER =
      AnalyticsPingManager.class.getPackage().getName();

  private static final String PREF_KEY_USER_CONSENT = "pref_key_user_consent";
  private static final String PREF_KEY_CLIENT_ID = "pref_key_client_id";

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

    IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(ANALYTICS_PREFERENCE_QUALIFIER);
    if (prefs != null) {
      clientId = prefs.get(PREF_KEY_CLIENT_ID, null);
      if (clientId == null) {
        // Use the current time in milliseconds as a client ID and store it back to the preferences.
        clientId = UUID.randomUUID().toString();
        prefs.put(PREF_KEY_CLIENT_ID, clientId);
      }
    }
    return clientId;
  }

  private static boolean hasUserOptedIn() {
    IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(ANALYTICS_PREFERENCE_QUALIFIER);
    if (prefs != null) {
      return prefs.getBoolean(PREF_KEY_USER_CONSENT, false);
    }
    return false;
  }

  public static void sendPing(String eventType, String eventName,
      String metadataKey, String metadataValue) {
    if (Platform.inDevelopmentMode() || !isTrackingIdDefined() || !hasUserOptedIn()) {
      return;
    }

    Map<String, String> parametersMap = new HashMap<>(STANDARD_PARAMETERS);
    parametersMap.put("cid", getAnonymizedClientId());
    parametersMap.put("cd19", eventType);
    parametersMap.put("cd20", eventName);

    String virtualPageUrl = "/virtual/" + APPLICATION_NAME + "/" + eventType + "/" + eventName;
    parametersMap.put("dp", virtualPageUrl);

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
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
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
}
