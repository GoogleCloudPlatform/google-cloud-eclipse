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

package com.google.cloud.tools.eclipse.appengine.libraries.util;

import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.common.base.Preconditions;
import java.io.File;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;

public class PathUtil {
  
  private PathUtil() {
  }
  
  /**
   * Returns the folder to which the file described by <code>artifact</code> should be
   * downloaded.
   * <p>
   * The folder is created as follows:
   * <code>&lt;bundle_state_location&gt;/downloads/&lt;groupId&gt;/&lt;artifactId&gt;/&lt;version&gt;</code>
   * <p>
   * The <code>&lt;bundle_state_location&gt;</code> is determined by using the bundle containing
   * {@link ILibraryRepositoryService}.
   * 
   * @return the location of the download folder, may not exist
   */
  public static IPath bundleStateBasedMavenFolder(MavenCoordinates mavenCoordinates) {
    Preconditions.checkArgument(!mavenCoordinates.getVersion().equals(MavenCoordinates.LATEST_VERSION));
    File downloadedSources =
        Platform.getStateLocation(FrameworkUtil.getBundle(ILibraryRepositoryService.class))
        .append("downloads")
        .append(mavenCoordinates.getGroupId())
        .append(mavenCoordinates.getArtifactId())
        .append(mavenCoordinates.getVersion())
        .toFile();
    return new Path(downloadedSources.getAbsolutePath());
  }
}
