package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

public final class AppEngineSdkClasspathContainer implements IClasspathContainer {

  public static final String CONTAINER_ID = "AppEngineSDK";

  private static final String API_JAR = "impl/appengine-api.jar";
  private static final String API_JAVADOC_URL =
      "https://cloud.google.com/appengine/docs/java/javadoc/";

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
    try {
      CloudSdk cloudSdk = new CloudSdk.Builder().build();
      if (cloudSdk != null) {
        java.nio.file.Path jarFile = cloudSdk.getJavaAppEngineSdkPath().resolve(API_JAR);
        if (jarFile != null) {
          //@formatter:off
          IClasspathAttribute javadocAttribute = 
              JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, API_JAVADOC_URL);
          IClasspathEntry appEngineApisEntry = JavaCore.newLibraryEntry(
              new Path(jarFile.toString()),
              null /* sourceAttachmentPath */,
              null /* sourceAttachmentRootPath */,
              null /* accessRules */,
              new IClasspathAttribute[] { javadocAttribute },
              true /* isExported */);
          //@formatter:on
          return new IClasspathEntry[] {appEngineApisEntry};
        }
      }
    } catch (AppEngineException ex) {
      /* fall through */
    }
    return new IClasspathEntry[0];
  }

}
