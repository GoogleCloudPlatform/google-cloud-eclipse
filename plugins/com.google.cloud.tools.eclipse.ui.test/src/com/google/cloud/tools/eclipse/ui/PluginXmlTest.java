/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.ui;

import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.eclipse.test.util.BasePluginXmlTest;
import com.google.cloud.tools.eclipse.ui.util.OpenDropDownMenuHandler;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PluginXmlTest extends BasePluginXmlTest {
  @Test
  public void testExtensionPoint() {
    NodeList extensions = getDocument().getElementsByTagName("extension");
    assertEquals(5, extensions.getLength());

    // first element is the showPopup command
    Element extension = (Element) extensions.item(0);
    assertEquals("org.eclipse.ui.commands", extension.getAttribute("point")); //$NON-NLS-1$ //$NON-NLS-2$
    NodeList commandDefinitions = extension.getElementsByTagName("command"); //$NON-NLS-1$
    assertEquals(1, commandDefinitions.getLength());
    Element configExtension = (Element) commandDefinitions.item(0);
    assertEquals(OpenDropDownMenuHandler.class.getName(),
        configExtension.getAttribute("defaultHandler")); //$NON-NLS-1$
    assertEquals("com.google.cloud.tools.eclipse.ui.util.showPopup", //$NON-NLS-1$
        configExtension.getAttribute("id")); //$NON-NLS-1$

    // second element is the GCP Toolbar definition
    extension = (Element) extensions.item(1);
    assertEquals("org.eclipse.ui.menus", extension.getAttribute("point")); //$NON-NLS-1$ //$NON-NLS-2$
    NodeList menuContributions = extension.getElementsByTagName("menuContribution"); //$NON-NLS-1$
    assertEquals(3, menuContributions.getLength());
    Element menuContribution = (Element) menuContributions.item(0);
    // first element is the toolbar definition
    assertEquals("toolbar:org.eclipse.ui.main.toolbar?after=additions", //$NON-NLS-1$
        menuContribution.getAttribute("locationURI")); //$NON-NLS-1$
    // second contribution is our trim area definition
    menuContribution = (Element) menuContributions.item(1);
    assertEquals(
        "toolbar:org.eclipse.ui.trim.status?after=additions", // $NON-NLS-1$
        menuContribution.getAttribute("locationURI")); // $NON-NLS-1$
    // third contribution is our actual menu definition
    menuContribution = (Element) menuContributions.item(2);
    assertEquals("menu:com.google.cloud.tools.eclipse.ui.actions", //$NON-NLS-1$
        menuContribution.getAttribute("locationURI")); //$NON-NLS-1$

    // last element is the keywords definition
    extension = (Element) extensions.item(2);
    assertEquals(
        "org.eclipse.ui.keywords", extension.getAttribute("point")); // $NON-NLS-1$ //$NON-NLS-2$
    NodeList keywords = extension.getElementsByTagName("keyword"); // $NON-NLS-1$
    assertEquals(3, keywords.getLength());
  }

}
