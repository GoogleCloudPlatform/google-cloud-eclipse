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

package com.google.cloud.tools.eclipse.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class Xslt {

  private Xslt() {}
  
  private static final TransformerFactory factory = TransformerFactory.newInstance();

  public static void transformInPlace(IFile file, URL xslt) 
      throws IOException, CoreException, TransformerException {
    try (InputStream in = file.getContents();
        InputStream stylesheetStream = xslt.openStream();
        InputStream resultStream = applyXslt(in, stylesheetStream)) {
      boolean force = true;
      boolean keepHistory = true;
      file.setContents(resultStream, force, keepHistory, null /* monitor */);
    }
  }

  /**
   * Applies XSLT transformation.
   *
   * @return the result of transformation as {@link InputStream}
   */
  public static InputStream applyXslt(InputStream document, InputStream stylesheet)
      throws IOException, TransformerException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Transformer transformer = factory.newTransformer(new StreamSource(stylesheet));
      transformer.transform(new StreamSource(document), new StreamResult(outputStream));

      return new ByteArrayInputStream(outputStream.toByteArray());
    }
  }
  
}
