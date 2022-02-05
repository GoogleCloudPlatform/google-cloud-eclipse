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

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.operations.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import java.text.MessageFormat;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.program.Program;

public class BugReportCommandHandler extends AbstractHandler {

  private static final String BUG_REPORT_URL =
      "https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/new";

  // should be kept up to date with .github/ISSUE_TEMPLATE.md
  private static final String BODY_TEMPLATE =
      "<!--\n"
          + "Before reporting a possible bug:\n\n"
          + "1. Please ensure you are running the latest version of CT4E with _Help > Check for Updates_\n"
          + "2. If the problem occurs when you deploy or after the application has been deployed, "
          + "try deploying from the command line using gcloud or Maven. "
          + "If the problem does not go away, then the issue is likely "
          + "not with Cloud Tools for Eclipse.\n-->\n"
          + "- Cloud Tools for Eclipse version: {0}\n"
          + "- Google Cloud SDK version: {1} {2}\n"
          + "- Eclipse version: {3}\n"
          + "- OS: {4} {5}\n"
          + "- Java version: {6}\n"
          + "\n"
          + "**What did you do?**\n"
          + "\n"
          + "**What did you expect to see?**\n"
          + "\n"
          + "**What did you see instead?**\n"
          + "\n"
          + "<!-- Screenshots and stacktraces are helpful. -->";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Program.launch(formatReportUrl());
    return null;
  }

  @VisibleForTesting
  static String formatReportUrl() {
    String body = MessageFormat.format(BODY_TEMPLATE, CloudToolsInfo.getToolsVersion(),
        getCloudSdkVersion(), getCloudSdkManagementOption(),
        CloudToolsInfo.getEclipseVersion(),
        System.getProperty("os.name"), System.getProperty("os.version"),
        System.getProperty("java.version"));

    Escaper escaper = UrlEscapers.urlFormParameterEscaper();
    return BUG_REPORT_URL + "?body=" + escaper.escape(body);
  }

  private static Object getCloudSdkManagementOption() {
    return CloudSdkPreferences.isAutoManaging() ? "(auto-managed)" : "(non-managed)";
  }

  private static String getCloudSdkVersion() {
    try {
      CloudSdk sdk = new CloudSdk.Builder().build();
      return sdk.getVersion().toString();
    } catch (AppEngineException ex) {
      return ex.toString();
    }
  }
}
