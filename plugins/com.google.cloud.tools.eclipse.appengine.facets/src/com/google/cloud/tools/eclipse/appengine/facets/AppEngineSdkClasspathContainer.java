package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import java.util.ArrayList;
import java.util.List;

public final class AppEngineSdkClasspathContainer implements IClasspathContainer {

  public static final String CONTAINER_ID = "AppEngineSDK";

  private static final String APPENGINE_API_JAR = "impl/appengine-api.jar";
  private static final String APPENGINE_LABS_JAR = "impl/appengine-api-labs.jar";
  private static final String APPENGINE_JSR107CACHE_JAR = "user/appengine-jsr107cache-0.0.0.jar";
  private static final String JSR107CACHE_JAR = "user/jsr107cache-1.1.jar";

  private static final String[] INCLUDED_JARS =
      {APPENGINE_API_JAR, APPENGINE_LABS_JAR, APPENGINE_JSR107CACHE_JAR, JSR107CACHE_JAR};
  private static final String APPENGINE_API_JAVADOC_URL =
      "https://cloud.google.com/appengine/docs/java/javadoc/";

  @Override
  public IPath getPath() {
    return new Path(AppEngineSdkClasspathContainer.CONTAINER_ID);
  }

  @Override
  public int getKind() {
    return IClasspathContainer.K_DEFAULT_SYSTEM;
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
        // todo: INCLUDED_JARS should be pulled from appengine-plugins-core
        // https://github.com/GoogleCloudPlatform/appengine-plugins-core/issues/186
        List<IClasspathEntry> entries = new ArrayList<>(INCLUDED_JARS.length);
        for (String jarLocation : INCLUDED_JARS) {
          java.nio.file.Path jarFile = cloudSdk.getJavaAppEngineSdkPath().resolve(jarLocation);
          if (jarFile != null) {
            IClasspathAttribute javadocAttribute = JavaCore.newClasspathAttribute(
                IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, APPENGINE_API_JAVADOC_URL);
            //@formatter:off
            IClasspathEntry jarEntry = JavaCore.newLibraryEntry(
                new Path(jarFile.toString()),
                null /* sourceAttachmentPath */,
                null /* sourceAttachmentRootPath */,
                null /* accessRules */,
                new IClasspathAttribute[] { javadocAttribute },
                false /* isExported */);
            //@formatter:on
            entries.add(jarEntry);
          }
        }
        return entries.toArray(new IClasspathEntry[entries.size()]);
      }
    } catch (AppEngineException ex) {
      /* fall through */
    }
    return new IClasspathEntry[0];
  }

}
