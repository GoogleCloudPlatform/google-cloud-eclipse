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

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

class AppEngineWebBlacklist {
  
  private static final ImmutableMap<String, List<String>> BLACKLIST =
      ImmutableMap.of(
          "application",
          Arrays.asList(Messages.getString("application.element"), 
              "com.google.cloud.tools.eclipse.appengine.validation.applicationMarker"),
          "version",
          Arrays.asList(Messages.getString("version.element"),
              "com.google.cloud.tools.eclipse.appengine.validation.versionMarker"));
  
  static boolean contains(String elementName) {
    return BLACKLIST.containsKey(elementName);
  }
  
  static String getBlacklistElementMessage(String element) {
    Preconditions.checkNotNull(element, "element is null");
    if (!BLACKLIST.containsKey(element)) {
      throw new IllegalArgumentException("element not in blacklist");
    }
    List<String> values = BLACKLIST.get(element);
    return values.get(0); 
  }
  
  static String getMarkerId(String element) {
    Preconditions.checkNotNull(element, "element is null");
    if (!BLACKLIST.containsKey(element)) {
      throw new IllegalArgumentException("element not in blacklist");
    }
    List<String> values = BLACKLIST.get(element);
    return values.get(1);
  }

}