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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;

import com.google.cloud.tools.eclipse.util.Xslt;

/**
 * Applies application.xsl to appengine-web.xml to remove an <application/> element.
 */
public class ApplicationQuickFix implements IMarkerResolution {
  
  private static final String APPLICATION_XSLT = "/xslt/application.xsl";
  private static final Logger logger = Logger.getLogger(
    ApplicationQuickFix.class.getName());

  @Override
  public String getLabel() {
    // TODO localize
    return "Remove Application Element";
  }

  @Override
  public void run(IMarker marker) {
    removeApplicationElements(marker.getResource());
  }
  
  private static void removeApplicationElements(IResource resource) {
    IFile file = (IFile) resource;
    URL xslt = ApplicationQuickFix.class.getResource(APPLICATION_XSLT);
    try {
      Xslt.transformInPlace(file, xslt);
    } catch (CoreException | IOException | TransformerException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }
  
}
