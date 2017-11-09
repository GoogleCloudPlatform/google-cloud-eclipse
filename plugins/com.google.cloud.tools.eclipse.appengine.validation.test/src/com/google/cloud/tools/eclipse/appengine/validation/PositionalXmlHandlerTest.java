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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.Stack;

import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.AttributesImpl;

public class PositionalXmlHandlerTest {

  private PositionalXmlHandler handler = new PositionalXmlHandler();
  
  @Test
  public void testStartDocument() {
    handler.startDocument();
    assertNotNull(handler.getDocument());
  }
  
  @Test
  public void testStartElement() throws SAXException {
    handler.startDocument();
    Locator2 locator = Mockito.mock(Locator2.class);
    handler.setDocumentLocator(locator);
    Mockito.when(locator.getLineNumber()).thenReturn(1);
    Mockito.when(locator.getColumnNumber()).thenReturn(7);
    handler.startElement("", "", "element", new AttributesImpl());
    
    assertEquals(1, handler.getElementStack().size());
    
    Element element = handler.getElementStack().pop();
    DocumentLocation location = (DocumentLocation) element.getUserData("location");
    assertEquals(1, location.getLineNumber());
    assertEquals(7, location.getColumnNumber());
  }
  
  @Test
  public void testEndElement() throws SAXException {
    handler.startDocument();
    Locator2 locator = Mockito.mock(Locator2.class);
    handler.setDocumentLocator(locator);
    Mockito.when(locator.getLineNumber()).thenReturn(1);
    Mockito.when(locator.getColumnNumber()).thenReturn(7);
    handler.startElement("", "", "element", new AttributesImpl());
    
    assertEquals(1, handler.getElementStack().size());
    
    Mockito.when(locator.getEncoding()).thenReturn("UTF-8");
    handler.endElement("", "", "element");
    
    assertEquals(0, handler.getElementStack().size());
    
    Document document = handler.getDocument();
    Node node = document.getDocumentElement();
    DocumentLocation location = (DocumentLocation) node.getUserData("location");
    assertEquals(1, location.getLineNumber());
    assertEquals(7, location.getColumnNumber());
  }
  
  @Test
  public void testAddText() throws SAXException {
    char[] test = "test".toCharArray();
    
    handler.startDocument();
    Locator2 locator = Mockito.mock(Locator2.class);
    handler.setDocumentLocator(locator);
    Mockito.when(locator.getLineNumber()).thenReturn(1);
    Mockito.when(locator.getColumnNumber()).thenReturn(7);
    handler.startElement("", "", "element", new AttributesImpl());
    
    handler.characters(test, 0, test.length);
    handler.addText();
    
    Stack<Element> elements = handler.getElementStack();
    Node parent = elements.pop();
    NodeList childNodes = parent.getChildNodes();
    assertEquals(1, childNodes.getLength());
    assertEquals("test", childNodes.item(0).getTextContent());
  }

}
