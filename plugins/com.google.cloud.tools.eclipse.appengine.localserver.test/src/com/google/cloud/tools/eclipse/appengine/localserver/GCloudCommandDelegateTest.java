/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.localserver;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;

import com.google.cloud.tools.eclipse.appengine.localserver.GCloudCommandDelegate;

/**
 * Unit tests for {@link GCloudCommandDelegate}
 */
public class GCloudCommandDelegateTest extends TestCase {
  public void testIsComponentInstalled() {
    String output1 =  createOutput("Installed");
    assertTrue(GCloudCommandDelegate.isComponentInstalled(output1,
        GCloudCommandDelegate.APP_ENGINE_COMPONENT_NAME));

    String output2 =  createOutput("Not Installed");
    assertFalse(GCloudCommandDelegate.isComponentInstalled(output2,
        GCloudCommandDelegate.APP_ENGINE_COMPONENT_NAME));

    String output3 =  createOutput("Update Available");
    assertTrue(GCloudCommandDelegate.isComponentInstalled(output3,
        GCloudCommandDelegate.APP_ENGINE_COMPONENT_NAME));

    assertFalse(GCloudCommandDelegate.isComponentInstalled(output1,
        "Invalid component name"));
  }

  public void testCreateAppRunCommand() throws IOException {
    File sdkLocationFile = File.createTempFile("tmp-cloud-sdk-", "");
    String sdkLocation = sdkLocationFile.getAbsolutePath();

    File runnablesFile = File.createTempFile("tmp-project-", "");
    String runnables = runnablesFile.getAbsolutePath();

    String retVal1 = "";
    try {
      retVal1 = GCloudCommandDelegate.createAppRunCommand(null, null, null, null, 0, 0);
    } catch (NullPointerException e) {
      // We're expecting the exception, so do nothing
    }
    assertEquals("", retVal1);

    String retVal2 = "";
    try {
      retVal2 = GCloudCommandDelegate.createAppRunCommand(sdkLocation, "fakeLocation", null, null,
          0, 0);
    } catch (InvalidPathException e) {
      // We're expecting the exception, so do nothing
    }
    assertEquals("", retVal2);

    String retVal3 = "";
    try {
      retVal3 = GCloudCommandDelegate.createAppRunCommand(sdkLocation, runnables, null, null, 0, 0);
    } catch (NullPointerException e) {
      // We're expecting the exception, so do nothing
    }
    assertEquals("", retVal3);

    String retVal4 =
        GCloudCommandDelegate.createAppRunCommand(sdkLocation, runnables, "run", "localhost", 1234, 2345);
    assertEquals(sdkLocation + "/bin/dev_appserver.py " + runnables + " --api_host localhost --api_port 1234",
        retVal4);

    String retVal5 =
        GCloudCommandDelegate.createAppRunCommand(sdkLocation, runnables, "debug", "localhost", 1234, 2345);
    assertEquals(sdkLocation + "/bin/dev_appserver.py " + runnables + " --api_host localhost --api_port 1234 "
        + "--jvm_flag=-Xdebug --jvm_flag=-Xrunjdwp:transport=dt_socket,server=y,suspend=y,"
        + "address=2345", retVal5);

    sdkLocationFile.delete();
    runnablesFile.delete();
  }

  private String createOutput(String status) {
    return "bq	Installed\n"
    	 + "gcloud	Installed\n"
    	 + "app-engine-java	" + status + "\n";
  }
}
