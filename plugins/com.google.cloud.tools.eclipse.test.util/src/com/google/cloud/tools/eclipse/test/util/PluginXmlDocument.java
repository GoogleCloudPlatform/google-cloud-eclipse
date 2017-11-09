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

package com.google.cloud.tools.eclipse.test.util;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.rules.ExternalResource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Test utility class to obtain a {@link Document} representing the host bundle's plugin.xml.
 * <p>
 * Assumptions:
 * <ul>
 * <li>instances are created only in test fragment bundles that are hosted by the corresponding
 * production</li>
 * <li>the tests execute with the working directory in a bundle or fragment directory</li>
 * </ul>
 */
public class PluginXmlDocument extends ExternalResource {

  private Document doc;

  @Override
  protected void before() throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilder builder = createDocumentBuilder();
    try (InputStream pluginXml = getPluginXml()) {
      assertNotNull(pluginXml);
      // test fails if malformed
      doc = builder.parse(pluginXml);
    }
  }

  /**
   * Subclasses should override this method if the plugin.xml is not at the location of
   * <code>&lt;work_dir&gt;/../&lt;host_bundle_name&gt;/plugin.xml</code>
   */
  protected InputStream getPluginXml() throws IOException {
    String hostBundleName = EclipseProperties.getHostBundleName();
    assertNotNull(hostBundleName);
    String pluginXmlLocation = "../" + hostBundleName + "/plugin.xml";
    return Files.newInputStream(Paths.get(pluginXmlLocation));
  }

  /**
   * Returns the Document representing the plugin.xml. The file is parsed only once when
   * {@link #before()} is executed by JUnit.
   */
  public Document get() {
    return doc;
  }

  private static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder;
  }
}
