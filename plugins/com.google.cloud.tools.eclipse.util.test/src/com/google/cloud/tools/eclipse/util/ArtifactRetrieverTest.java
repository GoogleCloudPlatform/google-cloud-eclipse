/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactRetrieverTest {
  
  @Test
  public void testGetInstance() throws URISyntaxException {
    ArtifactRetriever retriever1 = ArtifactRetriever.getInstance("http://www.example.com/");
    Assert.assertNotNull(retriever1);
    ArtifactRetriever retriever2 = ArtifactRetriever.getInstance("http://www.example.com/");
    Assert.assertSame(retriever1, retriever2);
  }

  @Test
  public void testGetMetadataUrl() throws MalformedURLException {
    Assert.assertEquals(
        new URL("https://repo1.maven.org/maven2/com/google/foo/bar-baz/maven-metadata.xml"),
        ArtifactRetriever.DEFAULT.getMetadataUrl("com.google.foo", "bar-baz"));
  }

  @Test
  public void testIdToKey() {
    Assert.assertEquals(
        "com.google.cloud.dataflow:google-cloud-dataflow-java-sdk-all",
        ArtifactRetriever.idToKey(
            "com.google.cloud.dataflow", "google-cloud-dataflow-java-sdk-all"));
  }

  @Test
  public void testKeyToId() {
    String[] actual =
        ArtifactRetriever.keyToId("com.google.cloud.dataflow:google-cloud-dataflow-java-sdk-all");
    Assert.assertEquals("com.google.cloud.dataflow", actual[0]);
    Assert.assertEquals("google-cloud-dataflow-java-sdk-all", actual[1]);
  }
}
