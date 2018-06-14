/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.sdk.ui;

import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import org.junit.Assert;
import org.junit.Test;

public class MessagesTest {

  @Test
  public void testCloudSdkNotConfigured() {
    Assert.assertEquals("Choose SDK", Messages.getString("UseLocalSdk"));
  }

  @Test
  public void testCloudSdkInstallNotificationTitle() {
    Assert.assertEquals(
        "Google Cloud SDK Required", Messages.getString("CloudSdkInstallNotificationTitle"));
  }

  @Test
  public void testCloudSdkInstallNotificationMessage() {
    Assert.assertEquals(
        "Google Cloud Tools for Eclipse requires the"
            + " <a href=\"https://cloud.google.com/sdk/\">Google Cloud SDK</a> with the"
            + " <a href=\"https://cloud.google.com/sdk/docs/managing-components\">App Engine Java components</a>."
            + " Installation will begin momentarily, or <a href=\"skip\">skip for now</a>.",
        Messages.getString("CloudSdkInstallNotificationMessage"));
  }

  @Test
  public void testCloudSdkUpdateNotificationTitle() {
    Assert.assertEquals(
        "Google Cloud SDK Update Available", Messages.getString("CloudSdkUpdateNotificationTitle"));
  }

  @Test
  public void testCloudSdkUpdateNotificationMessage() {
    Assert.assertEquals(
        "<a href=\"update\">Update now</a> or from the Google Cloud Tools preference page."
            + " See the <a href=\"https://cloud.google.com/sdk/docs/release-notes\">release notes</a> for changes since 1.9.0.",
        Messages.getString("CloudSdkUpdateNotificationMessage", new CloudSdkVersion("1.9.0")));
  }
}
