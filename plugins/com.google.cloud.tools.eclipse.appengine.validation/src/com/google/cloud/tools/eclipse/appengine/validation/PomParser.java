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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Retrieves and returns the results of the SAX parser.
 */
class PomParser {
  
  static SaxParserResults readXml(byte[] bytes)
      throws ParserConfigurationException, IOException, SAXException {
    if (bytes.length == 0) { //file is empty
      return new SaxParserResults();
    }
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    InputSource is = new InputSource(bais);
    XMLReader reader = XMLReaderFactory.createXMLReader();
    PomXmlScanner handler = new PomXmlScanner();
    
    reader.setContentHandler(handler);
    reader.setErrorHandler(handler);
    reader.parse(is);
    return handler.getParserResults();
  }
  
}