package com.google.cloud.tools.eclipse.usagetracker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class AnalyticsPingManagerTest {

  private static final String RANDOM_UUID = "bee5d838-c3f8-4940-a944-b56973597e74";

  private static final String EVENT_TYPE = "some-event-type";
  private static final String EVENT_NAME = "some-event-name";

  private static final String VIRTUAL_DOCUMENT_PAGE =
      "/virtual/some-application/" + EVENT_TYPE + "/" + EVENT_NAME;

  private static final String METADATA_KEY = "some-custom-key";
  private static final String METADATA_VALUE = "some-custom-value";

  @SuppressWarnings("serial")
  private static final Map<String, String> RANDOM_PARAMETERS = Collections.unmodifiableMap(
      new HashMap<String, String>() {
        {
          put("v", "1");
          put("tid", "UA-12345678-1");
          put("ni", "0");
          put("t", "pageview");
          put("cd21", "1");
          put("cd16", "0");
          put("cd17", "0");
          put("cid", RANDOM_UUID);
          put("cd19", EVENT_TYPE);
          put("cd20", EVENT_NAME);
          put("dp", VIRTUAL_DOCUMENT_PAGE);
          put("dt", METADATA_KEY + "=" + METADATA_VALUE);
        }
      });

  @SuppressWarnings("serial")
  private static final Map<String, String> ENCODED_PARAMETERS = Collections.unmodifiableMap(
      new HashMap<String, String>() {
        {
          put("dt", "some-custom-key%3Dsome-custom-value");
          put("cd16", "0");
          put("cd17", "0");
          put("v", "1");
          put("t", "pageview");
          put("cd21", "1");
          put("cd20", "some-event-name");
          put("ni", "0");
          put("tid", "UA-12345678-1");
          put("dp", "%2Fvirtual%2Fsome-application%2Fsome-event-type%2Fsome-event-name");
          put("cid", "bee5d838-c3f8-4940-a944-b56973597e74");
          put("cd19", "some-event-type");
        }
      });

  @Test
  public void testGetParametersString() {
    String urlEncodedParameters = AnalyticsPingManager.getParametersString(RANDOM_PARAMETERS);

    String[] keyValuePairs = urlEncodedParameters.split("&");
    Assert.assertEquals(keyValuePairs.length, RANDOM_PARAMETERS.size());

    for (String pair : keyValuePairs) {
      String[] keyValue = pair.split("=");
      Assert.assertEquals(keyValue.length, 2);
      Assert.assertEquals(ENCODED_PARAMETERS.get(keyValue[0]), keyValue[1]);
    }
  }
}
