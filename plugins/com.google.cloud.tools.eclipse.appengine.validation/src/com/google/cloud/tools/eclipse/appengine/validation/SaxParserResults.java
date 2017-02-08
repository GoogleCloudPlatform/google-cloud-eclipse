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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Wrap useful information found during parsing.
 */
class SaxParserResults {
  
  private Queue<BannedElement> blacklist;
  private String characterEncoding;
  
  SaxParserResults(Queue<BannedElement> blacklist, String characterEncoding) {
    Preconditions.checkNotNull(blacklist, "blacklist is null");
    Preconditions.checkNotNull(characterEncoding, "characterEncoding is null");
    this.blacklist = blacklist;
    this.characterEncoding = characterEncoding;
  }
  
  /**
   * If no character encoding is provided, assume UTF-8.
   */
  SaxParserResults() {
    this(new ArrayDeque<BannedElement>(), "UTF-8");
  }
  
  @VisibleForTesting
  SaxParserResults(Queue<BannedElement> blacklist) {
    this(blacklist, "UTF-8");
  }
  
  Queue<BannedElement> getBlacklist() {
    return blacklist;
  }
  
  String getEncoding() {
    return characterEncoding;
  }
  
}