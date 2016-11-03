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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.rules.ExternalResource;
import org.w3c.dom.Document;

/**
 * Test utility class to obtain a {@link Document} representing the host bundle's plugin.xml.
 * <p>
 * Assumptions:
 * <ul>
 *  <li>instances are created only in test fragments bundles that are hosted by the corresponding production</li>
 *  <li>host and test bundles are located under the same parent directory</li>
 *  <li>test bundle's name (Bundle-SymbolicName) is the host bundle's name postfixed with <code>.test</code>
 * </ul>
 */
public class PluginXmlDocument extends ExternalResource {

  private Document doc;

  @Override
  protected void before() throws Throwable {
    String hostBundleName = getHostBundleName();
    String pluginXmlLocation = "../" + hostBundleName + "/plugin.xml";
    DocumentBuilder builder = createDocumentBuilder();
    // test fails if malformed
    doc = builder.parse(new File(pluginXmlLocation));
  }
  
  public Document get() {
    return doc;
  }

  private DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder;
  }

  private String getHostBundleName() throws IOException, FileNotFoundException {
    String manifestPath = "META-INF/MANIFEST.MF";
    Manifest manifest = new Manifest(new FileInputStream(manifestPath));
    Attributes attr = manifest.getMainAttributes();
    String testBundleName = attr.getValue("Bundle-SymbolicName");
    List<String> bundleNameComponents = Splitter.on('.').splitToList(testBundleName);
    int size = bundleNameComponents.size();
    assertThat(bundleNameComponents.get(size - 1), is("test"));
    return Joiner.on('.').join(bundleNameComponents.subList(0, size - 1));
  }
}
