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

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import com.google.cloud.tools.eclipse.util.Xslt;

/**
 * Replaces web.xml <web-app> element with Servlet 2.5 deployment descriptor.
 */
public class ToServlet25QuickFix implements IMarkerResolution {
  
  private static final Logger logger = Logger.getLogger(
      ToServlet25QuickFix.class.getName());

  @Override
  public String getLabel() {
    return Messages.getString("convert.servlet");
  }

  @Override
  public void run(IMarker marker) {
    try {
      IFile file = (IFile) marker.getResource();
      URL xslPath = ToServlet25QuickFix.class.getResource("/xslt/servlet.xsl");
      Xslt.transformInPlace(file, xslPath);
    } catch (IOException | CoreException| TransformerException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }
  
}