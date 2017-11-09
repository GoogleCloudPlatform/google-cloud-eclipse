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

package com.google.cloud.tools.eclipse.appengine.validation;

/**
 * Quick fix for web.xml available in source editor.
 */
public class ToServlet25SourceQuickFix extends XsltSourceQuickFix {
  
  public ToServlet25SourceQuickFix() {
    super("/xslt/servlet.xsl", Messages.getString("convert.servlet"));
  }
  
}