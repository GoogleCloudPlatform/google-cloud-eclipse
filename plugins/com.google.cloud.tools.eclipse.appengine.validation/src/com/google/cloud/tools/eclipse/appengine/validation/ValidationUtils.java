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

import com.google.common.io.CharStreams;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for validating XML files.
 */
class ValidationUtils {

  private static final Logger logger = Logger.getLogger(ValidationUtils.class.getName());

  /**
   * Creates a {@link Map} of {@link ElementProblem}s and their respective document-relative
   * character offsets.
   */
  static Map<ElementProblem, Integer> getOffsetMap(byte[] bytes,
      List<ElementProblem> problems, String encoding) {
    Map<ElementProblem, Integer> elementProblemOffsetMap = new HashMap<>();
    for (ElementProblem problem : problems) {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(bais, encoding))) {
        int charOffset = 0;
        for (int i = 1; i < problem.getStart().getLineNumber(); i++) {
          String line = reader.readLine();
          charOffset += line.length() + 1;
        }
        int start = charOffset + problem.getStart().getColumnNumber() - 1;
        elementProblemOffsetMap.put(problem, start);
      } catch (IOException ex) {
        logger.log(Level.SEVERE, ex.getMessage());
      }
    }
    return elementProblemOffsetMap;
  }

  static String convertStreamToString(InputStream stream, String charset) throws IOException {
    String result = CharStreams.toString(new InputStreamReader(stream, charset));
    return result;
  }

}