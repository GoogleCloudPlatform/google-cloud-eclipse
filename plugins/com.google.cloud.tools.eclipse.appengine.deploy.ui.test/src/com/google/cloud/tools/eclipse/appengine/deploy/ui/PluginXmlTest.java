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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.BasePluginXmlTest;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PluginXmlTest extends BasePluginXmlTest {

  @Test
  public void testLimitedVisibility() {
    NodeList pages = getDocument().getElementsByTagName("page");
    Assert.assertEquals(2, pages.getLength());
    NodeList enabledWhen = getDocument().getElementsByTagName("enabledWhen");
    Assert.assertEquals(3, enabledWhen.getLength());
    NodeList tests = getDocument().getElementsByTagName("test");
    Assert.assertEquals(3, tests.getLength());
    NodeList adapts = getDocument().getElementsByTagName("adapt");
    Assert.assertEquals(1, adapts.getLength());

    for (int i = 0; i < enabledWhen.getLength(); i++) {
      Element element = (Element) enabledWhen.item(i);
      Node parent = element.getParentNode();
      assertThat(parent.getNodeName(), either(is("page")).or(is("handler")));
    }

    Element adapt = (Element) adapts.item(0);
    Assert.assertEquals("org.eclipse.core.resources.IProject", adapt.getAttribute("type"));

    NodeList adaptTestNodes = adapt.getElementsByTagName("test");
    Assert.assertEquals(1, adaptTestNodes.getLength());
    Element adaptTestEntry1 = (Element) adaptTestNodes.item(0);
    String adaptTestProperty = "org.eclipse.wst.common.project.facet.core.projectFacet";
    Assert.assertEquals(adaptTestProperty, adaptTestEntry1.getAttribute("property"));
    Assert.assertEquals(AppEngineStandardFacet.ID, adaptTestEntry1.getAttribute("value"));
  }
}
