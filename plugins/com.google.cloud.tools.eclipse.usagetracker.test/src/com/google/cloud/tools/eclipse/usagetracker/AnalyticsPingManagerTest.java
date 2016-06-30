package com.google.cloud.tools.eclipse.usagetracker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class AnalyticsPingManagerTest {

  private static final String UUID = "bee5d838-c3f8-4940-a944-b56973597e74";

  private static final String EVENT_TYPE = "some-event-type";
  private static final String EVENT_NAME = "some-event-name";

  private static final String VIRTUAL_DOCUMENT_PAGE =
      "/virtual/some-application/" + EVENT_TYPE + "/" + EVENT_NAME;

  private static final String METADATA_KEY = "some-custom-key";
  private static final String METADATA_VALUE = "some-custom-value";

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
          put("cid", UUID);
          put("cd19", EVENT_TYPE);
          put("cd20", EVENT_NAME);
          put("dp", VIRTUAL_DOCUMENT_PAGE);
          put("dt", METADATA_KEY + "=" + METADATA_VALUE);
        }
      });

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
      Assert.assertEquals(2, keyValue.length);
      Assert.assertEquals(keyValue[1], ENCODED_PARAMETERS.get(keyValue[0]));
    }
  }

  @Test
  public void testGetParametersString_percentEscaping() {
    Map<String, String> noEscape = new HashMap<>();
    noEscape.put("k", ".*-_abcXYZ");
    Assert.assertEquals("k=.*-_abcXYZ", AnalyticsPingManager.getParametersString(noEscape));

    Map<String, String> escape = new HashMap<>();
    escape.put("k", " ü한글+=,`~!@#$%^&()?<>{}][|:;/\\'\"");
    Assert.assertEquals("k=+%C3%BC%ED%95%9C%EA%B8%80%2B%3D%2C%60%7E%21%40%23"
        + "%24%25%5E%26%28%29%3F%3C%3E%7B%7D%5D%5B%7C%3A%3B%2F%5C%27%22",
        AnalyticsPingManager.getParametersString(escape));
  }

  @Test
  public void testEventTypeEventNameConvention() {
    Map<String, String> parameters =
        AnalyticsPingManager.buildParametersMap("some.event-name", null, null);
    Assert.assertEquals("/virtual/gcloud-eclipse-tools/some.event-name",
        parameters.get("dp"));
  }

  @Test
  public void testMetadataConvention() {
    Map<String, String> parameters =
        AnalyticsPingManager.buildParametersMap("some.event-name", "times-happened", "1234");
    Assert.assertEquals("times-happened=1234", parameters.get("dt"));
  }
}
