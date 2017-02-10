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

import com.google.common.annotations.VisibleForTesting;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Applies application.xsl to appengine-web.xml to remove an <application/> element.
 */
public class ApplicationQuickFix implements IMarkerResolution {
  
  private static final String APPLICATION_XSLT = "/xslt/application.xsl";
  private static final Logger logger = Logger.getLogger(
    ApplicationQuickFix.class.getName());

  @Override
  public String getLabel() {
    return "Remove Application Element";
  }

  @Override
  public void run(IMarker marker) {
    removeApplicationElements(marker.getResource());
  }
  
  static void removeApplicationElements(IResource resource) {
    IFile file = (IFile) resource;
    try (InputStream in = file.getContents();
        InputStream styleSheetStream =
            ApplicationQuickFix.class.getResourceAsStream(APPLICATION_XSLT)) {

      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      builderFactory.setNamespaceAware(true);
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      Document document = builder.parse(in);
      
      try (InputStream resultStream = applyXslt(document, styleSheetStream)) {
        file.setContents(resultStream, IFile.FORCE, null /*progress monitor*/);
      }
    } catch (CoreException | IOException | ParserConfigurationException 
        | SAXException | TransformerException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }
  
  @VisibleForTesting
  static InputStream applyXslt(Document document, InputStream stylesheet)
      throws IOException, TransformerException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer(new StreamSource(stylesheet));
      transformer.transform(new DOMSource(document), new StreamResult(outputStream));
      return new ByteArrayInputStream(outputStream.toByteArray());
    }
  }
  
}
