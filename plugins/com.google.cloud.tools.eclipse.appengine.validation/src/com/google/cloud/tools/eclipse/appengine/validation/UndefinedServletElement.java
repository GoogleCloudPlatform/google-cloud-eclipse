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

import org.eclipse.core.resources.IMarker;

/**
 * A servlet class element that will receive an undefined servlet marker. 
 */
public class UndefinedServletElement extends BannedElement {

  private static final String markerId = 
      "com.google.cloud.tools.eclipse.appengine.validation.undefinedServletMarker";
  private final String servletClassName;
  
  public UndefinedServletElement(String servletClassName, DocumentLocation start, int length) {
    super(Messages.getString("undefined.servlet.class", servletClassName),
        markerId, IMarker.SEVERITY_ERROR, start, length);
    this.servletClassName = servletClassName;
  }
  
  String getServletClassName() {
    return servletClassName;
  }

}