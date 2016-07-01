package com.google.cloud.tools.eclipse.usagetracker;

import com.google.cloud.tools.eclipse.preferences.Activator;
import com.google.cloud.tools.eclipse.preferences.CloudToolsPreferencePage;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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

  private static AnalyticsPingManager instance;

  // Preference store (should be configuration scoped) from which we get UUID, opt-in status, etc.
  private IEclipsePreferences preferences;

  private OptInDialogCreator optInDialogCreator;

  @VisibleForTesting
  AnalyticsPingManager(
      IEclipsePreferences preferences, OptInDialogCreator optInDialogCreator) {
    this.preferences = preferences;
    this.optInDialogCreator = optInDialogCreator;
  }

  public static synchronized AnalyticsPingManager getInstance() {
    if (instance == null) {
      IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(PREFERENCES_PLUGIN_ID);
      if (preferences == null) {
        throw new NullPointerException("Preference store cannot be null.");
      }
      instance = new AnalyticsPingManager(preferences, new OptInDialogCreator());
    }
    return instance;
  }

  private static boolean isTrackingIdDefined() {
    return Constants.ANALYTICS_TRACKING_ID != null
        && Constants.ANALYTICS_TRACKING_ID.startsWith("UA-");
  }

  private String getAnonymizedClientId() {
    String clientId = preferences.get(CloudToolsPreferencePage.ANALYTICS_CLIENT_ID, null);
    if (clientId == null) {
      clientId = UUID.randomUUID().toString();
      preferences.put(CloudToolsPreferencePage.ANALYTICS_CLIENT_ID, clientId);
      flushPreferences();
    }
    return clientId;
  }

  private boolean userHasOptedIn() {
    return preferences.getBoolean(CloudToolsPreferencePage.ANALYTICS_OPT_IN, false);
  }

  /**
   * Returns true if a user has made a decision on the opt-in status via the opt-in dialog.
   * Currently, dismissal of the dialog without explicit opt-in/opt-out decision counts as
   * opting out. (Therefore, this method essentially returns true if the opt-in dialog was ever
   * presented before.)
   */
  private boolean userHasRegisteredOptInStatus() {
    return preferences.getBoolean(CloudToolsPreferencePage.ANALYTICS_OPT_IN_REGISTERED, false);
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
    preferences.putBoolean(CloudToolsPreferencePage.ANALYTICS_OPT_IN, optedIn);
    preferences.putBoolean(CloudToolsPreferencePage.ANALYTICS_OPT_IN_REGISTERED, true);
    flushPreferences();
  }

  /**
   * Sends a usage metric to Google Analytics.
   *
   * If the user has never seen the opt-in dialog or set the opt-in preference beforehand,
   * this method can potentially present an opt-in dialog at the top workbench level. If you
   * are calling this method inside another modal dialog, consider using {@link #sendPing(
   * String, String, String, Shell)} and pass the {@Shell} of currently open modal dialog.
   * (Otherwise, the opt-in dialog won't be able to get input until the workbench can get input.)
   */
  public void sendPing(String eventName, String metadataKey, String metadataValue) {
    sendPing(eventName, metadataKey, metadataValue, null);
  }

  public void sendPing(String eventName, String metadataKey, String metadataValue,
      Shell parentShell) {
    // Non-modal and non-blocking dialog (if presented). This implies that the very first
    // sendPing() may drop this event.
    showOptInDialog(parentShell);

    if (Platform.inDevelopmentMode() || !isTrackingIdDefined() || !userHasOptedIn()) {
      return;
    }

    Map<String, String> parametersMap =
        buildParametersMap(getAnonymizedClientId(), eventName, metadataKey, metadataValue);
    sendPostRequest(getParametersString(parametersMap));
  }

  private void sendPostRequest(String parametersString) {
    HttpURLConnection connection = null;

    try {
      URL url = new URL(ANALYTICS_COLLECTION_URL);
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setReadTimeout(3000);  // milliseconds
      byte[] bytesToWrite = parametersString.getBytes("UTF-8");
      connection.setFixedLengthStreamingMode(bytesToWrite.length);

      try (OutputStream out = connection.getOutputStream()) {
        out.write(bytesToWrite);
        out.flush();
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
      String clientId, String eventName, String metadataKey, String metadataValue) {
    Map<String, String> parametersMap = new HashMap<>(STANDARD_PARAMETERS);
    parametersMap.put("cid", clientId);
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

  /**
   * @param parentShell if null, tries to show the dialog at the workbench level.
   */
  public void showOptInDialog(Shell parentShell) {
    if (!userHasOptedIn() && !userHasRegisteredOptInStatus()) {
      optInDialogCreator.create(findShell(parentShell)).open();
    }
  }

  /**
   * May return null. (However, dialogs can have null as a parent shell.)
   */
  private Shell findShell(Shell parentShell) {
    if (parentShell != null) {
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
}
