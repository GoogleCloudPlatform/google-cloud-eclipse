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

package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.appengine.libraries.util.PathUtil;
import java.net.URL;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;

public class SourceDownloaderJobFactory {

  private final MavenHelper mavenHelper;
  private final LibraryClasspathContainerSerializer serializer;
  
  public SourceDownloaderJobFactory(MavenHelper mavenHelper,
                                    LibraryClasspathContainerSerializer serializer) {
    this.mavenHelper = mavenHelper;
    this.serializer = serializer;
  }

  public Job createSourceDownloaderJob(IJavaProject javaProject, Artifact artifact,
                                       IPath classpathEntryPath, URL sourceUrl) {
    if (sourceUrl == null) {
      return new M2SourceAttachmentDownloaderJob(javaProject,
                                                 classpathEntryPath,
                                                 serializer,
                                                 artifact,
                                                 mavenHelper);
    } else {
      return new RemoteFileSourceAttachmentDownloaderJob(javaProject,
                                                         classpathEntryPath,
                                                         serializer,
                                                         PathUtil.bundleStateBasedMavenFolder(artifact),
                                                         sourceUrl);
    }
  }
}
