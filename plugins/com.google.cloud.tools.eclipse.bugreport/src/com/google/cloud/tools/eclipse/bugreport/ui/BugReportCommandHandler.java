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

  private static final String BODY_TEMPLATE =
      "- Cloud Tools for Eclipse Version: {0}\n- OS: {1} {2}\n- Java Version: {3}\n\n";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Program.launch(formatReportUrl());
    return null;
  }

  @VisibleForTesting
  static String formatReportUrl() {
    String body = MessageFormat.format(BODY_TEMPLATE, CloudToolsInfo.getToolsVersion(),
        System.getProperty("os.name"), System.getProperty("os.version"),
        System.getProperty("java.version"));

    Escaper escaper = UrlEscapers.urlFormParameterEscaper();
    return BUG_REPORT_URL + "?body=" + escaper.escape(body);
  }
}
