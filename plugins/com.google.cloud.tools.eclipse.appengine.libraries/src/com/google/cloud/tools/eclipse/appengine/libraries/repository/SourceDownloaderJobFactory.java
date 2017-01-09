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

import com.google.cloud.tools.eclipse.appengine.libraries.util.PathUtil;
import com.google.cloud.tools.eclipse.util.jobs.JobUtil;
import java.net.URL;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.core.IJavaProject;

public class SourceDownloaderJobFactory {

  private MavenHelper mavenHelper;
  
  public SourceDownloaderJobFactory(MavenHelper mavenHelper) {
    this.mavenHelper = mavenHelper;
  }

  public Job createSourceDownloaderJob(IJavaProject javaProject, Artifact artifact,
                                       IPath classpathEntryPath, URL sourceUrl) {
    if (sourceUrl == null) {
      return createM2SourceDownloaderJob(javaProject, artifact, classpathEntryPath);
    } else {
      return createRemoteFileSourceDownloaderJob(javaProject, classpathEntryPath, 
                                                 artifact, sourceUrl);
    }
  }
  
  private Job createM2SourceDownloaderJob(final IJavaProject javaProject,
                                          final Artifact artifact,
                                          final IPath classpathEntryPath) {
    return JobUtil.createJob(M2SourceAttachmentDownloaderJob.class, 
                             new JobUtil.ContextParameterSupplier() {
                               @Override
                               public void setParameters(IEclipseContext context) {
                                 context.set(IJavaProject.class, javaProject);
                                 context.set(Artifact.class, artifact);
                                 context.set(M2SourceAttachmentDownloaderJob.PARAM_CLASSPATHENTRY_PATH,
                                             classpathEntryPath);
                                 context.set(MavenHelper.class, mavenHelper);
                               }
                             });
  }

  private Job createRemoteFileSourceDownloaderJob(final IJavaProject javaProject,
                                                  final IPath classpathEntryPath,
                                                  final Artifact artifact,
                                                  final URL sourceUrl) {
    return JobUtil.createJob(RemoteFileSourceAttachmentDownloaderJob.class,
                             new JobUtil.ContextParameterSupplier() {
                               @Override
                               public void setParameters(IEclipseContext context) {
                                 context.set(IJavaProject.class, javaProject);
                                 context.set(RemoteFileSourceAttachmentDownloaderJob.PARAM_CLASSPATHENTRY_PATH,
                                             classpathEntryPath);
                                 context.set(RemoteFileSourceAttachmentDownloaderJob.PARAM_DOWNLOAD_FOLDER, 
                                     PathUtil.bundleStateBasedMavenFolder(artifact));
                                 context.set(URL.class, sourceUrl);
                               }
                             });
  }
}
