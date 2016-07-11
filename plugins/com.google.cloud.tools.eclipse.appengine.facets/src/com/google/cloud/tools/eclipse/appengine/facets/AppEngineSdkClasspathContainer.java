package com.google.cloud.tools.eclipse.appengine.facets;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.CloudSdkProvider;

public final class AppEngineSdkClasspathContainer implements IClasspathContainer {

  private static final CloudSdk CLOUD_SDK = new CloudSdkProvider().getCloudSdk();
  public static final String CONTAINER_ID = "AppEngineSDK";

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
    if (CLOUD_SDK.getJavaAppEngineSdkPath() != null) {
      File jarFile = CLOUD_SDK.getJavaToolsJar().toFile();
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
