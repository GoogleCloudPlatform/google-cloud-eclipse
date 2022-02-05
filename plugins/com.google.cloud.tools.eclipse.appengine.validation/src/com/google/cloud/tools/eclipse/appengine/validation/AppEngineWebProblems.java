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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

class AppEngineWebProblems {
  
  private static final ImmutableMap<String, List<String>> DEPRECATED_ELEMENTS =
      ImmutableMap.of(
          "application",
          Arrays.asList(Messages.getString("application.element"), 
              "com.google.cloud.tools.eclipse.appengine.validation.applicationMarker"),
          "version",
          Arrays.asList(Messages.getString("version.element"),
              "com.google.cloud.tools.eclipse.appengine.validation.versionMarker"));
  
  /**
   * The {@link AbstractQuickAssistProcessor} for each tag.
   */
  private static final ImmutableMap<String, AbstractQuickAssistProcessor> QUICK_ASSIST_PROCESSORS =
      ImmutableMap.of(
          "application", new ApplicationQuickAssistProcessor(),
          "runtime", new UpgradeRuntimeQuickAssistProcessor(),
          "version", new VersionQuickAssistProcessor());
  
  static boolean contains(String elementName) {
    return DEPRECATED_ELEMENTS.containsKey(elementName);
  }
  
  static String getDeprecatedElementMessage(String elementName) {
    Preconditions.checkNotNull(elementName, "elementName is null");
    if (!DEPRECATED_ELEMENTS.containsKey(elementName)) {
      throw new IllegalArgumentException(elementName + " is not deprecated");
    }
    List<String> values = DEPRECATED_ELEMENTS.get(elementName);
    return values.get(0); 
  }
  
  static String getMarkerId(String element) {
    Preconditions.checkNotNull(element, "element is null");
    if (!DEPRECATED_ELEMENTS.containsKey(element)) {
      throw new IllegalArgumentException(element + " is not deprecated");
    }
    List<String> values = DEPRECATED_ELEMENTS.get(element);
    return values.get(1);
  }
  
  static AbstractQuickAssistProcessor getQuickAssistProcessor(String element) {
    Preconditions.checkNotNull(element, "element is null");
    return QUICK_ASSIST_PROCESSORS.get(element);
  }
  
  static ArrayList<String> getDeprecatedElements() {
    return Lists.newArrayList(DEPRECATED_ELEMENTS.keySet());
  }

}