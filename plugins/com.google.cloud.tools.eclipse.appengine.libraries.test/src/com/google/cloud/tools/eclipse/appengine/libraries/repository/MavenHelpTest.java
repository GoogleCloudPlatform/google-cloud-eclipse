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
import org.eclipse.core.runtime.IPath;
import org.junit.Test;

public class MavenHelpTest {

  private static final String EXPECTED_DOWNLOAD_FOLDER =
      ".metadata/.plugins/com.google.cloud.tools.eclipse.appengine.libraries/downloads/groupId/artifactId/1.0.0";

  @Test
  public void testBundleStateBasedMavenFolder_withSpecificVersion() {
    MavenCoordinates artifact = new MavenCoordinates.Builder()
        .setGroupId("groupId")
        .setArtifactId("artifactId")
        .setVersion("1.0.0")
        .build();
    
    IPath folder = MavenHelper.bundleStateBasedMavenFolder(artifact);
    assertTrue(folder.toString().endsWith(EXPECTED_DOWNLOAD_FOLDER));
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testBundleStateBasedMavenFolder_withLatestVersion() {
    
    MavenCoordinates artifact = new MavenCoordinates.Builder()
        .setGroupId("groupId")
        .setArtifactId("artifactId")
        .setVersion("LATEST")
        .build();
        
    MavenHelper.bundleStateBasedMavenFolder(artifact);
  }
}
