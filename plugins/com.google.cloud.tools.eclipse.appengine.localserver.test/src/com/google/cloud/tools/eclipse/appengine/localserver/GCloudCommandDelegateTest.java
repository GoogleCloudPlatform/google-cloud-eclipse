/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.localserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;

/**
 * Unit tests for {@link GCloudCommandDelegate}
 */
public class GCloudCommandDelegateTest {

  @Test
  public void testIsComponentInstalled_installed() {
    String output = createOutput("Installed");
    assertTrue(GCloudCommandDelegate.isComponentInstalled(output,
                                                          GCloudCommandDelegate.APP_ENGINE_COMPONENT_NAME));
  }

  @Test
  public void testIsComponentInstalled_notInstalled() {
    String output = createOutput("Not Installed");
    assertFalse(GCloudCommandDelegate.isComponentInstalled(output,
                                                           GCloudCommandDelegate.APP_ENGINE_COMPONENT_NAME));
  }

  @Test
  public void testIsComponent_updateAvailable() {
    String output = createOutput("Update Available");
    assertTrue(GCloudCommandDelegate.isComponentInstalled(output,
                                                          GCloudCommandDelegate.APP_ENGINE_COMPONENT_NAME));
  }

  @Test
  public void testIsComponent_invalidComponent() {
    String output = createOutput("Installed");
    assertFalse(GCloudCommandDelegate.isComponentInstalled(output, "invalid-component-name"));
  }

  @Test(expected = NullPointerException.class)
  public void testCreateAppRunCommand_nullArgs() throws IOException {
    GCloudCommandDelegate.createAppRunCommand(null, (String) null, null, null, 0, 0);
  }

  @Test(expected = InvalidPathException.class)
  public void testCreateAppRunCommand_invalidSdkLocation() throws IOException {
    GCloudCommandDelegate.createAppRunCommand("sdkLocation", (String) null, null, null, 0, 0);
  }

  @Test(expected = InvalidPathException.class)
  public void testCreateAppRunCommand_invalidLocation() throws IOException {
    File sdkLocationFile = createTmpFile("tmp-cloud-sdk-", "");
    String sdkLocation = sdkLocationFile.getAbsolutePath();

    GCloudCommandDelegate.createAppRunCommand(sdkLocation, "fakeLocation", null, null, 0, 0);
  }

  @Test(expected = NullPointerException.class)
  public void testCreateAppRunCommand_nullApiHost() throws IOException {
    File sdkLocationFile = createTmpFile("tmp-cloud-sdk-", "");
    String sdkLocation = sdkLocationFile.getAbsolutePath();

    File runnablesFile = createTmpFile("tmp-project-", "");
    String runnables = runnablesFile.getAbsolutePath();

    GCloudCommandDelegate.createAppRunCommand(sdkLocation, runnables, null, null, 0, 0);
  }

  @Test
  public void testCreateAppRunCommand_validRunCommand() throws IOException {
    File sdkLocationFile = createTmpFile("tmp-cloud-sdk-", "");
    String sdkLocation = sdkLocationFile.getAbsolutePath();

    File runnablesFile = createTmpFile("tmp-project-", "");
    String runnables = runnablesFile.getAbsolutePath();

    String cmd = GCloudCommandDelegate.createAppRunCommand(sdkLocation,
                                                           runnables,
                                                           "run",
                                                           "localhost",
                                                           1234,
                                                           2345);

    assertEquals(sdkLocation + "/bin/dev_appserver.py "
                 + runnables
                 + " --api_host localhost --api_port 1234",
                 cmd);
  }

  @Test
  public void testCreateAppRunCommand_validDebugCommand() throws IOException {
    File sdkLocationFile = createTmpFile("tmp-cloud-sdk-", "");
    String sdkLocation = sdkLocationFile.getAbsolutePath();

    File runnablesFile = createTmpFile("tmp-project-", "");
    String runnables = runnablesFile.getAbsolutePath();

    String cmd = GCloudCommandDelegate.createAppRunCommand(sdkLocation,
                                                           runnables,
                                                           "debug",
                                                           "localhost",
                                                           1234,
                                                           2345);

    assertEquals(sdkLocation + "/bin/dev_appserver.py " + runnables
        + " --api_host localhost --api_port 1234 "
        + "--jvm_flag=-Xdebug --jvm_flag=-Xrunjdwp:transport=dt_socket,server=y,suspend=y,quiet=y,"
        + "address=2345", cmd);
  }

  private String createOutput(String status) {
    return "bq	Installed\n" + "gcloud	Installed\n" + "app-engine-java	" + status + "\n";
  }

  private File createTmpFile(String prefix, String suffix) throws IOException {
    File sdkLocationFile = File.createTempFile(prefix, suffix);
    sdkLocationFile.deleteOnExit();
    return sdkLocationFile;
  }
}
