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

package com.google.cloud.tools.eclipse.appengine.whitelist;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;

import com.google.cloud.tools.eclipse.test.util.BasePluginXmlTest;

public class XmlTest extends BasePluginXmlTest {
  
  @Test
  public void testLoadCompilationParticipant() {
    Element compilationParticipant = (Element) getDocument()
        .getDocumentElement()
        .getElementsByTagName("compilationParticipant")
        .item(0);
    String className = compilationParticipant.getAttribute("class");
    try {
      Class.forName(className).newInstance();
    } catch (ClassNotFoundException ex) {
      Assert.fail("Could not load class " + className + " referenced in plugin.xml");
    } catch (InstantiationException ex) {
      Assert.fail("Class " + className + " does not have a no-arg constructor");
    } catch (IllegalAccessException e) {
      Assert.fail("Class " + className + " no-arg constructor is not public");
    }
  }
  
}
