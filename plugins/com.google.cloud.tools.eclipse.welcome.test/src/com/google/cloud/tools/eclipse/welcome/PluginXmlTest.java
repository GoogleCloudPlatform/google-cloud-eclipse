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

package com.google.cloud.tools.eclipse.welcome;

import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.eclipse.test.util.BasePluginXmlTest;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PluginXmlTest extends BasePluginXmlTest {

  @Test
  public void testExtensionPoint() {
    NodeList extensions = getDocument().getElementsByTagName("extension");
    assertEquals(1, extensions.getLength());
    Element extension = (Element) extensions.item(0);
    assertEquals("org.eclipse.ui.intro.configExtension", extension.getAttribute("point"));

    NodeList configExtensions = extension.getElementsByTagName("configExtension");
    assertEquals(1, configExtensions.getLength());
    Element configExtension = (Element) configExtensions.item(0);
    assertEquals("intro/cloud-tools-for-eclipse.xml", configExtension.getAttribute("content"));
    assertEquals("org.eclipse.ui.intro.universalConfig", configExtension.getAttribute("configId"));
  }
}
