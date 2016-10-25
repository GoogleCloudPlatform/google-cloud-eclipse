/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.util;

import org.junit.Assert;
import org.junit.Test;

public class CloudToolsInfoTest {

  @Test
  public void testGetToolsVersion() {
    Assert.assertTrue(CloudToolsInfo.getToolsVersion().startsWith("0.1.0."));
  }

  @Test
  public void testUserAgent() {
    Assert.assertTrue(CloudToolsInfo.USER_AGENT.startsWith("gcloud-eclipse-tools/0.1.0"));
  }
}
