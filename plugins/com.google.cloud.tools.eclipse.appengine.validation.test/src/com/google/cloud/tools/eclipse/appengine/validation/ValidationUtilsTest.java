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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
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
  
  private DocumentLocation location = new DocumentLocation(2, 13);
  private BannedElement element =
      new AppEngineBlacklistElement("application", location, 3);
  private ArrayList<BannedElement> blacklist = new ArrayList<>(Arrays.asList(element));
  
  @Test
  public void testGetOffsetMap_unixXml() {
    byte[] bytes = UNIX_XML_WITH_PROJECT_ID.getBytes(StandardCharsets.UTF_8);
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, blacklist, "UTF-8");
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(20, offset);
  }
  
  @Test
  public void testGetOffsetMap_macXml() {
    byte[] bytes = MAC_XML_WITH_PROJECT_ID.getBytes(StandardCharsets.ISO_8859_1);
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, blacklist, "ISO_8859_1");
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(20, offset);
  }
  
  @Test
  public void testGetOffsetMap_windowsXml() throws IOException {
    byte[] bytes = WINDOWS_XML_WITH_PROJECT_ID.getBytes("CP1252");
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, blacklist, "CP1252");
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(20, offset);
  }

  @Test
  public void testGetOffsetMap_mixedXml() {
    blacklist.clear();
    byte[] bytes = MIXED_XML_WITH_PROJECT_ID.getBytes(StandardCharsets.UTF_8);
    DocumentLocation start = new DocumentLocation(3, 13);
    BannedElement element = new BannedElement(
        "application", 
        "", 
        IMarker.SEVERITY_WARNING, 
        IMessage.NORMAL_SEVERITY, 
        start, 
        1, 
        null);
    blacklist.add(element);
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, blacklist, "UTF-8");
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(21, offset);
  }
  
  @Test
  public void testGetOffsetMap_lineWithWhitespace() {
    byte[] bytes = XML_WITH_PROJECT_ID_WHITESPACE.getBytes(StandardCharsets.UTF_8);
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, blacklist, "UTF-8");
    assertEquals(1, map.size());
    int offset = map.get(element);
    assertEquals(14, offset);
  }
  
  @Test
  public void testGetOffsetMap_orderedElements() {
    
    DocumentLocation applicationLocation = new DocumentLocation(2, 14);
    DocumentLocation versionLocation = new DocumentLocation(1, 10);
    BannedElement application =
        new AppEngineBlacklistElement("application", applicationLocation, 0);
    BannedElement version = new AppEngineBlacklistElement("version", versionLocation, 0);
    ArrayList<BannedElement> blacklist = new ArrayList<>(Arrays.asList(application, version));
    
    String xml = "<version>   </version>\n\n<application>   </application>";
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    Map<BannedElement, Integer> map = ValidationUtils.getOffsetMap(bytes, blacklist, "UTF-8");
    
    assertEquals(2, map.size());
    assertEquals(36, (int) map.get(application));
    assertEquals(9, (int) map.get(version));
  }
  
  @Test
  public void testConvertStreamToString() throws IOException {
    String test = "test string";
    byte[] bytes = test.getBytes(StandardCharsets.UTF_8);
    InputStream stream = new ByteArrayInputStream(bytes);
    assertEquals(test, ValidationUtils.convertStreamToString(stream, "UTF-8"));
  }

}
