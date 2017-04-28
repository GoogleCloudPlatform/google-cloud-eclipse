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

import org.junit.Assert;
import org.junit.Test;

public class DeployJobTest {

  @Test
  public void testGetDeployedAppUrl_internal() {
    AppEngineDeployOutput deployOutput =
        createDeployOutput("google.com:notable-torch", "version", "default");

    Assert.assertEquals("https://notable-torch.googleplex.com",
        DeployJob.getDeployedAppUrl(true /* promoted */, deployOutput));
  }

  @Test
  public void testGetDeployedAppUrl_withPartition() {
    AppEngineDeployOutput deployOutput =
        createDeployOutput("s~google.com:notable-torch", "version", "default");

    Assert.assertEquals("https://notable-torch.googleplex.com",
        DeployJob.getDeployedAppUrl(true /* promoted */, deployOutput));
  }

  @Test
  public void testGetDeployedAppUrl_promoteWithDefaultService() {
    AppEngineDeployOutput deployOutput = createDeployOutput("testProject", "version", "default");

    Assert.assertEquals("https://testProject.appspot.com",
        DeployJob.getDeployedAppUrl(true /* promoted */, deployOutput));
  }

  @Test
  public void testGetDeployedAppUrl_promoteWithNonDefaultService() {
    AppEngineDeployOutput deployOutput = createDeployOutput("testProject", "version", "service");

    Assert.assertEquals("https://service-dot-testProject.appspot.com",
        DeployJob.getDeployedAppUrl(true /* promoted */, deployOutput));
  }

  @Test
  public void testGetDeployedAppUrl_noPromoteWithDefaultService() {
    AppEngineDeployOutput deployOutput = createDeployOutput("testProject", "version", "default");

    Assert.assertEquals("https://version-dot-testProject.appspot.com",
        DeployJob.getDeployedAppUrl(false /* promoted */, deployOutput));
  }

  @Test
  public void testGetDeployedAppUrl_noPromoteWithNonDefaultService() {
    AppEngineDeployOutput deployOutput = createDeployOutput("testProject", "version", "service");

    Assert.assertEquals("https://version-dot-service-dot-testProject.appspot.com",
        DeployJob.getDeployedAppUrl(false /* promoted */, deployOutput));
  }

  private static AppEngineDeployOutput createDeployOutput(String project, String version,
      String service) {
    String jsonOutput =
        "{\n" +
        "  \"configs\": [],\n" +
        "  \"versions\": [\n" +
        "    {\n" +
        "      \"id\": \"" + version + "\",\n" +
        "      \"last_deployed_time\": null,\n" +
        "      \"project\": \"" + project + "\",\n" +
        "      \"service\": \"" + service + "\",\n" +
        "      \"traffic_split\": null,\n" +
        "      \"version\": null\n" +
        "    }\n" +
        "  ]\n" +
        "}\n";
    return AppEngineDeployOutput.parse(jsonOutput);
  }
}
