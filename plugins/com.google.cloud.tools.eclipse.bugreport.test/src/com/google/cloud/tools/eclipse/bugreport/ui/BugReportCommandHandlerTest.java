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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    Pattern pattern = Pattern.compile("body="
        + "-\\+Cloud\\+Tools\\+for\\+Eclipse\\+Version%3A\\+(.*)%0A"
        + "-\\+OS%3A\\+(.*)%0A"
        + "-\\+Java\\+Version%3A\\+(.*)%0A%0A");
    Matcher matcher = pattern.matcher(url.getQuery());
    assertTrue(matcher.matches());
    String toolVersion = matcher.group(1);
    String os = matcher.group(2);
    String javaVersion = matcher.group(3);

    assertTrue(Pattern.compile("^\\d+\\.\\d+\\.\\d+").matcher(toolVersion).find());
    assertTrue(javaVersion.startsWith("1.7.") || javaVersion.startsWith("1.8."));
    assertTrue(os.startsWith("Linux") || os.startsWith("Mac") || os.startsWith("Windows"));
  }
}
