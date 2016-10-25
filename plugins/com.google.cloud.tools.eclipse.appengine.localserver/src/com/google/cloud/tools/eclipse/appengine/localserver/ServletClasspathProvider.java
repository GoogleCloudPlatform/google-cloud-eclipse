/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.localserver;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.server.core.IRuntime;

/**
 * Supply Java standard classes, specifically servlet-api.jar and jsp-api.jar,
 * to non-Maven projects.
 */
public class ServletClasspathProvider extends RuntimeClasspathProviderDelegate {
  private CloudSdk cloudSdkForTesting;

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
    CloudSdk cloudSdk = cloudSdkForTesting;
    if (cloudSdk == null) {
      try {
        cloudSdk = new CloudSdk.Builder().build();
      } catch (AppEngineException ex) {
        return new IClasspathEntry[0];
      }
    }
    java.nio.file.Path servletJar = cloudSdk.getJarPath("servlet-api.jar");
    java.nio.file.Path jspJar = cloudSdk.getJarPath("jsp-api.jar");
    IClasspathEntry servletEntry =
        JavaCore.newLibraryEntry(new Path(servletJar.toString()), null, null);
    IClasspathEntry jspEntry = JavaCore.newLibraryEntry(new Path(jspJar.toString()), null, null);
    
    IClasspathEntry[] entries = {servletEntry, jspEntry};
    return entries;
  }
  
  @VisibleForTesting
  public void setCloudSdk(CloudSdk cloudSdk) {
    this.cloudSdkForTesting = cloudSdk;
  }
}
