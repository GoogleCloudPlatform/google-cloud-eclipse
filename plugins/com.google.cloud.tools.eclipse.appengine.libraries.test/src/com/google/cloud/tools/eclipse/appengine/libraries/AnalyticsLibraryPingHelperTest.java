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

import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

public class AnalyticsLibraryPingHelperTest {

  @Test
  public void testPrepareMetadata_emptyLibraryList() {
    Map<String, String> metadata = AnalyticsLibraryPingHelper.prepareMetadata(
        "awesome", new ArrayList<Library>());
    assertEquals(2, metadata.size());
    assertEquals("awesome", metadata.get("project.type"));
    assertEquals("null", metadata.get("library.ids"));
  }

  @Test
  public void testPrepareMetadata_singletonList() {
    Map<String, String> metadata = AnalyticsLibraryPingHelper.prepareMetadata(
        "awesome", Arrays.asList(new Library("id-1")));
    assertEquals(2, metadata.size());
    assertEquals("awesome", metadata.get("project.type"));
    assertEquals("id-1", metadata.get("library.ids"));
  }

  @Test
  public void testPrepareMetadata() {
    Map<String, String> metadata = AnalyticsLibraryPingHelper.prepareMetadata(
        "awesome", Arrays.asList(new Library("id-1"), new Library("id-2"), new Library("id-3")));
    assertEquals(2, metadata.size());
    assertEquals("awesome", metadata.get("project.type"));
    assertEquals("id-1,id-2,id-3", metadata.get("library.ids"));
  }
}
