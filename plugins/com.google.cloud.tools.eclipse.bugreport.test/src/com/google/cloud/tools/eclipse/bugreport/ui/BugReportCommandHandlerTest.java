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

package com.google.cloud.tools.eclipse.bugreport.ui;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class BugReportCommandHandlerTest {

  @Test
  public void formatReportUrl() throws MalformedURLException {
    URL url = new URL(BugReportCommandHandler.formatReportUrl());

    assertEquals("https", url.getProtocol());
    assertEquals("github.com", url.getHost());
    assertEquals("/GoogleCloudPlatform/google-cloud-eclipse/issues/new", url.getPath());

    //@formatter:off
    // check that values are properly filled in
    Pattern pattern = Pattern.compile("body="
        + "%28please\\+ensure\\+you\\+are\\+running\\+the\\+latest\\+version\\+of\\+CT4E\\+with\\+_Help\\+%3E\\+Check\\+for\\+Updates_%29%0A"
        + "-\\+Cloud\\+Tools\\+for\\+Eclipse\\+version%3A\\+(?<toolVersion>.*)%0A"
        + "-\\+Google\\+Cloud\\+SDK\\+version%3A\\+(?<gcloudVersion>.*)%0A"
        + "-\\+Eclipse\\+version%3A\\+(?<eclipseVersion>.*)%0A"
        + "-\\+OS%3A\\+(?<os>.*)%0A"
        + "-\\+Java\\+version%3A\\+(?<javaVersion>.*)%0A%0A"
        + "\\*\\*What\\+did\\+you\\+do%3F\\*\\*%0A%0A" 
        + "\\*\\*What\\+did\\+you\\+expect\\+to\\+see%3F\\*\\*%0A%0A"
        + "\\*\\*What\\+did\\+you\\+see\\+instead%3F\\*\\*%0A%0A" 
        + "%28screenshots\\+are\\+helpful%29");
    //@formatter:on
    Matcher matcher = pattern.matcher(url.getQuery());
    assertTrue(matcher.matches());
    String toolVersion = matcher.group("toolVersion");
    String gcloudVersion = matcher.group("gcloudVersion");
    String eclipseVersion = matcher.group("eclipseVersion");
    String os = matcher.group("os");
    String javaVersion = matcher.group("javaVersion");

    assertTrue(Pattern.compile("^\\d+\\.\\d+\\.\\d+").matcher(toolVersion).find());
    assertThat(javaVersion, anyOf(startsWith("1.7."), startsWith("1.8.")));
    assertThat(os, anyOf(startsWith("Linux"), startsWith("Mac"), startsWith("Windows")));
    assertTrue(Pattern.compile("^\\d+\\.\\d+").matcher(eclipseVersion).find());
    new CloudSdkVersion(gcloudVersion); // throws IllegalArgumentException if invalid
  }
}
