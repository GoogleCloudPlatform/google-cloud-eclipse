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

package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsLibraryPingHelper {

  public static void sendLibrarySelectionPing(String projectType, List<Library> librariesSelected) {
    Map<String, String> metadata = prepareMetadata(projectType, librariesSelected);
    AnalyticsPingManager.getInstance().sendPing(AnalyticsEvents.LIBRARY_SELECTED, metadata);
  }

  @VisibleForTesting
  static Map<String, String> prepareMetadata(String projectType, List<Library> libraries) {
    String libraryIdsString = "null";

    if (!libraries.isEmpty()) {
      List<String> libraryIds = new ArrayList<>();
      for (Library library : libraries) {
        libraryIds.add(library.getId());
      }
      libraryIdsString = Joiner.on(',').join(libraryIds);
    }

    Map<String, String> metadata = new HashMap<>();
    metadata.put(AnalyticsEvents.PROJECT_TYPE, projectType);
    metadata.put(AnalyticsEvents.LIBRARY_IDS, libraryIdsString);
    return metadata;
  }
}
