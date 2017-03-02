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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import org.junit.Before;
import org.junit.Test;

public class ValidationUtilsTest {
  
  private static final String NEWLINE_UNIX = "\n";
  private static final String NEWLINE_MAC = "\r";
  private static final String NEWLINE_WINDOWS = "\r\n";
  private static final String PROJECT_ID = "<application></application>";
  private static final String LINE_WITH_WHITESPACE = " \n";
  private static final String UNIX_XML_WITH_PROJECT_ID =
      "1234567" + NEWLINE_UNIX + PROJECT_ID;
  
  private static final String MAC_XML_WITH_PROJECT_ID =
      "1234567" + NEWLINE_MAC + PROJECT_ID;
  
  private static final String WINDOWS_XML_WITH_PROJECT_ID =
      "1234567" + NEWLINE_WINDOWS + PROJECT_ID;
  
  private static final String MIXED_XML_WITH_PROJECT_ID =
      NEWLINE_UNIX + MAC_XML_WITH_PROJECT_ID;
  
  private static final String XML_WITH_PROJECT_ID_WHITESPACE =
      LINE_WITH_WHITESPACE + PROJECT_ID;
  
  private static final String XML_WITH_PROJECT_ID_FIRST =
      PROJECT_ID;
  
  private Queue<BannedElement> blacklist = new ArrayDeque<>();
  private BannedElement element;
  
  @Before
  public void setUp() {
    DocumentLocation start = new DocumentLocation(2, 1);
    element = new BannedElement("application", "", 1, start, 1);
    blacklist.add(element);
  }
  
  @Test
  public void testGetOffsetMap_unixXml() throws IOException {
    byte[] bytes = UNIX_XML_WITH_PROJECT_ID.getBytes(StandardCharsets.UTF_8);
    SaxParserResults result = new SaxParserResults(blacklist, "UTF-8");
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, result);
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(8, offset);
  }
  
  @Test
  public void testGetOffsetMap_macXml() throws IOException {
    byte[] bytes = MAC_XML_WITH_PROJECT_ID.getBytes(StandardCharsets.ISO_8859_1);
    SaxParserResults result = new SaxParserResults(blacklist, "ISO_8859_1");
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, result);
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(8, offset);
  }
  
  @Test
  public void testGetOffsetMap_windowsXml() throws IOException {
    byte[] bytes = WINDOWS_XML_WITH_PROJECT_ID.getBytes("CP1252");
    SaxParserResults result = new SaxParserResults(blacklist, "CP1252");
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, result);
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(8, offset);
  }
  
  @Test
  public void testGetOffsetMap_mixedXml() throws IOException {
    byte[] bytes = MIXED_XML_WITH_PROJECT_ID.getBytes(StandardCharsets.UTF_8);
    blacklist.clear();
    DocumentLocation start = new DocumentLocation(3, 1);
    BannedElement element = new BannedElement("application", "", 1, start, 1);
    blacklist.add(element);
    SaxParserResults result = new SaxParserResults(blacklist, "UTF-8");
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, result);
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(9, offset);
  }
  
  @Test
  public void testGetOffsetMap_lineWithWhitespace() throws IOException {
    byte[] bytes = XML_WITH_PROJECT_ID_WHITESPACE.getBytes(StandardCharsets.UTF_8);
    SaxParserResults result = new SaxParserResults(blacklist, "UTF-8");
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, result);
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(2, offset);
  }
  
  @Test
  public void testGetOffsetMap_firstElement() throws IOException {
    blacklist.clear();
    DocumentLocation start = new DocumentLocation(1, 1);
    BannedElement newElement = new BannedElement("application", "", 1, start, 1);
    blacklist.add(newElement);
    byte[] bytes = XML_WITH_PROJECT_ID_FIRST.getBytes(StandardCharsets.UTF_8);
    SaxParserResults result = new SaxParserResults(blacklist, "UTF-8");
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, result);
    assertEquals(1, map.size());
    int offset = map.get(newElement);
    assertEquals(0, offset);
  }
  
  @Test
  public void testConvertStreamToString() throws IOException {
    String test = "test string";
    byte[] bytes = test.getBytes(StandardCharsets.UTF_8);
    InputStream stream = new ByteArrayInputStream(bytes);
    assertEquals(test, ValidationUtils.convertStreamToString(stream, "UTF-8"));
  }
}
