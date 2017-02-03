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
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class AppEngineWebXmlValidatorTest {

  private static final String XML = "<application></application>";
  private static final String BAD_XML = "<";
  private static final String BAD_XML_MESSAGE =
      "XML document structures must start and end within the same entity.";
  private static final String ELEMENT_NAME = "application";
  private static final String ELEMENT_MESSAGE =
      "project ID tag not recommended";
  private static final String UTF = "UTF-8";
  
  private IResource resource = mock(IResource.class);
  
  @Test
  public void testValidate_badXml()
      throws IOException, CoreException, ParserConfigurationException {
 
    byte[] bytes = BAD_XML.getBytes(UTF);
    ValidationResult result = AppEngineWebXmlValidator.validate(resource, bytes);
    ValidatorMessage[] messages = result.getMessages();
    String resultMessage = (String)messages[0].getAttribute(IMarker.MESSAGE);
    assertEquals(BAD_XML_MESSAGE, resultMessage);
  }
  
  @Test
  public void testAddMessage_noBannedTags() throws IOException {
    byte[] bytes = XML.getBytes(UTF);
    Stack<BannedElement> blacklist = new Stack<>();
    ValidationResult result = AppEngineWebXmlValidator.addMessages(resource, bytes, blacklist);
    ValidatorMessage[] messages = result.getMessages();
    assertEquals(0, messages.length);
  }
  
  @Test
  public void testAddMessages() throws IOException {
    byte[] bytes = XML.getBytes(UTF);
    Stack<BannedElement> blacklist = new Stack<>();
    blacklist.add(new BannedElement(ELEMENT_NAME));
    ValidationResult result = AppEngineWebXmlValidator.addMessages(resource, bytes, blacklist);
    ValidatorMessage[] messages = result.getMessages();
    assertEquals(1, messages.length);
    assertEquals(ELEMENT_MESSAGE, (String) messages[0].getAttribute(IMarker.MESSAGE));
  }
  
  @Test
  public void testCreateMessage() {
    BannedElement element = new BannedElement(ELEMENT_NAME);
    ValidatorMessage message = AppEngineWebXmlValidator.createMessage(resource, element, 0);
    assertEquals(ELEMENT_MESSAGE, (String) message.getAttribute(IMarker.MESSAGE));
  }
  
  @Test
  public void testCreateSAXErrorMessage() {
    SAXParseException spe = new SAXParseException("", "", "", 1, 1);
    SAXException ex = new SAXException(ELEMENT_MESSAGE, spe);
    ValidationResult result = AppEngineWebXmlValidator.createSaxErrorMessage(resource, ex);
    ValidatorMessage[] messages = result.getMessages();
    assertEquals(ELEMENT_MESSAGE, (String) messages[0].getAttribute(IMarker.MESSAGE));
  }
}
