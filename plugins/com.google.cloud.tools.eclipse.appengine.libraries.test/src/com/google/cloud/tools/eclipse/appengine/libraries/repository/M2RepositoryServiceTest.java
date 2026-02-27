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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

public class M2RepositoryServiceTest {

  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1993
  @Test
  public void testResolveSourceArtifact_badUriToUrl()
      throws URISyntaxException, MalformedURLException {
    URI noSchemeUri = new URI("host");
    try {
      noSchemeUri.toURL();
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("URI is not absolute", ex.getMessage());
    }

    MavenCoordinates mavenCoordinates = new MavenCoordinates.Builder()
        .setGroupId("groupId").setArtifactId("artifactId").build();
    LibraryFile library = new LibraryFile(mavenCoordinates);
    library.setSourceUri(noSchemeUri);

    IPath sourcePath = new M2RepositoryService()
        .resolveSourceArtifact(library, null, new NullProgressMonitor());
    assertNull(sourcePath);
  }
}
