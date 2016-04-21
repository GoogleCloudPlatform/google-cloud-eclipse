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
package com.google.cloud.tools.eclipse.appengine.localserver.server;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import com.google.cloud.tools.eclipse.appengine.localserver.server.ServerFlagsInfo.Flag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Holds the list of "dev_appserver.py" flags for the App Engine local dev server.
 */
public class CloudSdkServerFlags {
  private static final String FLAGS_FILE = "server-flags.json";

  private static List<Flag> serverFlags = null;

  /**
   * Returns an unmodifiable list of supported server flags
   */
  public static List<Flag> getFlags() {
    if (serverFlags == null) {
      try (Scanner scanner = new Scanner(CloudSdkServerFlags.class.getResourceAsStream(FLAGS_FILE))) {
        String content = scanner.useDelimiter("\\Z").next();
        Gson gson = new GsonBuilder().create();
        ServerFlagsInfo serverFlagsInfo = gson.fromJson(content, ServerFlagsInfo.class);
        serverFlags = Collections.unmodifiableList(serverFlagsInfo.getFlags());
      }
    }
    return serverFlags;
  }
}
