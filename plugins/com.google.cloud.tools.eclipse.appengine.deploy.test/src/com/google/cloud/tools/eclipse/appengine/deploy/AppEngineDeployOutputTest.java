/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.deploy;

import com.google.gson.JsonParseException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link AppEngineDeployOutput}
 */
public class AppEngineDeployOutputTest {
  @Test
  public void testDeployOutputJsonParsingOneVersion() {
    String jsonOutput =
        "{\n" +
            "  \"configs\": [],\n" +
            "  \"versions\": [\n" +
            "    {\n" +
            "      \"id\": \"20160429t112518\",\n" +
            "      \"last_deployed_time\": null,\n" +
            "      \"project\": \"some-project\",\n" +
            "      \"service\": \"default\",\n" +
            "      \"traffic_split\": null,\n" +
            "      \"version\": null\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    AppEngineDeployOutput deployOutput =
        AppEngineDeployOutput.parse(jsonOutput);
    Assert.assertEquals("20160429t112518", deployOutput.getVersion());
    Assert.assertEquals("default", deployOutput.getService());
  }

  @Test
  public void testDeployOutputJsonParsingTwoVersions() {
    String jsonOutput =
        "{\n" +
            "  \"configs\": [],\n" +
            "  \"versions\": [\n" +
            "    {\n" +
            "      \"id\": \"20160429t112518\",\n" +
            "      \"last_deployed_time\": null,\n" +
            "      \"project\": \"some-project\",\n" +
            "      \"service\": \"default\",\n" +
            "      \"traffic_split\": null,\n" +
            "      \"version\": null\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"20160429t112518\",\n" +
            "      \"last_deployed_time\": null,\n" +
            "      \"project\": \"some-project\",\n" +
            "      \"service\": \"default\",\n" +
            "      \"traffic_split\": null,\n" +
            "      \"version\": null\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    try {
      AppEngineDeployOutput.parse(jsonOutput);
      Assert.fail("Failure to throw exception when parsing deploy output with more that one version entry");
    } catch (JsonParseException e) {
      // Success! Should throw a JsonParseException.
    }
  }

  @Test
  public void testDeployOutputJsonParsingOldFormat() {
    String jsonOutput =
        "{\n" +
            "  \"default\": \"https://springboot-maven-project.appspot.com\"\n" +
            "}\n";

    try {
      AppEngineDeployOutput.parse(jsonOutput);
      Assert.fail("Failure to throw exception when parsing deploy output in old format");
    } catch (JsonParseException e) {
      // Success! Should throw a JsonParseException.
    }
  }
}
