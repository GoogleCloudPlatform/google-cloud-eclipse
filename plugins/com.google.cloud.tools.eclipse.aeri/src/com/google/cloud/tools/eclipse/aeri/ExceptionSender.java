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

package com.google.cloud.tools.eclipse.aeri;

import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.io.HttpUtil;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.epp.logging.aeri.core.IStackTraceElement;
import org.eclipse.epp.logging.aeri.core.IThrowable;

public class ExceptionSender {

  private static final Logger logger = Logger.getLogger(ExceptionSender.class.getName());

  private static final String REPORT_URL = "https://clients2.google.com/cr/report";

  private static final String NONE_MARKER = "__NONE__";

  private static final String PRODUCT_KEY = "product";
  private static final String VERSION_KEY = "version";
  private static final String EXCEPTION_INFO_KEY = "exception_info";
  private static final String COMMENTS_KEY = "comments";

  private static final String ECLIPSE_BUILD_ID_LABEL = "eclipseBuildId: ";
  private static final String JAVA_VERSION_LABEL = "javaVersion: ";
  private static final String OS_LABEL = "os: ";
  private static final String USER_SEVERITY_LABEL = "userSeverity: ";
  private static final String USER_COMMENT_LABEL = "userComment: ";

  private final String endpointUrl;

  ExceptionSender() {
    this(REPORT_URL);
  }

  @VisibleForTesting
  ExceptionSender(String endpointUrl) {
    this.endpointUrl = endpointUrl;
  }

  // We accept AERI's "IThrowable" instead of standard "Throwable", because AERI's version takes
  // anonymization (if done) into account.
  void sendException(IThrowable exception, String eclipseBuildId, String javaVersion,
      String os, String osVersion, String userSeverity, String userComment) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PRODUCT_KEY, CloudToolsInfo.EXCEPTION_REPORT_PRODUCT_ID);
    parameters.put(VERSION_KEY, CloudToolsInfo.getToolsVersion());
    parameters.put(EXCEPTION_INFO_KEY, formatStackTrace(exception));

    String extraInfo = ECLIPSE_BUILD_ID_LABEL + nullOrEmptyToNone(eclipseBuildId) + "\n"
        + JAVA_VERSION_LABEL + nullOrEmptyToNone(javaVersion) + "\n"
        + OS_LABEL + os + " " + nullOrEmptyToNone(osVersion) + "\n"
        + USER_SEVERITY_LABEL + nullOrEmptyToNone(userSeverity) + "\n"
        + USER_COMMENT_LABEL + nullOrEmptyToNone(userComment);
    // "comments" seems to be the only viable place where we can put extra info.
    parameters.put(COMMENTS_KEY, extraInfo);

    try {
      // Internal system expects the product ID and version in URL too.
      String urlParameters = "?product=" + CloudToolsInfo.EXCEPTION_REPORT_PRODUCT_ID
          + "&version=" + CloudToolsInfo.getToolsVersion();
      int code = HttpUtil.sendPostMultipart(endpointUrl + urlParameters, parameters);
      if (code != HttpURLConnection.HTTP_OK) {
        logger.log(Level.SEVERE, "Failed to send exception report. HTTP response code: " + code);
      }
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Failed to send exception report.", ex);
    }
  }

  private static String nullOrEmptyToNone(String string) {
    if (string == null || string.trim().isEmpty()) {
      return NONE_MARKER;
    }
    return string;
  }

  // Unfortunately, "IThrowable" doesn't have "Throwable.printStackTrace(...)" equivalent,
  // so we need to implement it ourselves.
  @VisibleForTesting
  static String formatStackTrace(IThrowable exception) {
    if (exception == null) {
      return NONE_MARKER;
    }

    StringBuilder trace =
        new StringBuilder(exception.getClassName() + ": " + exception.getMessage());
    for (IStackTraceElement frame : exception.getStackTrace()) {
      trace.append("\n\tat ").append(frame.getClassName())
          .append('.').append(frame.getMethodName());
      trace.append('(');
      if (frame.getFileName() != null) {
        trace.append(frame.getFileName());
        if (frame.getLineNumber() > 0) {
          trace.append(':').append(frame.getLineNumber());
        }
      }
      trace.append(')');
    }
    return trace.toString();
  }
}
