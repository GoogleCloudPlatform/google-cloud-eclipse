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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Utility methods for validating XML files.
 */
public class ValidationUtils {
  
  /**
   * Creates a Map of BannedElements and their respective document-relative
   * character offsets
   */
  public static Map<BannedElement, Integer> getOffsetMap(byte[] bytes,
      Stack<BannedElement> blacklist) throws IOException {
    Map<BannedElement, Integer> bannedElementOffsetMap = new HashMap<>();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    BufferedReader reader = new BufferedReader(new InputStreamReader(bais));
    int i = 1;
    int charOffset = 0;
    while (!blacklist.isEmpty()) {
      BannedElement element = blacklist.pop();
      while (element.getStart().getLineNumber() > i) {
        String line = reader.readLine();
        charOffset += line.length() + 1;
        i++;
      }
      charOffset += element.getStart().getColumnNumber() - 1;
      bannedElementOffsetMap.put(element, charOffset);
    }
    reader.close();
    return bannedElementOffsetMap;
  }
}