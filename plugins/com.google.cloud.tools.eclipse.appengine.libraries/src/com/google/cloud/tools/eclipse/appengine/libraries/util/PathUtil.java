package com.google.cloud.tools.eclipse.appengine.libraries.util;

import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import java.io.File;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;

public class PathUtil {
  
  private PathUtil() {
  }
  
  /**
   * Returns the folder to which the a file corresponding to <code>mavenCoordinates</code> should be
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
    File downloadedSources =
        Platform.getStateLocation(FrameworkUtil.getBundle(ILibraryRepositoryService.class))
        .append("downloads")
        .append(mavenCoordinates.getGroupId())
        .append(mavenCoordinates.getArtifactId())
        .append(mavenCoordinates.getVersion()).toFile();
    return new Path(downloadedSources.getAbsolutePath());
  }
}
