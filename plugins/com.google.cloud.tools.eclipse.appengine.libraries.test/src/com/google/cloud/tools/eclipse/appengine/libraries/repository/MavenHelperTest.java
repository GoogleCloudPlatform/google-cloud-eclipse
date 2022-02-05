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

package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Test;

public class MavenHelperTest {

  private static final String EXPECTED_DOWNLOAD_FOLDER =
      ".metadata/.plugins/com.google.cloud.tools.eclipse.appengine.libraries/downloads/groupId/artifactId/1.0.0";

  @Test
  public void testBundleStateBasedMavenFolder_withSpecificVersion() {
    MavenCoordinates coordinates = new MavenCoordinates.Builder()
        .setGroupId("groupId")
        .setArtifactId("artifactId")
        .setVersion("1.0.0")
        .build();
    IPath folder = MavenHelper.bundleStateBasedMavenFolder(coordinates);
    assertTrue(folder.toString().endsWith(EXPECTED_DOWNLOAD_FOLDER));
  }
  
  @Test
  public void testResolveArtifact() throws CoreException {
    MavenCoordinates coordinates = new MavenCoordinates.Builder()
        .setGroupId("com.google.cloud")
        .setArtifactId("google-cloud-datastore")
        .setVersion("1.102.1")
        .build();
    Artifact artifact = MavenHelper.resolveArtifact(coordinates, new NullProgressMonitor());
    Assert.assertEquals("google-cloud-datastore", artifact.getArtifactId());
  }
  
  @Test
  public void testResolveArtifact_googleApiClient() throws CoreException {
    MavenCoordinates coordinates = new MavenCoordinates.Builder()
        .setGroupId("com.google.api-client")
        .setArtifactId("google-api-client")
        .setVersion("1.30.8")
        .build();
    Artifact artifact = MavenHelper.resolveArtifact(coordinates, new NullProgressMonitor());
    Assert.assertEquals("google-api-client", artifact.getArtifactId());
  }
  
  @Test
  public void testResolveArtifact_android() throws CoreException {
    MavenCoordinates coordinates = new MavenCoordinates.Builder()
        .setGroupId("androidx.annotation")
        .setArtifactId("annotation")
        .setVersion("1.1.0")
        .setRepository("https://maven.google.com")
        .build();
    Artifact artifact = MavenHelper.resolveArtifact(coordinates, new NullProgressMonitor());
    Assert.assertEquals("annotation", artifact.getArtifactId());
  }
  
  @Test
  public void testResolveArtifact_android_noRepository() throws CoreException {
    MavenCoordinates coordinates = new MavenCoordinates.Builder()
        .setGroupId("androidx.annotation")
        .setArtifactId("annotation")
        .setVersion("1.1.0")
        .build();
    Artifact artifact = MavenHelper.resolveArtifact(coordinates, new NullProgressMonitor());
    Assert.assertEquals("annotation", artifact.getArtifactId());
  }
  
  @Test
  public void testBundleStateBasedMavenFolder_withLatestVersion() {
    MavenCoordinates coordinates = new MavenCoordinates.Builder()
        .setGroupId("com.google.cloud")
        .setArtifactId("datastore")
        .setVersion("LATEST")
        .build();
    try {
      MavenHelper.bundleStateBasedMavenFolder(coordinates);
      Assert.fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
