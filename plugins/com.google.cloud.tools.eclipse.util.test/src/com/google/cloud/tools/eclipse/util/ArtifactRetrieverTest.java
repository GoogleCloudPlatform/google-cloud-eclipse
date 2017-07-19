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
 *
 */

package com.google.cloud.tools.eclipse.util;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactRetrieverTest {
  
  private ArtifactRetriever retriever = new ArtifactRetriever();
  
  @Test
  public void testGetGuavaLatest() {
    ArtifactVersion guava = retriever.getLatestArtifactVersion("com.google.guava", "guava");
    Assert.assertTrue(guava.getMajorVersion() > 20);
    Assert.assertTrue(guava.getMinorVersion() >= 0);
  }
    
  @Test
  public void testGetGuava19() throws InvalidVersionSpecificationException {
    VersionRange range = VersionRange.createFromVersionSpec("[1.0,19.0]");
    ArtifactVersion guava = retriever.getLatestArtifactVersion("com.google.guava", "guava", range);
    Assert.assertEquals(19, guava.getMajorVersion());
    Assert.assertEquals(0, guava.getMinorVersion());
  }
  
  @Test
  public void testGetMetadataUrl() throws MalformedURLException {
    Assert.assertEquals(
        new URL("https://repo1.maven.org/maven2/com/google/foo/bar-baz/maven-metadata.xml"),
        retriever.getMetadataUrl("com.google.foo", "bar-baz"));
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
