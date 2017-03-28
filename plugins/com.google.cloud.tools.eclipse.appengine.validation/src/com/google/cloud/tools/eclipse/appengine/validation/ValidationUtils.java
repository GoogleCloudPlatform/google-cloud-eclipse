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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;
import java.util.Queue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.CharStreams;

/**
 * Utility methods for validating XML files.
 */
public class ValidationUtils {

  private static final Logger logger = Logger.getLogger(ValidationUtils.class.getName());
  
  /**
   * Creates a {@link Map} of {@link BannedElement}s and their respective document-relative
   * character offsets.
   */
  public static Map<BannedElement, Integer> getOffsetMap(byte[] bytes,
      SaxParserResults parserResults) {
    Queue<BannedElement> blacklist = parserResults.getBlacklist();
    Map<BannedElement, Integer> bannedElementOffsetMap = new HashMap<>();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(bais, parserResults.getEncoding()))) {
      int currentLine = 1;
      int charOffset = 0;
      while (!blacklist.isEmpty()) {
        BannedElement element = blacklist.poll();
        while (element.getStart().getLineNumber() > currentLine) {
          String line = reader.readLine();
          charOffset += line.length() + 1;
          currentLine++;
        }
        int start = charOffset + element.getStart().getColumnNumber() - 1;
        bannedElementOffsetMap.put(element, start);
      }
    } catch (IOException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
    return bannedElementOffsetMap;
  }
  
  static String convertStreamToString(InputStream is, String charset) throws IOException {
    String result = CharStreams.toString(new InputStreamReader(is, charset));
    return result;
  }
  
  public static Map<BannedElement, Integer> getOffsetMap(byte[] bytes,
      ArrayList<BannedElement> blacklist, String encoding) {
    Map<BannedElement, Integer> bannedElementOffsetMap = new HashMap<>();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(bais, encoding))) {
      int currentLine = 1;
      int charOffset = 0;
      for (BannedElement element : blacklist) {
        while (element.getStart().getLineNumber() > currentLine) {
          String line = reader.readLine();
          charOffset += line.length() + 1;
          currentLine++;
        }
        int start = charOffset + element.getStart().getColumnNumber() - 1;
        bannedElementOffsetMap.put(element, start);
      }
    } catch (IOException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
    return bannedElementOffsetMap;
  }
  
  @VisibleForTesting
  static ArrayList<BannedElement> checkForElements(Document document) {
    ArrayList<BannedElement> blacklist = new ArrayList<>();
    for (String elementName : AppEngineWebBlacklist.getBlacklistElements()) {
      NodeList nodeList = document.getElementsByTagName(elementName);
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        AppEngineBlacklistElement element = new AppEngineBlacklistElement(
            AppEngineWebBlacklist.getBlacklistElementMessage(elementName),
            (DocumentLocation) node.getUserData("location"),
            node.getTextContent().length());    
        blacklist.add(element);
      }
    }
    return blacklist;
  }
 
}