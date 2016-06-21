package com.google.cloud.tools.eclipse.appengine.localserver;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.server.core.IRuntime;

import com.google.cloud.tools.appengine.cloudsdk.PathResolver;

/**
 * Supply Java standard classes, specifically servlet-api.jar and jsp-api.jar.
 */
public class ServletClasspathProvider extends RuntimeClasspathProviderDelegate {

  public ServletClasspathProvider() {
  }

  @Override
  public IClasspathEntry[] resolveClasspathContainer(IProject project, IRuntime runtime) {
    return resolveClasspathContainer(runtime);
  }

  @Override
  public IClasspathEntry[] resolveClasspathContainer(IRuntime runtime) {
    java.nio.file.Path cloudSdkPath = PathResolver.INSTANCE.getCloudSdkPath();
    if (cloudSdkPath == null) {
      return new IClasspathEntry[0];
    };
    String servletJar = cloudSdkPath + "/platform/google_appengine/google/appengine/tools/java/lib/shared/servlet-api.jar";
    String jspJar = cloudSdkPath + "/platform/google_appengine/google/appengine/tools/java/lib/shared/jsp-api.jar";
    IClasspathEntry servletEntry = JavaCore.newLibraryEntry(new Path(servletJar), null, null);
    IClasspathEntry jspEntry = JavaCore.newLibraryEntry(new Path(jspJar), null, null);
    
    IClasspathEntry[] entries = {servletEntry, jspEntry};
    return entries;
  }  
  
}
