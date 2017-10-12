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

package com.google.cloud.tools.eclipse.integration.appengine;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.Assert;
import org.junit.Test;

import com.google.cloud.tools.eclipse.util.ArtifactRetriever;

/**
 * These tests connect to Maven Central.
 */
public class ArtifactRetrieverIntegrationTest {
    
  @Test
  public void testGetServletLatest() {
    // group and version IDs changed with 3.0 so this should be stable
    ArtifactVersion servlet =
        ArtifactRetriever.DEFAULT.getLatestArtifactVersion("javax.servlet", "servlet-api");
    Assert.assertEquals(2, servlet.getMajorVersion());
    Assert.assertEquals(5, servlet.getMinorVersion());
    Assert.assertNull(servlet.getQualifier());
  }
  
  @Test
  public void testGetGuava() {
    ArtifactVersion guava =
        ArtifactRetriever.DEFAULT.getLatestArtifactVersion("com.google.guava", "guava");
    Assert.assertTrue(guava.getMajorVersion() > 19);
    Assert.assertTrue(guava.getMinorVersion() >= 0);
    Assert.assertNull(guava.getQualifier());
  }
    
  @Test
  public void testGetGuava19() throws InvalidVersionSpecificationException {
    VersionRange range = VersionRange.createFromVersionSpec("[1.0,19.0]");
    ArtifactVersion guava =
        ArtifactRetriever.DEFAULT.getLatestArtifactVersion("com.google.guava", "guava", range);
    Assert.assertEquals(19, guava.getMajorVersion());
    Assert.assertEquals(0, guava.getMinorVersion());
  }
  
  @Test
  public void testGetLatestArtifactVersion() {
    ArtifactVersion version = ArtifactRetriever.DEFAULT.getLatestArtifactVersion(
        "com.google.cloud", "google-cloud-pubsub");
    if (version == null) {
      // No release version. This is success.
      return; 
    }
    Assert.assertTrue(version.getMajorVersion() > 0);
  }

}
