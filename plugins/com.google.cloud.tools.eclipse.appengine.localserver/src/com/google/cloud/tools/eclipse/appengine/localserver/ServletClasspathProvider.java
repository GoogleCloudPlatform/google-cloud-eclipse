package com.google.cloud.tools.eclipse.appengine.localserver;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.server.core.IRuntime;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.sdk.CloudSdkProvider;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.common.annotations.VisibleForTesting;

/**
 * Supply Java standard classes, specifically servlet-api.jar and jsp-api.jar,
 * to non-Maven projects.
 */
public class ServletClasspathProvider extends RuntimeClasspathProviderDelegate {
  
  private CloudSdkProvider cloudSdkProvider = new CloudSdkProvider();

  public ServletClasspathProvider() {
  }

  @Override
  public IClasspathEntry[] resolveClasspathContainer(IProject project, IRuntime runtime) {
    if (project != null && MavenUtils.hasMavenNature(project)) { // Maven handles its own classpath
      return new IClasspathEntry[0];
    } else {
      return resolveClasspathContainer(runtime);
    }
  }

  @Override
  public IClasspathEntry[] resolveClasspathContainer(IRuntime runtime) {
    CloudSdk cloudSdk = cloudSdkProvider.getCloudSdk();
    if (cloudSdk == null) {
      return new IClasspathEntry[0];
    };
    java.nio.file.Path servletJar = cloudSdk.getJarPath("servlet-api.jar");
    java.nio.file.Path jspJar = cloudSdk.getJarPath("jsp-api.jar");
    IClasspathEntry servletEntry =
        JavaCore.newLibraryEntry(new Path(servletJar.toString()), null, null);
    IClasspathEntry jspEntry = JavaCore.newLibraryEntry(new Path(jspJar.toString()), null, null);
    
    IClasspathEntry[] entries = {servletEntry, jspEntry};
    return entries;
  }
  
  @VisibleForTesting
  public void setCloudSdkProvider(CloudSdkProvider cloudSdkProvider) {
    this.cloudSdkProvider = cloudSdkProvider;
  }
}
