package com.google.cloud.tools.eclipse.appengine.sdk;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import com.google.cloud.tools.appengine.cloudsdk.PathResolver;

public final class AppEngineSDKClasspathContainer implements IClasspathContainer {

  private static final String SDK_JAR = "/platform/google_appengine/google/appengine/tools/java/lib/appengine-tools-api.jar";
  public static final String CONTAINER_ID = "AppEngineSDK";

  @Override
  public IPath getPath() {
    return new Path(AppEngineSDKClasspathContainer.CONTAINER_ID);
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
    java.nio.file.Path cloudSdkPath = PathResolver.INSTANCE.getCloudSdkPath();
    if (cloudSdkPath != null) {
      String appEngineToolsApiJar = cloudSdkPath + SDK_JAR;
      IClasspathEntry appEngineToolsEntry =
          JavaCore.newLibraryEntry(new Path(appEngineToolsApiJar),
                                   null /* sourceAttachmentPath */,
                                   null /* sourceAttachmentRootPath */);
      return new IClasspathEntry[]{ appEngineToolsEntry };
    } else {
      return new IClasspathEntry[0];
    }
  }
}
