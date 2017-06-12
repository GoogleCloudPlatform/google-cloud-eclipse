/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.cloud.tools.eclipse.test.util.BasePluginXmlTest;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XmlTest extends BasePluginXmlTest {

  private Document parse(File location)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(location);
  }

  @Test
  public void testValidMavenLifecycleMapping() throws Exception {
    parse(new File(
        "../com.google.cloud.tools.eclipse.appengine.newproject.maven/lifecycle-mapping-metadata.xml"));
  }

}
