package com.google.cloud.tools.eclipse.appengine.facets;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import com.google.cloud.tools.eclipse.sdk.CloudSdkProvider;

public final class AppEngineSdkClasspathContainer implements IClasspathContainer {

  // TODO should be changed once app-tools-lib can provide the directory
  // https://github.com/GoogleCloudPlatform/app-tools-lib-for-java/issues/134
  private static final String SDK_JAR = "platform/google_appengine/google/appengine/tools/java/lib/appengine-tools-api.jar";
  public static final String CONTAINER_ID = "AppEngineSDK";

  private File cloudSdkLocation = new CloudSdkProvider().getCloudSdkLocation();

  @Override
  public IPath getPath() {
    return new Path(AppEngineSdkClasspathContainer.CONTAINER_ID);
  }

  @Override
  public int getKind() {
    return IClasspathEntry.CPE_CONTAINER;
  }

  @Override
  public String getDescription() {
    return "App Engine SDKs";
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    if (cloudSdkLocation != null) {
      File jarFile = cloudSdkLocation.toPath().resolve(SDK_JAR).toFile();
      if (jarFile.exists()) {
        String appEngineToolsApiJar = jarFile.getPath();
        IClasspathEntry appEngineToolsEntry =
            JavaCore.newLibraryEntry(new Path(appEngineToolsApiJar),
                                     null /* sourceAttachmentPath */,
                                     null /* sourceAttachmentRootPath */,
                                     true /* isExported */);
        return new IClasspathEntry[]{ appEngineToolsEntry };
      }
    }
    return new IClasspathEntry[0];
  }

}
